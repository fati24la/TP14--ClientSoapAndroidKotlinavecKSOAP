import android.util.Log
import com.example.soapcompteapp.beans.Compte
import com.example.soapcompteapp.beans.TypeCompte
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapPrimitive
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Vector
import org.ksoap2.serialization.MarshalFloat

private const val TAG = "SOAP_DBG"

class Service {
    private val NAMESPACE = "http://ws.tp12_webservicesoap.tp.com/"
    private val URL = "http://10.0.2.2:8080/services/ws"
    private val TIMEOUT_MS = 30_000
    private val METHOD_GET_COMPTES = "getComptes"
    private val METHOD_CREATE_COMPTE = "createCompte"
    private val METHOD_DELETE_COMPTE = "deleteCompte"

    private val SOAP_ACTION_GET_COMPTES = "$NAMESPACE$METHOD_GET_COMPTES"
    private val SOAP_ACTION_CREATE_COMPTE = "$NAMESPACE$METHOD_CREATE_COMPTE"
    private val SOAP_ACTION_DELETE_COMPTE = "$NAMESPACE$METHOD_DELETE_COMPTE"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    private fun parseDate(raw: String?): Date {
        if (raw.isNullOrBlank()) return Date()
        val datePart = raw.substringBefore('T')
        return try {
            dateFormat.parse(datePart) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    fun getComptes(): List<Compte> {
        val request = SoapObject(NAMESPACE, METHOD_GET_COMPTES)
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
            implicitTypes = true
        }
        val transport = HttpTransportSE(URL, TIMEOUT_MS).apply { debug = true }
        val comptes = mutableListOf<Compte>()

        try {
            transport.call("", envelope)
            Log.d(TAG, "RequestDump:\n${transport.requestDump ?: "empty"}")
            Log.d(TAG, "ResponseDump:\n${transport.responseDump ?: "empty"}")

            val body = envelope.bodyIn

            fun addFromSoapObject(so: SoapObject) {
                for (i in 0 until so.propertyCount) {
                    val prop = so.getProperty(i)
                    if (prop is SoapObject) {
                        val soapCompte = prop
                        val id = soapCompte.getPropertySafelyAsString("id")?.toLongOrNull()
                        val solde =
                            soapCompte.getPropertySafelyAsString("solde")?.toDoubleOrNull() ?: 0.0
                        val dateCreation =
                            parseDate(soapCompte.getPropertySafelyAsString("dateCreation"))
                        val typeStr = soapCompte.getPropertySafelyAsString("type") ?: "COURANT"
                        val type = try {
                            TypeCompte.valueOf(typeStr)
                        } catch (e: Exception) {
                            TypeCompte.COURANT
                        }
                        comptes.add(
                            Compte(
                                id = id,
                                solde = solde,
                                dateCreation = dateCreation,
                                type = type
                            )
                        )
                    }
                }
            }

            when (body) {
                is SoapObject -> {
                    // cas où le service renvoie un enveloppe contenant une liste ou un élément
                    if (body.propertyCount == 1 && body.getProperty(0) is Vector<*>) {
                        val vec = body.getProperty(0) as Vector<*>
                        for (item in vec) if (item is SoapObject) addFromSoapObject(item)
                    } else {
                        addFromSoapObject(body)
                    }
                }

                is Vector<*> -> {
                    for (item in body) if (item is SoapObject) addFromSoapObject(item)
                }

                else -> {
                    val resp = envelope.response
                    if (resp is SoapObject) addFromSoapObject(resp)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getComptes failed: ${e.message}", e)
        }
        return comptes
    }

    fun createCompte(solde: Double, typeCompte: TypeCompte): Boolean {
        val request = SoapObject(NAMESPACE, METHOD_CREATE_COMPTE).apply {
            // solde sans namespace (serveur attend {}solde)
            val soldeProp = org.ksoap2.serialization.PropertyInfo().apply {
                name = "solde"
                type = java.lang.Double::class.java
                value = java.lang.Double.valueOf(solde)
                namespace = "" // important : élément non qualifié
            }
            addProperty(soldeProp)

            // type sans namespace
            val typeProp = org.ksoap2.serialization.PropertyInfo().apply {
                name = "type"
                type = java.lang.String::class.java
                value = typeCompte.name
                namespace = ""
            }
            addProperty(typeProp)
        }

        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
            implicitTypes = true
        }

        // enregistrer un marshal disponible dans la version ksoap2 utilisée
        try {
            MarshalFloat().register(envelope)
        } catch (t: Throwable) {
            // si MarshalFloat absent ou incompatible, on ignore ici — priorité : namespace
        }

        val transport = HttpTransportSE(URL, TIMEOUT_MS).apply { debug = true }

        return try {
            transport.call("", envelope)
            val resp = envelope.response
            resp != null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "createCompte failed: ${e.message}", e)
            false
        }
    }


    fun deleteCompte(id: Long): Boolean {
        val request = SoapObject(NAMESPACE, METHOD_DELETE_COMPTE).apply {
            addProperty("id", id)
        }
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER11).apply {
            dotNet = false
            setOutputSoapObject(request)
        }
        val transport = HttpTransportSE(URL, TIMEOUT_MS).apply { debug = true }

        return try {
            transport.call("", envelope)
            Log.d(TAG, "RequestDump:\n${transport.requestDump ?: "empty"}")
            Log.d(TAG, "ResponseDump:\n${transport.responseDump ?: "empty"}")

            val resp = envelope.response
            when (resp) {
                is Boolean -> resp
                is String -> resp.toBoolean()
                is SoapPrimitive -> resp.toString().toBoolean()
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteCompte failed: ${e.message}", e)
            false
        }
    }
}
package com.example.soapcompteapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.example.soapcompteapp.R
import com.example.soapcompteapp.beans.Compte
import java.text.SimpleDateFormat
import java.util.*

class CompteAdapter : RecyclerView.Adapter<CompteAdapter.CompteViewHolder>() {
    private var comptes = mutableListOf<Compte>()

    // Listeners pour gérer les clics sur Modifier et Supprimer
    var onEditClick: ((Compte) -> Unit)? = null
    var onDeleteClick: ((Compte) -> Unit)? = null

    /**
     * Met à jour la liste des comptes affichés.
     * @param newComptes Nouvelle liste de comptes.
     */
    fun updateComptes(newComptes: List<Compte>) {
        comptes.clear()
        comptes.addAll(newComptes)
        notifyDataSetChanged()
    }

    /**
     * Supprime un compte de la liste.
     * @param compte Compte à supprimer.
     */
    fun removeCompte(compte: Compte) {
        val position = comptes.indexOf(compte)
        if (position >= 0) {
            comptes.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * Crée une nouvelle vue pour chaque élément de la liste.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item, parent, false)
        return CompteViewHolder(view)
    }

    /**
     * Lie un élément de données à une vue.
     */
    override fun onBindViewHolder(holder: CompteViewHolder, position: Int) {
        holder.bind(comptes[position])
    }

    /**
     * Retourne le nombre total d'éléments dans la liste.
     */
    override fun getItemCount() = comptes.size

    /**
     * Classe ViewHolder pour gérer les vues individuelles.
     */
    inner class CompteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val id: TextView = view.findViewById(R.id.textId)
        private val solde: TextView = view.findViewById(R.id.textSolde)
        private val type: Chip = view.findViewById(R.id.textType)
        private val crDate: TextView = view.findViewById(R.id.textDate)
        private val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        private val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)

        /**
         * Associe un objet Compte aux vues.
         */
        fun bind(compte: Compte) {
            id.text = "Compte Numéro ${compte.id}"
            solde.text = "${compte.solde} DH"
            type.text = compte.type.name
            crDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(compte.dateCreation)

            btnEdit.setOnClickListener { onEditClick?.invoke(compte) }
            btnDelete.setOnClickListener { onDeleteClick?.invoke(compte) }
        }
    }
}
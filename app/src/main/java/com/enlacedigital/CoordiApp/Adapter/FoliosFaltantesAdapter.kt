package com.enlacedigital.CoordiApp.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enlacedigital.CoordiApp.models.ComparativaResponse
import com.enlacedigital.CoordiApp.R

class FoliosFaltantesAdapter(private val folios: List<ComparativaResponse>) : RecyclerView.Adapter<FoliosFaltantesAdapter.FolioViewHolder>() {
    class FolioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val folioTextView: TextView = itemView.findViewById(R.id.folio_text_view)
        val telefonoTextView: TextView = itemView.findViewById(R.id.telefono_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolioViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folios_faltantes, parent, false)
        return FolioViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: FolioViewHolder, position: Int) {
        val currentFolio = folios[position]
        holder.folioTextView.text = "Folio: ${currentFolio.Folios_Telmex}"
        holder.telefonoTextView.text = "Teléfono: ${currentFolio.TELEFONOS_TELMEX}"
    }

    override fun getItemCount() = folios.size
}

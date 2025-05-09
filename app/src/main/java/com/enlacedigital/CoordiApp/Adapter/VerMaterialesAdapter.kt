package com.enlacedigital.CoordiApp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.models.materialesdetalle

class VerMaterialesAdapter(val items: MutableList<materialesdetalle>) : RecyclerView.Adapter<VerMaterialesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val Producto: TextView = itemView.findViewById(R.id.Producto)
        val Modelo: TextView = itemView.findViewById(R.id.Modelo)
        val Num_Serie_Salida_Det: TextView = itemView.findViewById(R.id.Num_Serie_Salida_Det)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_materiales, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.Producto.text = item.Producto.orDash()
        holder.Modelo.text = item.Modelo.orDash()
        holder.Num_Serie_Salida_Det.text = item.Num_Serie_Salida_Det.orDash()
    }

    override fun getItemCount() = items.size
    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"
    private fun String?.orIncompleto(): String = this?.takeIf { it.isNotBlank() } ?: "INCOMPLETO"

}
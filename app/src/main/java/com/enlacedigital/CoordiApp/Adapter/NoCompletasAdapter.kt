package com.enlacedigital.CoordiApp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enlacedigital.CoordiApp.models.FoliosDetalle
import com.enlacedigital.CoordiApp.R

class NoCompletasAdapter(val items: MutableList<FoliosDetalle>) : RecyclerView.Adapter<NoCompletasAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val folioPisa: TextView = itemView.findViewById(R.id.folio_pisa)
        val fkCope: TextView = itemView.findViewById(R.id.fk_cope)
        val tipoTarea: TextView = itemView.findViewById(R.id.tipo_tarea)
        val estatusOrden: TextView = itemView.findViewById(R.id.estatus_orden)
        val tecnologia: TextView = itemView.findViewById(R.id.tecnologia)
        val telefono: TextView = itemView.findViewById(R.id.telefono)
        val contratista: TextView = itemView.findViewById(R.id.contratista)
        val step: TextView = itemView.findViewById(R.id.step)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_incompleted, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.folioPisa.text = item.Folio_Pisa.orDash()
        holder.fkCope.text = item.FK_Cope.orDash()
        holder.tipoTarea.text = item.Tipo_Tarea.orDash()
        holder.estatusOrden.text = item.Estatus_Orden.orIncompleto()
        holder.tecnologia.text = item.Tecnologia.orDash()
        holder.telefono.text = item.Telefono.orDash()
        holder.contratista.text = item.Contratista.orDash()
        holder.step.text = item.Step_Registro ?: "-"
    }

    override fun getItemCount() = items.size
    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"
    private fun String?.orIncompleto(): String = this?.takeIf { it.isNotBlank() } ?: "INCOMPLETO"

}
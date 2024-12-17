package com.enlacedigital.CoordiApp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.models.FoliosDetalle

/**
 * Adaptador para mostrar una lista de elementos [FoliosDetalle] en un RecyclerView.
 *
 * @param items Lista mutable de [FoliosDetalle] que se mostrarán en la vista.
 */
class CompletedAdapter(val items: MutableList<FoliosDetalle>) : RecyclerView.Adapter<CompletedAdapter.ViewHolder>() {

    /**
     * ViewHolder que representa una vista individual de un elemento en el RecyclerView.
     *
     * @param itemView La vista correspondiente a un elemento individual.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val folioPisa: TextView = itemView.findViewById(R.id.folio_pisa)
        val tecnico: TextView = itemView.findViewById(R.id.tecnico)
        val cope: TextView = itemView.findViewById(R.id.cope)
        val tipoTarea: TextView = itemView.findViewById(R.id.tipo_tarea)
        val estatusOrden: TextView = itemView.findViewById(R.id.estatus_orden)
        val tecnologia: TextView = itemView.findViewById(R.id.tecnologia)
        val telefono: TextView = itemView.findViewById(R.id.telefono)
        val contratista: TextView = itemView.findViewById(R.id.contratista)
        val fecha: TextView = itemView.findViewById(R.id.fecha)
    }

    /**
     * Crea una nueva instancia del [ViewHolder] inflando la vista correspondiente.
     *
     * @param parent El ViewGroup al que se añadirá la nueva vista.
     * @param viewType El tipo de vista (no utilizado aquí).
     * @return Una nueva instancia de [ViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_completed, parent, false)
        return ViewHolder(view)
    }

    /**
     * Enlaza los datos de un elemento de la lista a una posición específica en el ViewHolder.
     *
     * @param holder La instancia de [ViewHolder] que se enlazará con los datos.
     * @param position La posición del elemento en la lista.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tecnico.text = item.Tecnico.orDash()
        holder.folioPisa.text = item.Folio_Pisa?.orDash()
        holder.cope.text = item.COPE.orDash()
        holder.tipoTarea.text = item.Tipo_Tarea.orDash()
        holder.fecha.text = item.Fecha_Coordiapp.orDash()
        holder.estatusOrden.text = item.Estatus_Orden.orDash()
        holder.tecnologia.text = item.Tecnologia.orDash()
        holder.telefono.text = item.Telefono.orDash()
        holder.contratista.text = item.Contratista.orDash()
    }

    /**
     * Devuelve la cantidad de elementos en la lista.
     *
     * @return El tamaño de la lista [items].
     */
    override fun getItemCount() = items.size

    /**
     * Extensión de la clase [String] que devuelve un guion "-" si la cadena es nula o está vacía.
     *
     * @receiver Una cadena que se verificará.
     * @return La cadena original si no está vacía, o un guion "-".
     */
    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"
}

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.models.FoliosDetalle

class CompletedAdapter(val items: MutableList<FoliosDetalle>) : RecyclerView.Adapter<CompletedAdapter.ViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_completed, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tecnico.text = item.Tecnico.orDash()
        holder.folioPisa.text = item.Folio_Pisa?.toString().orDash()
        holder.cope.text = item.COPE.orDash()
        holder.tipoTarea.text = item.Tipo_Tarea.orDash()
        holder.fecha.text = item.Fecha_Coordiapp.orDash()
        holder.estatusOrden.text = item.Estatus_Orden.orDash()
        holder.tecnologia.text = item.Tecnologia.orDash()
        holder.telefono.text = item.Telefono.orDash()
        holder.contratista.text = item.Contratista.orDash()
    }

    override fun getItemCount() = items.size
    private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"

}

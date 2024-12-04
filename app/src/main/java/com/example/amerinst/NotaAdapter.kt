import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.amerinst.Nota
import com.example.amerinst.R

class NotaAdapter(private val notas: List<Nota>) : RecyclerView.Adapter<NotaAdapter.NotaViewHolder>() {

    class NotaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val estudianteNombre: TextView = view.findViewById(R.id.textEstudianteNombre)
        val estudianteId: TextView = view.findViewById(R.id.textEstudianteId)
        val materiaNombre: TextView = view.findViewById(R.id.textMateriaNombre)
        val nota: TextView = view.findViewById(R.id.textNota)
        val bimestre: TextView = view.findViewById(R.id.textBimestre)
        val tipo: TextView = view.findViewById(R.id.textTipo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = notas[position]
        holder.estudianteNombre.text = "Estudiante: ${nota.estudianteNombre} ${nota.estudianteApellido}"
        holder.estudianteId.text = "ID Estudiante: ${nota.estudianteId}"
        holder.materiaNombre.text = "Materia: ${nota.materiaNombre}"
        holder.nota.text = "Nota: ${nota.nota}"
        holder.bimestre.text = "Bimestre: ${nota.bimestre}"
        holder.tipo.text = "Tipo: ${nota.tipo}"
    }

    override fun getItemCount() = notas.size
}


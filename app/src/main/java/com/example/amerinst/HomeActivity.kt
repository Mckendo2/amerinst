package com.example.amerinst

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException



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

class HomeActivity : AppCompatActivity() {

    private lateinit var adapter: NotaAdapter
    private lateinit var spinnerMaterias: Spinner
    private lateinit var recyclerView: RecyclerView
    private var materiasList: List<String> = listOf()
    private var notasList: List<Nota> = listOf()
    private var isSessionActive = true

    override fun onBackPressed() {
        if (isSessionActive) {
            // Si la sesión está activa, mostramos el mensaje indicando que debe cerrarse
            Toast.makeText(this, "Debes cerrar sesión antes de salir", Toast.LENGTH_SHORT).show()
        } else {
            // Si la sesión no está activa, entonces dejamos que la actividad se cierre normalmente
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val imageButton: ImageButton = findViewById(R.id.imageButton)
        imageButton.setOnClickListener {
            // Creamos un intent para redirigir a MainActivity
            val intent = Intent(this@HomeActivity, MainActivity::class.java)
            startActivity(intent)  // Iniciamos la actividad
            finish()  // Opcional: Termina la actividad actual (si deseas cerrar HomeActivity)
        }

        spinnerMaterias = findViewById(R.id.spinnerMaterias)
        recyclerView = findViewById(R.id.recyclerViewNotas)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId != -1) {
        CoroutineScope(Dispatchers.IO).launch {
            // Obtener materias para el Spinner
            materiasList = getMateriasFromDatabase()
            withContext(Dispatchers.Main) {
                setupSpinner()
            }

            // Obtener todas las notas iniciales
            notasList = getNotasFromDatabase(userId)
            withContext(Dispatchers.Main) {
                recyclerView.adapter = NotaAdapter(notasList)
            }
        }

        // Configurar botón para filtrar
        findViewById<Button>(R.id.btnFiltrar).setOnClickListener {
            filtrarNotasPorMateria()
        }
    }
    }

    private fun connToDatabase(): Connection? {
        val url = "jdbc:mysql://sql10.freesqldatabase.com:3306/sql10748092"
        val username = "sql10748092"
        val password = "mMnHKQZbF9"
        return try {
            Class.forName("com.mysql.jdbc.Driver").newInstance()
            DriverManager.getConnection(url, username, password)
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun getNotasFromDatabase(userId: Int): List<Nota> {
        val notasList = mutableListOf<Nota>()
        val conn = connToDatabase()

        if (conn != null) {
            try {
                // Consulta modificada para obtener notas solo de los estudiantes asociados al padre
                val query = conn.prepareStatement(
                    """
                    SELECT e.nombre AS estudiante_nombre,
                           e.apellido AS estudiante_apellido,
                           e.estudiante_id AS estudiante_id,
                           m.nombre AS materia_nombre, 
                           n.nota, 
                           n.bimestre, 
                           n.tipo
                    FROM notas n
                    JOIN estudiantes e ON n.estudiante_id = e.estudiante_id
                    JOIN materias m ON n.materia_id = m.materia_id
                    JOIN estudiante_padre ep ON ep.estudiante_id = e.estudiante_id
                    WHERE ep.padre_id = ?
                    """
                )
                query.setInt(1, userId)

                val result: ResultSet = query.executeQuery()

                while (result.next()) {
                    val nota = Nota(
                        estudianteNombre = result.getString("estudiante_nombre"),
                        estudianteApellido = result.getString("estudiante_apellido"),
                        estudianteId = result.getInt("estudiante_id"),
                        materiaNombre = result.getString("materia_nombre"),
                        nota = result.getFloat("nota"),
                        bimestre = result.getInt("bimestre"),
                        tipo = result.getString("tipo")
                    )
                    notasList.add(nota)
                }

                result.close()
                query.close()
                conn.close()

            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        return notasList
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, materiasList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMaterias.adapter = adapter
    }

    private fun filtrarNotasPorMateria() {
        val materiaSeleccionada = spinnerMaterias.selectedItem.toString()
        val notasFiltradas = notasList.filter { it.materiaNombre == materiaSeleccionada }
        recyclerView.adapter = NotaAdapter(notasFiltradas)
    }

    private suspend fun getMateriasFromDatabase(): List<String> {
        val materias = mutableListOf<String>()
        val conn = connToDatabase()

        if (conn != null) {
            try {
                val query = conn.prepareStatement("SELECT nombre FROM materias")
                val result: ResultSet = query.executeQuery()

                while (result.next()) {
                    materias.add(result.getString("nombre"))
                }

                result.close()
                query.close()
                conn.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        return materias
    }



}
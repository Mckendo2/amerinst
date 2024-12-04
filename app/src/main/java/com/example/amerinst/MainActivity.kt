package com.example.amerinst

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import at.favre.lib.crypto.bcrypt.BCrypt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular las vistas
        val emailField = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val passwordField = findViewById<EditText>(R.id.editTextTextPassword)
        val loginButton = findViewById<Button>(R.id.button)

        // Configurar el click del botón
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    loginUser(email, password)
                }
            } else {
                Toast.makeText(this, "Por favor, ingrese todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connToDatabase(): Connection? {
        // Ajustar el URL de conexión
        val url = "jdbc:mysql://sql10.freesqldatabase.com:3306/sql10748092"
        val username = "sql10748092"
        val password = "mMnHKQZbF9"
        return try {
            // Cargar el driver de MySQL
            Class.forName("com.mysql.jdbc.Driver").newInstance()
            // Realizar la conexión
            DriverManager.getConnection(url, username, password)
        } catch (e: SQLException) {
            e.printStackTrace() // Para depuración
            null
        }
    }

    private suspend fun loginUser(email: String, password: String) {
        val conn = connToDatabase()

        if (conn != null) {
            try {
                // Consulta que obtiene la contraseña y el rol
                val query = conn.prepareStatement("SELECT user_id, password, rol_id FROM usuarios WHERE email = ?")
                query.setString(1, email)

                // Ejecutar la consulta
                val result: ResultSet = query.executeQuery()

                if (result.next()) {
                    val storedHash = result.getString("password")
                    val rolId = result.getInt("rol_id")  // Obtener el rol del usuario
                    val userId = result.getInt("user_id")  // Obtener el user_id

                    // Verificar el hash de la contraseña usando la biblioteca BCrypt de at.favre.lib
                    val verificationResult = BCrypt.verifyer().verify(password.toCharArray(), storedHash)
                    val passwordMatches = verificationResult.verified

                    if (passwordMatches) {
                        // Verificar que el rol sea "padre" (rol_id == 3)
                        if (rolId == 3) {
                            // Guardar el user_id en SharedPreferences para usarlo en otras actividades
                            withContext(Dispatchers.Main) {
                                val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
                                val editor = sharedPreferences.edit()
                                editor.putInt("user_id", userId)  // Guardamos el user_id
                                editor.apply()

                                // Mostrar mensaje de éxito
                                Toast.makeText(this@MainActivity, "Inicio de sesión exitoso", Toast.LENGTH_LONG).show()

                                // Iniciar la HomeActivity
                                val intent = Intent(this@MainActivity, HomeActivity::class.java)
                                startActivity(intent)
                                finish()  // Cerrar MainActivity para que no pueda volver al login con el botón atrás
                            }
                        } else {
                            // Si el rol no es 3 (no es un padre), mostrar mensaje de error
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "No tienes permisos para acceder", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        // Si la contraseña no es correcta, mostrar un mensaje de error
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Si no se encuentra el usuario, mostrar un mensaje de error
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Usuario no encontrado", Toast.LENGTH_LONG).show()
                    }
                }

                // Cerrar recursos
                result.close()
                query.close()
            } catch (e: SQLException) {
                // Manejo de error de conexión o SQL
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error en la consulta o en la conexión", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Captura cualquier otro tipo de excepción que pueda ocurrir
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error desconocido", Toast.LENGTH_LONG).show()
                }
            } finally {
                // Asegurarse de que la conexión se cierra
                try {
                    conn.close()
                } catch (e: SQLException) {
                    // Si la conexión no se puede cerrar, manejamos el error aquí
                }
            }
        } else {
            // Si no se pudo establecer la conexión, mostrar mensaje de error
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Problemas de conexión con la base de datos.", Toast.LENGTH_LONG).show()
            }
        }
    }




}

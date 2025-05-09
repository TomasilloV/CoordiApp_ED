package com.enlacedigital.CoordiApp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.enlacedigital.CoordiApp.models.LogoutResponse
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.showToast
import com.enlacedigital.CoordiApp.utils.startNewActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Clase `Menu` que representa la pantalla principal de navegación en la aplicación.
 * Muestra botones para acceder a funcionalidades principales, como:
 * - Tareas completadas
 * - Tareas incompletas
 * - Comparativa
 * - Cerrar sesión
 */
class Menu : AppCompatActivity() {

    /** Gestor de preferencias compartidas */
    private val preferencesManager = PreferencesHelper.getPreferencesManager()

    /** Servicio de API para realizar llamadas de red */
    private val apiService = ApiServiceHelper.getApiService()

    /**
     * Método llamado al crear la actividad.
     * Configura la interfaz de usuario y listeners para los botones.
     *
     * @param savedInstanceState Estado anterior de la actividad si está disponible.
     */
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        setupListeners()
        setupUI()
    }

    /**
     * Configura la interfaz de usuario inicializando el nombre del técnico y la versión.
     */
    private fun setupUI() {
        val tecnico: TextView = findViewById(R.id.tecnico)
        tecnico.text = preferencesManager.getString("tecnico")

        val version: TextView = findViewById(R.id.version)
        version.text = "v${packageManager.getPackageInfo(packageName, 0).versionName}"
    }

    /**
     * Configura los listeners de los botones de navegación.
     * Utiliza un método auxiliar para asignar eventos a cada botón.
     */
    private fun setupListeners() {
        setClickListener(R.id.registered, Completed::class.java)
        setClickListener(R.id.incompleted, NoCompletas::class.java)
        setClickListener(R.id.compare, Comparativa::class.java)
        setClickListener(R.id.materiales, ver_materiales::class.java)
        setClickListener(R.id.nuevoRegistro, Registrando::class.java)

        findViewById<ImageButton>(R.id.logout).setOnClickListener {
            // Alerta de confirmacion de cierre de sesión
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Alerta")
            builder.setMessage("¿Estas seguro de cerrar sesión?")
            builder.setPositiveButton("Aceptar") { dialog, _ ->
                performLogout()
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }

    /**
     * Método auxiliar para asignar un evento `onClick` a un botón.
     *
     * @param buttonId ID del botón definido en el layout.
     * @param activityClass Clase de la actividad a la que se redirige al hacer clic.
     */
    private fun setClickListener(buttonId: Int, activityClass: Class<*>) {
        findViewById<ImageButton>(buttonId).setOnClickListener {
            startActivity(Intent(this, activityClass))
        }
    }

    /**
     * Realiza el proceso de cierre de sesión del usuario.
     * Llama a la API para invalidar la sesión actual y limpia las preferencias.
     */
    private fun performLogout() {
        apiService.logout().enqueue(object : Callback<LogoutResponse> {
            /**
             * Se ejecuta al recibir una respuesta exitosa o fallida de la API.
             *
             * @param call Instancia de la llamada Retrofit.
             * @param response Respuesta HTTP recibida del servidor.
             */
            override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) {
                if (response.isSuccessful) {
                    preferencesManager.clearSession()
                    startNewActivity(Login::class.java)
                } else {
                    showToast("Error en la respuesta del servidor: ${response.code()}")
                }
            }

            /**
             * Se ejecuta cuando ocurre un fallo en la solicitud.
             *
             * @param call Instancia de la llamada Retrofit.
             * @param t Excepción que describe el fallo.
             */
            override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                showToast("Error en la conexión: ${t.localizedMessage}")
            }
        })
    }
}

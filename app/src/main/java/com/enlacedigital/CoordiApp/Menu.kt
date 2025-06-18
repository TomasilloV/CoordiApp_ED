package com.enlacedigital.CoordiApp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.enlacedigital.CoordiApp.models.Folios
import com.enlacedigital.CoordiApp.models.LogoutResponse
import com.enlacedigital.CoordiApp.models.materiales
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.showToast
import com.enlacedigital.CoordiApp.utils.startNewActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.activity.OnBackPressedCallback

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
        val idTecnicoString = preferencesManager.getString("id_tecnico")
        if (idTecnicoString.isNullOrEmpty()) {
            showToast("Por favor, inicia sesión nuevamente.")
            startNewActivity(Login::class.java)
            return
        }
        val idTecnico = idTecnicoString.toInt()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mostrarAlertaSalir()
            }
        })
        completadas(idTecnico)
        Incompletas(idTecnico)
        Materiales(idTecnico)
        setupListeners()
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        val idTecnicoString = preferencesManager.getString("id_tecnico")
        val idTecnico = idTecnicoString!!.toInt()
        Log.d("LogLOco",""+idTecnico)
        completadas(idTecnico)
        Incompletas(idTecnico)
        Materiales(idTecnico)
        setupListeners()
        setupUI()
    }

    private fun mostrarAlertaSalir() {
        AlertDialog.Builder(this)
            .setTitle("¿Estás seguro?")
            .setMessage("¿Seguro que quieres salir de la aplicación?")
            .setPositiveButton("Sí") { _, _ ->
                finish()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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

    fun completadas (idTecnico: Int)
    {
        val cantCompletadas: TextView = findViewById<TextView>(R.id.cantidad_completadas)
        apiService.getCompletados(idTecnico /*, page, limit*/).enqueue(object : Callback<Folios> {
            /**
             * Maneja la respuesta exitosa de la solicitud.
             */
            override fun onResponse(call: Call<Folios>, response: Response<Folios>) {
                if (response.isSuccessful) {
                    val cantidad: Int
                    if (response.body()!!.items == null)
                    {
                        cantidad = 0
                    }
                    else
                    {
                        cantidad = response.body()!!.items!!.size
                    }
                    cantCompletadas.setText(cantidad.toString())
                } else {
                    Log.d("DebugCantidad",""+response.message()+response.body()+response.code()+response.isSuccessful)
                    showToast("Error en la respuesta del servidor")
                }
            }

            /**
             * Maneja el fallo de la solicitud de la API.
             */
            override fun onFailure(call: Call<Folios>, t: Throwable) {
                Log.d("DebugCantidad",""+t.message)
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    fun Incompletas(tecnicoId: Int)
    {
        val cantIncompletadas: TextView = findViewById<TextView>(R.id.cantidad_incompletadas)
        apiService.getNoCompletados(tecnicoId/*, page, limit*/).enqueue(object : Callback<Folios> {
            override fun onResponse(call: Call<Folios>, response: Response<Folios>) {
                if (response.isSuccessful) {
                    val cantidad: Int
                    if (response.body()!!.items == null)
                    {
                        cantidad = 0
                    }
                    else
                    {
                        cantidad = response.body()!!.items!!.size
                    }
                    cantIncompletadas.setText(cantidad.toString())
                } else {
                    showToast("Error en la respuesta")
                    finish()
                }
            }

            override fun onFailure(call: Call<Folios>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
                finish()
            }
        })
    }

    fun Materiales(tecnicoId: Int)
    {
        val cantMateriales: TextView = findViewById<TextView>(R.id.cantidad_materiales)
        apiService.vermateriales(tecnicoId/*, page, limit*/).enqueue(object : Callback<materiales> {
            override fun onResponse(call: Call<materiales>, response: Response<materiales>) {

                if (response.isSuccessful) {
                    val cantidad: Int
                    if (response.body()!!.items == null)
                    {
                        cantidad = 0
                    }
                    else
                    {
                        cantidad = response.body()!!.items!!.size
                    }
                    cantMateriales.setText(cantidad.toString())
                } else {
                    showToast("Error en la respuesta")
                    finish()
                }
            }

            override fun onFailure(ignoredCall: Call<materiales>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
                finish()
            }
        })
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
                    Log.d("LoginDebug", "Código HTTP: ${response.code()}")
                    Log.d("LoginDebug", "Es exitoso: ${response.isSuccessful}")
                    Log.d("LoginDebug", "Mensaje: ${response.message()}")
                    Log.d("LoginDebug", "Raw body: ${response.errorBody()?.string()}")
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

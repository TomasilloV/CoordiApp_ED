package com.enlacedigital.CoordiApp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.enlacedigital.CoordiApp.models.LoginResponse
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkPermission
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.showToast
import com.enlacedigital.CoordiApp.utils.hideKeyboardOnOutsideTouch
import com.enlacedigital.CoordiApp.utils.startNewActivity
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Actividad de inicio de sesión para la aplicación.
 * Permite al usuario autenticarse y acceder al menú principal de la aplicación.
 */
class Login : AppCompatActivity() {

    /** Gestor de preferencias compartidas para almacenar datos del usuario */
    private val preferencesManager = PreferencesHelper.getPreferencesManager()

    /** Código de solicitud para permisos */
    private val requestPERMISSION = 1

    /** Vista para mostrar el estado de carga */
    private lateinit var loadingLayout: FrameLayout

    /**
     * Método llamado al crear la actividad.
     * Configura permisos, la interfaz y los listeners para el inicio de sesión.
     *
     * @param savedInstanceState Estado anterior de la actividad si está disponible.
     */
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Solicita permisos necesarios
        checkPermission(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ), requestPERMISSION
        )

        // Configura el número de versión de la app
        val versionText: TextView = findViewById(R.id.version)
        versionText.text = "v" + packageManager.getPackageInfo(packageName, 0).versionName

        // Configura el botón de login
        val loginButton = findViewById<Button>(R.id.loginButton)
        val userEditText = findViewById<EditText>(R.id.user)
        loadingLayout = findViewById(R.id.loading_layout)

        loginButton.setOnClickListener {
            val username = userEditText.text.toString()
            if (username.isEmpty()) {
                showToast("Por favor ingrese un nombre de usuario")
                return@setOnClickListener
            }

            // Inicia la llamada de login en segundo plano
            lifecycleScope.launch {
                loadingLayout.setLoadingVisibility(true)
                val apiService = createRetrofitService(this@Login)
                val call = apiService.login(username)

                call.enqueue(object : Callback<LoginResponse> {
                    /**
                     * Maneja la respuesta exitosa o con error de la llamada de inicio de sesión.
                     *
                     * @param call Instancia de la llamada Retrofit.
                     * @param response Respuesta HTTP recibida del servidor.
                     */
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        handleLoginResponse(response)
                    }

                    /**
                     * Maneja el fallo en la conexión durante la llamada de inicio de sesión.
                     *
                     * @param call Instancia de la llamada Retrofit.
                     * @param t Excepción que describe el fallo.
                     */
                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        loadingLayout.setLoadingVisibility(false)
                        showToast("Error. Asegúrate de tener una conexión a internet estable")
                    }
                })
            }
        }
    }

    /**
     * Procesa la respuesta de inicio de sesión.
     * Si la autenticación es exitosa, guarda los datos del usuario y redirige al menú principal.
     *
     * @param response Respuesta recibida del servidor.
     */
    private fun handleLoginResponse(response: Response<LoginResponse>) {
        loadingLayout.setLoadingVisibility(false)
        if (response.isSuccessful) {
            val loginResponse = response.body()
            if (loginResponse?.mensaje == "Datos Correctos") {
                loginResponse.info.let { item ->
                    preferencesManager.saveString("id_tecnico", item.idTecnico.toString())
                    val tecnico = "${item.Nombre_T} ${item.Apellidos_T}"
                    preferencesManager.saveString("tecnico", tecnico)
                    startNewActivity(Menu::class.java)
                }
            } else {
                showToast("Usuario o datos incorrectos")
            }
        } else {
            showToast("Error en la respuesta del servidor")
        }
    }

    /**
     * Detecta eventos táctiles para ocultar el teclado al tocar fuera de los campos de texto.
     *
     * @param ev Evento de toque detectado.
     * @return `true` si el evento fue manejado correctamente.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        hideKeyboardOnOutsideTouch(ev)
        return super.dispatchTouchEvent(ev)
    }
}
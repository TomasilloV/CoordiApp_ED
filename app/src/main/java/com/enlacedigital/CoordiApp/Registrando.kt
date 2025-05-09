package com.enlacedigital.CoordiApp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.enlacedigital.CoordiApp.Registrar.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.google.android.gms.location.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.ApiException
import com.enlacedigital.CoordiApp.utils.*
import kotlinx.coroutines.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Actividad principal para el proceso de registro.
 * Administra la actualización de datos, permisos y configuración de ubicación.
 */
class Registrando : AppCompatActivity(), ActualizadBDListener {
    /** Servicio de la API utilizado para actualizar los datos del técnico */
    private val apiService = ApiServiceHelper.getApiService()

    /** Overlay que se muestra al cargar procesos */
    private lateinit var loadingOverlay: FrameLayout

    /** Job para controlar las actualizaciones con coroutines */
    private var updateJob: Job? = null

    /** Cliente de configuración de ubicación */
    private lateinit var settingsClient: SettingsClient

    /** Botón para cancelar la actualización en curso */
    private lateinit var cancelButton: Button

    /** Configuración para las solicitudes de ubicación */
    private lateinit var locationRequest: LocationRequest

    /** Manejador para solicitudes de permisos */
    private lateinit var locationPermissionRequest: ActivityResultLauncher<String>

    /** Código de verificación para la configuración de ubicación */
    private val checkSettings = 1001

    /**
     * Inicializa la actividad y configura permisos, ubicación y fragmentos.
     *
     * @param savedInstanceState Estado anterior de la actividad si existe.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrando)

        // Configura la versión actual de la app
        val version: TextView = findViewById(R.id.version)
        version.text = "v" + packageManager.getPackageInfo(packageName, 0).versionName

        checkPermissions()

        cancelButton = findViewById(R.id.cancelButton)
        cancelButton.visibility = View.INVISIBLE
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingOverlay.setLoadingVisibility(false)

        cancelButton.setOnClickListener {
            updateJob?.cancel()
            showToast("Actualización cancelada")
            loadingOverlay.setLoadingVisibility(false)
        }

        settingsClient = LocationServices.getSettingsClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkLocationSettings()
            } else {
                showToast("No se puede continuar sin permisos de ubicación")
                finish()
            }
        }

        // Inicia el primer fragmento de registro
        supportFragmentManager.beginTransaction()
            .replace(R.id.main, RegistrandoFragmentValidar())
            .commit()
    }

    /**
     * Verifica y solicita permisos necesarios para la aplicación.
     */
    private fun checkPermissions() {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        checkPermission(permissions, checkSettings)
    }

    /**
     * Muestra un mensaje tipo Toast.
     *
     * @param message El mensaje a mostrar.
     */
    fun toasting(message: String) {
        showToast(message)
    }

    /**
     * Verifica la configuración de ubicación del dispositivo.
     */
    private fun checkLocationSettings() {
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnCompleteListener { task ->
                try {
                    task.result
                } catch (e: ResolvableApiException) {
                    e.startResolutionForResult(this, checkSettings)
                } catch (e: ApiException) {
                    showToast("Por favor, activa la ubicación para continuar")
                    finish()
                }
            }
    }

    /**
     * Actualiza los datos del técnico utilizando la API.
     *
     * @param requestData Datos a actualizar encapsulados en un objeto [ActualizarBD].
     */
    override fun updateTechnicianData(requestData: ActualizarBD) {
        if (updateJob?.isActive == true) return
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            loadingOverlay.setLoadingVisibility(true)
            cancelButton.visibility = View.INVISIBLE

            launch {
                delay(6000)
                if (updateJob?.isActive == true) {
                    cancelButton.visibility = View.VISIBLE
                }
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.updateTechnicianData(requestData).execute()
                }
                val apiResponse = response.body()
                if (response.isSuccessful) {
                    if (apiResponse?.mensaje == "Registro actualizado exitosamente.") {
                        showToast(apiResponse.mensaje)
                        if (requestData.Estatus_Orden == "OBJETADA") startNewActivity(Menu::class.java)
                        else goToNextStep(requestData.Step_Registro!!)
                    } else showToast(apiResponse?.mensaje ?: "Intenta de nuevo")
                } else showToast("Error: ${response.code()}")
            } catch (e: Exception) {
                handleUpdateException(e)
            } finally {
                cancelButton.visibility = View.INVISIBLE
                loadingOverlay.setLoadingVisibility(false)
            }
        }
    }

    /**
     * Maneja las excepciones durante la actualización de datos.
     *
     * @param e La excepción ocurrida.
     */
    private fun handleUpdateException(e: Exception) {
        when (e) {
            is CancellationException -> {}
            is UnknownHostException -> showToast("Sin conexión a Internet. Verifica tu red e intenta de nuevo.")
            is SocketTimeoutException -> showToast("Tiempo de espera agotado. La red puede estar lenta o caída.")
            is IOException -> showToast("Error de red. Verifica tu conexión e intenta nuevamente.")
            else -> showToast("Error: ${e.message}")
        }
    }

    /** Mapa de fragmentos de registro por paso */
    private val fragmentMap = mapOf(
        0 to RegistrandoFragment1(),
        1 to RegistrandoFragment2(),
        2 to RegistrandoFragment3(),
        3 to RegistrandoFragment4(),
        4 to RegistrandoFragment5(),
        5 to RegistrandoFragment6(),
        6 to RegistrandoFragment7()
    )

    /**
     * Avanza al siguiente paso del proceso de registro.
     *
     * @param step El paso actual en el proceso.
     */
    fun goToNextStep(step: Int) {
        applicationContext.cacheDir.deleteRecursively()
        if (step == 7) {
            startNewActivity(Menu::class.java)
        }

        val fragment = fragmentMap[step] ?: return
        supportFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }

    /**
     * Oculta el teclado si se toca fuera de un campo de texto.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        hideKeyboardOnOutsideTouch(ev)
        return super.dispatchTouchEvent(ev)
    }
}

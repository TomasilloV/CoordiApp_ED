package com.enlacedigital.CoordiApp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.enlacedigital.CoordiApp.Registrar.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsRequest
import com.enlacedigital.CoordiApp.utils.checkPermission
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.enlacedigital.CoordiApp.utils.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class Registrando : AppCompatActivity(), ActualizadBDListener {
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationPermissionRequest: ActivityResultLauncher<String>
    private val checkSettings = 1001

    @Suppress("DEPRECATION")
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrando)
        val version: TextView = findViewById(R.id.version)
        version.text = "v" + packageManager.getPackageInfo(packageName, 0).versionName

        checkPermissions()

        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingOverlay.setLoadingVisibility(false)

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

        supportFragmentManager.beginTransaction()
            .replace(R.id.main, RegistrandoFragmentValidar())
            .commit()
    }

    private fun checkPermissions() {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        checkPermission(permissions, checkSettings)
    }

    fun toasting(message: String){
        showToast(message)
    }

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

    override fun updateTechnicianData(requestData: ActualizarBD) {
        CoroutineScope(Dispatchers.Main).launch {
            loadingOverlay.setLoadingVisibility(true)
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
            } catch (e: SocketTimeoutException) {
                showToast("Tiempo de espera excedido, por favor intenta de nuevo")
            } catch (e: UnknownHostException) {
                showToast("Verifica tu conexión y vuelve a intentarlo")
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                loadingOverlay.setLoadingVisibility(false)
            }
        }
    }

    private val fragmentMap = mapOf(
        0 to RegistrandoFragment1(),
        1 to RegistrandoFragment2(),
        2 to RegistrandoFragment3(),
        3 to RegistrandoFragment4(),
        4 to RegistrandoFragment5()
    )

    fun goToNextStep(step: Int) {
        applicationContext.cacheDir.deleteRecursively()
        if (step == 5) {
            startNewActivity(Menu::class.java)
        }

        val fragment = fragmentMap[step] ?: return
        supportFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        hideKeyboardOnOutsideTouch(ev)
        return super.dispatchTouchEvent(ev)
    }
}

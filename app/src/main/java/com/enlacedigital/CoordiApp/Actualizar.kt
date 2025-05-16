package com.enlacedigital.CoordiApp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.snackbar.Snackbar
import android.app.AlertDialog
import android.content.pm.PackageManager
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.AppUpdateType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Build
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.google.gson.JsonObject
import kotlin.system.exitProcess
import com.enlacedigital.CoordiApp.utils.checkSession

/**
 * Actividad encargada de gestionar la actualización de la aplicación.
 * Verifica si la app está instalada desde Google Play Store y maneja el proceso
 * de actualización, ya sea mediante la actualización automática o solicitando
 * la descarga e instalación de un APK.
 */
class Actualizar : AppCompatActivity() {

    /** Gestor de preferencias para acceder a los datos guardados localmente */
    val preferencesManager = PreferencesHelper.getPreferencesManager()

    /** Servicio API para realizar peticiones relacionadas con la versión de la app */
    val apiService = ApiServiceHelper.getApiService()

    /** Gestor de actualizaciones de la app usando la API de Google Play */
    private lateinit var appUpdateManager: AppUpdateManager

    /** Código de solicitud para manejar el flujo de actualización */
    private val requestCodeUpdate = 100

    /**
     * Método que se ejecuta al iniciar la actividad. Aquí se maneja la instalación
     * de la pantalla de inicio (SplashScreen) y la lógica para verificar la versión
     * de la app y proceder con la actualización si es necesario.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Si la versión de Android es 12 o superior, usar la API de SplashScreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.actualizar)

        // Inicializa el gestor de actualizaciones de la app
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // Verifica si la app está instalada desde Google Play Store o no
        if (isInstalledFromPlayStore()) {
            checkForAppUpdate()
        } else {
            updateAPK()
        }
    }

    /**
     * Verifica si la aplicación fue instalada desde Google Play Store.
     *
     * @return true si la app fue instalada desde Play Store, false en caso contrario.
     */
    private fun isInstalledFromPlayStore(): Boolean {
        return try {
            // Verifica la fuente de instalación en versiones de Android 11 o superiores
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val installSourceInfo = packageManager.getInstallSourceInfo(packageName)
                installSourceInfo.installingPackageName == "com.android.vending"
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName) == "com.android.vending"
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Realiza una solicitud al API para verificar si es necesario actualizar la app
     * con un APK. Si la versión actual es inferior a la mínima requerida, se muestra
     * un diálogo solicitando la actualización.
     */
    private fun updateAPK() {
        apiService.checkVersion()!!.enqueue(object : Callback<JsonObject?> {
            /**
             * Maneja la respuesta de la verificación de la versión de la app.
             * Si la versión instalada es inferior a la mínima requerida, muestra un
             * diálogo pidiendo al usuario que actualice.
             */
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                response.body()?.let {
                    val minVersion = it.get("min_version_CordiApp").asString.toFloatOrNull() ?: 0f
                    val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName?.toFloatOrNull()
                    if (currentVersion != null && currentVersion < minVersion) {
                        showUpdateDialog() // Muestra el diálogo de actualización
                    } else {
                        checkSession(apiService, this@Actualizar, Menu::class.java) // Continúa si no es necesario actualizar
                    }
                }
            }

            /**
             * Maneja el fallo de la verificación de la versión de la app.
             */
            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Log.e("VersionCheck", "Error: $t")
            }
        })
    }

    /**
     * Verifica si hay una actualización disponible en Google Play Store.
     * Si es posible realizar la actualización inmediata, inicia el proceso de actualización.
     */
    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { handleUpdate(it) }
            .addOnFailureListener { Log.e("AppUpdate", "Error: $it") }
    }

    /**
     * Maneja la lógica de la actualización si está disponible en Play Store.
     * Si se permite una actualización inmediata, se inicia el flujo de actualización.
     * Si no, se verifica la sesión.
     *
     * @param appUpdateInfo Información sobre la actualización disponible.
     */
    private fun handleUpdate(appUpdateInfo: AppUpdateInfo) {
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            startUpdate(appUpdateInfo) // Inicia la actualización si está disponible
        } else {
            checkSession(apiService, this, Menu::class.java) // Verifica la sesión si no es necesario actualizar
        }
    }

    /**
     * Inicia el flujo de actualización para la app en Play Store.
     *
     * @param appUpdateInfo Información sobre la actualización disponible.
     */
    @Suppress("DEPRECATION")
    private fun startUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, requestCodeUpdate)
    }

    /**
     * Muestra un diálogo indicando que la actualización es obligatoria y que la app debe ser cerrada.
     */
    private fun showUpdateDialog() {
        AlertDialog.Builder(this)
            .setTitle("Actualización requerida")
            .setMessage("Debes instalar la actualización para continuar.")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ -> exitProcess(0) }
            .show()
    }

    /**
     * Listener que maneja el estado de la instalación de la actualización. Si la actualización se descarga
     * con éxito, muestra un mensaje de reinicio para completar la instalación.
     */
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Snackbar.make(findViewById(android.R.id.content), "Actualización descargada, reinicia para instalar.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Reiniciar") { appUpdateManager.completeUpdate() }
                .show()
        }
    }

    /**
     * Maneja el resultado de la actividad de actualización. Si la actualización no fue exitosa, muestra
     * un diálogo solicitando la actualización.
     */
    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeUpdate && resultCode != RESULT_OK) {
            showUpdateDialog() // Muestra el diálogo si la actualización no fue exitosa
        }
    }

    /**
     * Registra el listener para detectar el estado de la actualización cada vez que la actividad se reanuda.
     */
    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(appUpdateInfo) // Inicia la actualización si está en progreso
            }
        }
    }

    /**
     * Desregistra el listener cuando la actividad es destruida.
     */
    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}
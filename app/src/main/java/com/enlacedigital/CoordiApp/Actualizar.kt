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

class Actualizar : AppCompatActivity() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var appUpdateManager: AppUpdateManager
    private val requestCodeUpdate = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        // Usar la API de SplashScreen en Android 12 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actualizar)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        if(isInstalledFromPlayStore()) checkForAppUpdate() else updateAPK()
    }

    private fun isInstalledFromPlayStore(): Boolean {
        return try {
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

    private fun updateAPK() {
        apiService.checkVersion()!!.enqueue(object : Callback<JsonObject?> {
            override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                response.body()?.let {
                    val minVersion = it.get("min_version_CordiApp").asString.toFloatOrNull() ?: 0f
                    val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName?.toFloatOrNull()
                    if (currentVersion != null && currentVersion < minVersion) showUpdateDialog() else checkSession(apiService, this@Actualizar, Menu::class.java)
                }
            }

            override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                Log.e("VersionCheck", "Error: $t")
            }
        })
    }

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { handleUpdate(it) }
            .addOnFailureListener { Log.e("AppUpdate", "Error: $it") }
    }

    private fun handleUpdate(appUpdateInfo: AppUpdateInfo) {
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            startUpdate(appUpdateInfo)
        } else {
            checkSession(apiService, this, Menu::class.java)
        }
    }

    @Suppress("DEPRECATION")
    private fun startUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, requestCodeUpdate)
    }

    private fun showUpdateDialog() {
        AlertDialog.Builder(this)
            .setTitle("Actualización requerida")
            .setMessage("Debes instalar la actualización para continuar.")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ -> exitProcess(0) }
            .show()
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Snackbar.make(findViewById(android.R.id.content), "Actualización descargada, reinicia para instalar.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Reiniciar") { appUpdateManager.completeUpdate() }
                .show()
        }
    }

    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeUpdate && resultCode != RESULT_OK) {
            showUpdateDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(appUpdateInfo)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}
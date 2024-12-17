package com.enlacedigital.CoordiApp

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.enlacedigital.CoordiApp.utils.showToast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

/**
 * Helper para manejar la obtención y verificación de la ubicación del usuario.
 *
 * @param context Contexto de la aplicación o actividad.
 * @param onLocationObtained Callback que se ejecuta cuando se obtiene una ubicación exitosa.
 * @param onLocationFailed Callback que se ejecuta cuando falla la obtención de la ubicación.
 * @param settingsLauncher Lanzador para resolver la configuración de ubicación requerida.
 */
class LocationHelper(
    private val context: Context,
    private val onLocationObtained: (Location) -> Unit,
    private val onLocationFailed: (String) -> Unit,
    private var settingsLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

    /** Cliente para obtener actualizaciones y la última ubicación conocida */
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    /** Configuración de la solicitud de ubicación */
    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(1000)
        .setMaxUpdateDelayMillis(15000)
        .build()

    /**
     * Obtiene la última ubicación conocida del dispositivo.
     * Si no está disponible, solicita una nueva ubicación.
     */
    @SuppressLint("MissingPermission")
    fun obtenerUbicacion() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                // Llama al callback con la ubicación obtenida o solicita una nueva
                location?.let { onLocationObtained(it) } ?: solicitarNuevaUbicacion()
            }
            .addOnFailureListener { exception ->
                manejarErrorUbicacion("Error al obtener la ubicación: ${exception.message}")
            }
    }

    /**
     * Solicita una nueva ubicación en caso de que no exista una última ubicación conocida.
     * Utiliza actualizaciones de ubicación en tiempo real.
     */
    @SuppressLint("MissingPermission")
    private fun solicitarNuevaUbicacion() {
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            /**
             * Se llama cuando se recibe una actualización de ubicación.
             *
             * @param locationResult Resultado con la última ubicación obtenida.
             */
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    onLocationObtained(it)
                } ?: manejarErrorUbicacion("No se pudo obtener la ubicación actualizada.")
            }
        }, Looper.getMainLooper())
    }

    /**
     * Maneja errores relacionados con la obtención de la ubicación.
     *
     * @param mensaje Mensaje descriptivo del error.
     */
    private fun manejarErrorUbicacion(mensaje: String) {
        context.showToast(mensaje)
        onLocationFailed(mensaje)
    }

    /**
     * Verifica si la configuración de ubicación está habilitada.
     * Si no lo está, solicita al usuario habilitarla.
     */
    fun verificarUbicacion() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Configuración correcta, procede a obtener la ubicación
            obtenerUbicacion()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Solicita al usuario corregir la configuración
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    settingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    onLocationFailed("Error al intentar resolver la configuración de ubicación: ${sendEx.message}")
                }
            } else {
                onLocationFailed("La configuración de ubicación no es adecuada y no se puede resolver.")
            }
        }
    }
}

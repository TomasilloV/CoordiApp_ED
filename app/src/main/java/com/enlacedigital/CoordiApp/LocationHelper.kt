package com.enlacedigital.CoordiApp

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.location.*
import com.enlacedigital.CoordiApp.utils.showToast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.tasks.Task

class LocationHelper(
    private val context: Context,
    private val onLocationObtained: (Location) -> Unit,
    private val onLocationFailed: (String) -> Unit,
    private var settingsLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Configuración de la solicitud de ubicación
    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(1000)
        .setMaxUpdateDelayMillis(15000)
        .build()

    // Función para obtener la última ubicación conocida o solicitar una nueva si no está disponible
    @SuppressLint("MissingPermission")
    fun obtenerUbicacion() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let { onLocationObtained(it) } ?: solicitarNuevaUbicacion()
            }
            .addOnFailureListener { exception ->
                manejarErrorUbicacion("Error al obtener la ubicación: ${exception.message}")
            }
    }

    // Solicita una nueva ubicación en caso de que no se tenga una última ubicación conocida
    @SuppressLint("MissingPermission")
    private fun solicitarNuevaUbicacion() {
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    onLocationObtained(it)
                } ?: manejarErrorUbicacion("No se pudo obtener la ubicación actualizada.")
            }
        }, Looper.getMainLooper())
    }

    // Función para manejar los errores de ubicación
    private fun manejarErrorUbicacion(mensaje: String) {
        context.showToast(mensaje)
        onLocationFailed(mensaje)
    }

    fun verificarUbicacion() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            obtenerUbicacion()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
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
package com.enlacedigital.CoordiApp.Registrar

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.enlacedigital.CoordiApp.LocationHelper
import com.enlacedigital.CoordiApp.Menu
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.activity.result.IntentSenderRequest
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.startNewActivity

data class Checking(
    val mensaje: String,
    val item: ActualizarBD?,
    val id: String?
)

class RegistrandoFragmentValidar : Fragment(R.layout.fragment_registrandovalidar) {

    private lateinit var locationPermissionRequest: ActivityResultLauncher<String>
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var settingsLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var locationHelper: LocationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                locationHelper.obtenerUbicacion()
            } else {
                (requireActivity() as? Registrando)?.toasting("No se puede continuar sin habilitar la ubicación")
                requireActivity().supportFragmentManager.popBackStack()
                (activity as? Registrando)?.goToNextStep(5)
            }
        }

        locationHelper = LocationHelper(
            context = requireContext(),
            onLocationObtained = { location ->
                view?.findViewById<EditText>(R.id.editLatitud)?.setText(location.latitude.toString())
                view?.findViewById<EditText>(R.id.editLongitud)?.setText(location.longitude.toString())
            },
            onLocationFailed = { message ->
                (requireActivity() as? Registrando)?.toasting(message)
                requireActivity().supportFragmentManager.popBackStack()
                (activity as? Registrando)?.goToNextStep(5)
            },
            settingsLauncher = settingsLauncher
        )

        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationHelper.obtenerUbicacion()
            } else {
                (requireActivity() as? Registrando)?.toasting("No se puede continuar sin habilitar la ubicación.")
                requireActivity().supportFragmentManager.popBackStack()
                (activity as? Registrando)?.goToNextStep(5)
            }
        }
        return inflater.inflate(R.layout.fragment_registrandovalidar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editFolio = view.findViewById<EditText>(R.id.editFolio)
        val editTelefono = view.findViewById<EditText>(R.id.editTelefono)
        val editLatitud = view.findViewById<EditText>(R.id.editLatitud)
        val editLongitud = view.findViewById<EditText>(R.id.editLongitud)
        val nextButton = view.findViewById<Button>(R.id.next)

        checkSession(apiService, requireContext(), null as Class<Nothing>?)

        locationHelper.verificarUbicacion()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationHelper.obtenerUbicacion()
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        nextButton.setOnClickListener {
            val folio = editFolio.text.toString()
            val telefono = editTelefono.text.toString()
            val latitud = editLatitud.text.toString()
            val longitud = editLongitud.text.toString()

            if (folio.isEmpty() || telefono.isEmpty() || latitud.isEmpty() || longitud.isEmpty()) {
                (requireActivity() as? Registrando)?.toasting("Por favor, completa todos los campos")
            } else if (!telefono.matches(Regex("\\d{10}"))) {
                (requireActivity() as? Registrando)?.toasting("Por favor, inserta un número válido")
            } else {
                sendData(folio, telefono, latitud, longitud, preferencesManager.getString("id_tecnico"))
            }
        }
    }

    private fun sendData(folio: String, telefono: String, latitud: String, longitud: String, idTecnico: String?) {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fechaActual = Date()
        val fecha = formato.format(fechaActual)
        apiService.checkAndInsert(folio, telefono, latitud, longitud, idTecnico, fecha).enqueue(object : Callback<Checking> {
            override fun onResponse(call: Call<Checking>, response: Response<Checking>) {
                if (response.isSuccessful) {
                    response.body()?.let { processResponse(it, folio) }
                } else {
                    (requireActivity() as? Registrando)?.toasting("Error: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Checking>, t: Throwable) {
                (activity as? Registrando)?.toasting("Error: ${t.message}")
            }
        })
    }

    private fun processResponse(checking: Checking, folio: String) {
        val mensaje = checking.mensaje
        val registro = checking.item
        val id = checking.id

        when (mensaje) {
            "Registro insertado correctamente" -> {
                preferencesManager.saveString("id", id.toString())
                (activity as? Registrando)?.goToNextStep(0)
            }
            "Registro existente" -> {
                val registroId = registro?.idtecnico_instalaciones_coordiapp.toString()
                val estado = registro?.Estatus_Orden.toString()
                if (estado == "OBJETADA") {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Este folio ya ha sido objetado")
                        .setMessage("¿Deseas actualizar?")
                        .setPositiveButton("Sí") { _, _ ->
                            preferencesManager.saveString("id", registroId)
                            preferencesManager.saveString("folio", folio)
                            existing(0)
                        }
                        .setNegativeButton("No") { _, _ ->
                            startNewActivity(Menu::class.java)
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    if(registro?.Step_Registro == 5) (requireActivity() as? Registrando)?.toasting("Ya fue registrado anteriormente")
                    preferencesManager.saveString("id", registroId)
                    preferencesManager.saveString("folio", folio)
                    existing(registro?.Step_Registro!!)
                }
            }
            else -> (requireActivity() as? Registrando)?.toasting("Otro caso")
        }
    }

    private fun existing(stepRegistro: Int) {
        if (stepRegistro in 0..5) {
            (activity as? Registrando)?.goToNextStep(stepRegistro)
        } else {
            (requireActivity() as? Registrando)?.toasting("Ocurrió un error, revisa los datos nuevamente")
        }
    }
}

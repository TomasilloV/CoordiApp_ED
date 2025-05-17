package com.enlacedigital.CoordiApp.Registrar

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
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
import com.enlacedigital.CoordiApp.models.OrdenRequest
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.startNewActivity
import kotlinx.coroutines.delay

/**
 * Data class para representar la respuesta del servidor.
 *
 * @property mensaje El mensaje devuelto por el servidor.
 * @property item Información del registro, si existe.
 * @property id El ID asociado al registro.
 */
data class Checking(
    val mensaje: String,
    val item: ActualizarBD?,
    val id: String?
)

/**
 * Fragmento responsable de manejar el proceso de validación de registros.
 */
class RegistrandoFragmentValidar : Fragment(R.layout.fragment_registrandovalidar) {

    /**
     * Lanzador para gestionar los permisos de ubicación.
     */
    private lateinit var locationPermissionRequest: ActivityResultLauncher<String>
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    /**
     * Lanzador para gestionar los ajustes de ubicación.
     */
    private lateinit var settingsLauncher: ActivityResultLauncher<IntentSenderRequest>

    /**
     * Helper para obtener la ubicación del usuario.
     */
    private lateinit var locationHelper: LocationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Configuración del lanzador de ajustes
        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                locationHelper.obtenerUbicacion()
            } else {
                (requireActivity() as? Registrando)?.toasting("No se puede continuar sin habilitar la ubicación")
                requireActivity().supportFragmentManager.popBackStack()
                (activity as? Registrando)?.goToNextStep(5)
            }
        }

        // Configuración del helper de ubicación
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

        // Configuración del lanzador de permisos
        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationHelper.obtenerUbicacion()
            } else {
                (requireActivity() as? Registrando)?.toasting("No se puede continuar sin habilitar la ubicación.")
                requireActivity().supportFragmentManager.popBackStack()
                (activity as? Registrando)?.goToNextStep(5)
            }
        }
        val view = inflater.inflate(R.layout.fragment_registrandovalidar, container, false)
        val btnrecargar = view.findViewById<Button>(R.id.btnrecargar)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        btnrecargar.setOnClickListener {
            btnrecargar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                parentFragmentManager.beginTransaction()
                    .replace(R.id.swiperefresh, RegistrandoFragmentValidar())
                    .commit()
            }, 1500)
        }
        Thread.sleep(200L)
        btnrecargar.isEnabled = true

        return view
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

            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                locationHelper.verificarUbicacion()
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationHelper.obtenerUbicacion()
                } else {
                    locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }, 0)
            handler.postDelayed({
                preferencesManager.saveString("folio-pisa",folio)

                if (folio.isEmpty() || telefono.isEmpty() || latitud.isEmpty() || longitud.isEmpty()) {
                    (requireActivity() as? Registrando)?.toasting("Por favor, completa todos los campos")
                } else if (!telefono.matches(Regex("\\d{10}"))) {
                    (requireActivity() as? Registrando)?.toasting("Por favor, inserta un número válido")
                } else {
                    sendData(folio, telefono, latitud, longitud, preferencesManager.getString("id_tecnico"))
                }
            }, 400)
        }
    }

    /**
     * Envía los datos capturados al servidor.
     *
     * @param folio El folio de registro.
     * @param telefono El teléfono del usuario.
     * @param latitud La latitud obtenida.
     * @param longitud La longitud obtenida.
     * @param idTecnico El ID del técnico.
     */
    private fun sendData(folio: String, telefono: String, latitud: String, longitud: String, idTecnico: String?) {
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fechaActual = Date()
        val fecha = formato.format(fechaActual)

        apiService.checkAndInsert(folio, telefono, latitud, longitud, idTecnico, fecha).enqueue(object : Callback<Checking> {
            override fun onResponse(call: Call<Checking>, response: Response<Checking>) {
                if (response.isSuccessful) {
                    response.body()?.let { processResponse(it, folio) }
                } else {
                    (requireActivity() as? Registrando)?.toasting("Error: verifica tu conexión")
                }
            }

            override fun onFailure(call: Call<Checking>, t: Throwable) {
                (activity as? Registrando)?.toasting("Error: verifica tu conexión")
            }
        })
    }

    /**
     * Procesa la respuesta del servidor y gestiona el flujo de trabajo según el estado.
     *
     * @param checking La respuesta del servidor.
     * @param folio El folio de registro.
     */
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

    /**
     * Realiza acciones según el paso de registro existente.
     *
     * @param stepRegistro El paso de registro actual.
     */
    private fun existing(stepRegistro: Int) {
        if (stepRegistro in 0..5) {
            (activity as? Registrando)?.goToNextStep(stepRegistro)
        } else {
            (requireActivity() as? Registrando)?.toasting("Ocurrió un error, revisa los datos nuevamente")
        }
    }
}

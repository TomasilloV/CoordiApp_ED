package com.enlacedigital.CoordiApp.Registrar

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
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
import com.enlacedigital.CoordiApp.MenuRegistrando
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
    private lateinit var spinnerEstatus: Spinner
    private lateinit var spinnerTipoInstalacion: Spinner

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
        preferencesManager.saveString("boton1","")
        preferencesManager.saveString("boton2","")
        preferencesManager.saveString("boton3","")
        preferencesManager.saveString("boton4","")
        preferencesManager.saveString("boton5","")
        preferencesManager.saveString("boton6","")
        preferencesManager.saveString("boton7","")
        preferencesManager.saveString("QUEJAMIGRA","")
        preferencesManager.saveString("botonCobre","")
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
        spinnerEstatus = view.findViewById(R.id.spinnerEstatus)
        spinnerTipoInstalacion = view.findViewById(R.id.spinnerTipoInstalacion)

        setupSpinners(spinnerEstatus)
        setupSpinners2(spinnerTipoInstalacion)

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
                val selectedEstatus = spinnerEstatus.selectedItem?.takeIf { it != "Elige una opción" } as? String
                val selectedTipoInstalacion = spinnerTipoInstalacion.selectedItem?.takeIf { it != "Elige una opción" } as? String

                if (folio.isEmpty() || telefono.isEmpty() || latitud.isEmpty() || longitud.isEmpty() || selectedEstatus == null || selectedTipoInstalacion == null) {
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
                    Log.d("ValidarDebug", "Código HTTP: ${response.code()}")
                    Log.d("ValidarDebug", "Es exitoso: ${response.isSuccessful}")
                    Log.d("ValidarDebug", "Mensaje: ${response.message()}")
                    Log.d("ValidarDebug", "Raw body: ${response.errorBody()?.string()}")
                    (requireActivity() as? Registrando)?.toasting("Error: verifica tu conexión")
                }
            }

            override fun onFailure(call: Call<Checking>, t: Throwable) {
                Log.e("ValidarDebug", "Fallo de conexión: ${t.localizedMessage}", t)
                (activity as? Registrando)?.toasting("Error: verifica tu conexión")
            }
        })
    }

    private fun setupSpinner(spinner: Spinner, items: List<String?>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupSpinners(vararg spinners: Spinner) {
        val estatusOptions = listOf("Elige una opción", "OBJETADA", "COMPLETADA")
        setupSpinner(spinners[0], estatusOptions)
    }

    private fun setupSpinners2(vararg spinners: Spinner) {
        val estatusOptions = listOf("Elige una opción", "ALTA", "QUEJA MIGRA")
        setupSpinner(spinners[0], estatusOptions)
    }

    private fun setupButtonClick(spinnerEstatus: Spinner, spinnerTipoInstalacion: Spinner) {
        val selectedEstatus = spinnerEstatus.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val selectedTipoInstalacion = spinnerTipoInstalacion.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fechaActual = Date()
            val fecha = formato.format(fechaActual)
            Log.d("Paso1Debug","id: "+preferencesManager.getString("id"))
            Log.d("Paso1Debug","id: "+preferencesManager.getString("id_tecnico"))
        if (selectedTipoInstalacion == "QUEJA MIGRA")
        {
            preferencesManager.saveString("QUEJAMIGRA","SI")
        }
            preferencesManager.getString("id")!!.let { id ->
                val updateRequest = ActualizarBD(
                    idtecnico_instalaciones_coordiapp = id,
                    Estatus_Orden = selectedEstatus,
                    FK_Tecnico_apps = preferencesManager.getString("id_tecnico")!!.toInt(),
                    Fecha_Coordiapp = fecha,
                    Tipo_Orden = selectedTipoInstalacion,
                    Step_Registro = 0
                )
                Log.d("Paso1Debug","updateRequest: "+updateRequest)
                (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
                if (selectedEstatus == "OBJETADA")
                {
                    startNewActivity(Menu::class.java)
                }
                val fragmentA = MenuRegistrando()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main, fragmentA)
                    .commit()
            } ?: (requireActivity() as? Registrando)?.toasting("Inicia sesión para continuar")
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
                setupButtonClick(spinnerEstatus,spinnerTipoInstalacion)
            }
            "Registro existente" -> {
                val registroId = registro?.idtecnico_instalaciones_coordiapp.toString()
                val estado = registro?.Estatus_Orden.toString()
                if (estado == "OBJETADA") {
                    Log.d("ValidarDebug","Aqui esta el error if")
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
                    Log.d("ValidarDebug","Aqui esta el error else")
                    if(registro?.Step_Registro == 5) (requireActivity() as? Registrando)?.toasting("Ya fue registrado anteriormente")
                    Log.d("ValidarDebug","Aqui esta el error else1")
                    preferencesManager.saveString("id", registroId)
                    Log.d("ValidarDebug","Aqui esta el error else2")
                    preferencesManager.saveString("folio", folio)
                    Log.d("ValidarDebug","Aqui esta el error else3: "+registro?.Step_Registro)
                    existing(registro?.Step_Registro!!)
                    Log.d("ValidarDebug","Aqui esta el error else4")
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
        Log.d("ValidarDebug","Aqui esta el error else: "+stepRegistro)
        if (stepRegistro in 0..5) {
            val selectedTipoInstalacion = spinnerTipoInstalacion.selectedItem?.takeIf { it != "Elige una opción" } as? String
            var iaa = 0
            while (iaa <= stepRegistro)
            {
                if (iaa == 1) {
                    preferencesManager.saveString("boton1", "listo1")
                }
                if (iaa == 2) {
                    preferencesManager.saveString("boton2", "listo2")
                }
                if (iaa == 3) {
                    preferencesManager.saveString("boton3", "listo3")
                }
                if (iaa == 4) {
                    preferencesManager.saveString("boton4", "listo4")
                }
                if (iaa == 5) {
                    preferencesManager.saveString("boton5", "listo5")
                }
                if (iaa == 6) {
                    preferencesManager.saveString("boton6", "listo6")
                }
                Log.d("ValidarDebug","Aqui esta el error else iaa: "+iaa)
                iaa = iaa + 1
            }
            if (selectedTipoInstalacion == "QUEJA MIGRA")
            {
                preferencesManager.saveString("QUEJAMIGRA","SI")
            }
            (activity as? Registrando)?.goToNextStep(0)
        } else {
            Log.d("stepDebug",""+stepRegistro)
            (requireActivity() as? Registrando)?.toasting("Ocurrió un error, revisa los datos nuevamente")
        }
    }
}

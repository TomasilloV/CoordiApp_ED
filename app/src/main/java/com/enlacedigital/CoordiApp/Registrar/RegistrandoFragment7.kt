package com.enlacedigital.CoordiApp.Registrar

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.content.FileProvider
import com.enlacedigital.CoordiApp.LocationHelper
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import androidx.activity.result.IntentSenderRequest
import com.enlacedigital.CoordiApp.MenuRegistrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.encodeImageToBase64
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.createImageFile
import java.io.File
import java.io.IOException
import kotlin.toString

class RegistrandoFragment7 : Fragment(R.layout.fragment_registrando7) {

    // Propiedades
    private lateinit var btnFotoPuerto: Button
    private lateinit var editLatitud: EditText
    private lateinit var editLongitud: EditText
    private lateinit var editTerminal: EditText
    private lateinit var loadingLayout: FrameLayout
    private lateinit var locationHelper: LocationHelper
    private lateinit var locationPermissionRequest: ActivityResultLauncher<String>
    private lateinit var settingsLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri?>
    private lateinit var spinnerPuerto: Spinner
    private var fotoONT: String? = null
    private var currentPhotoPath: String = ""
    private val preferencesManager = PreferencesHelper.getPreferencesManager()
    private val apiService = ApiServiceHelper.getApiService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupLocationHelper()
        setupPermissionLauncher()
        setupPhotoLauncher()
        val view = inflater.inflate(R.layout.fragment_registrando7, container, false)
        val btnrecargar = view.findViewById<Button>(R.id.btnrecargar)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar2)
        val btnRegresar = view.findViewById<Button>(R.id.btnRegresar)

        Handler(Looper.getMainLooper()).postDelayed({
            btnRegresar.setOnClickListener {
                val fragmentA = MenuRegistrando()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.main, fragmentA)
                    .commit()
            }
        }, 1500)

        btnrecargar.setOnClickListener {
            btnrecargar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                parentFragmentManager.beginTransaction()
                    .replace(R.id.swiperefresh, RegistrandoFragment7())
                    .commit()
            }, 1500)

        }
        Thread.sleep(200L)
        btnrecargar.isEnabled = true
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupListeners()
        updateSpinners()
    }

    // Inicialización de vistas
    private fun initializeViews(view: View) {
        spinnerPuerto = view.findViewById(R.id.spinnerPuerto)
        btnFotoPuerto = view.findViewById(R.id.btnFoto_Puerto)
        editLatitud = view.findViewById(R.id.editLatitud)
        editLongitud = view.findViewById(R.id.editLongitud)
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        editTerminal = view.findViewById(R.id.editTerminal)
        loadingLayout.setLoadingVisibility(false)
    }

    // Configuración de listeners
    private fun setupListeners() {
        btnFotoPuerto.setOnClickListener {
            takePhoto()
        }
        view?.findViewById<Button>(R.id.next)?.setOnClickListener {
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
                validateAndProceed()
            }, 500)
        }
    }

    // Configuración del LocationHelper
    private fun setupLocationHelper() {
        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                locationHelper.obtenerUbicacion()
            } else {
                showToastAndExit("No se puede continuar sin habilitar la ubicación")
            }
        }

        locationHelper = LocationHelper(
            context = requireContext(),
            onLocationObtained = { location ->
                editLatitud.setText(location.latitude.toString())
                editLongitud.setText(location.longitude.toString())
            },
            onLocationFailed = { message ->
                showToastAndExit(message)
            },
            settingsLauncher = settingsLauncher
        )
    }

    // Configuración del lanzador de permisos
    private fun setupPermissionLauncher() {
        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationHelper.obtenerUbicacion()
            } else {
                showToastAndExit("No se puede continuar sin habilitar la ubicación.")
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            locationHelper.obtenerUbicacion()
        }
    }

    // Configuración del lanzador de fotos
    private fun setupPhotoLauncher() {
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) handleCameraPhoto()
        }
    }

    // Toma de foto
    private fun takePhoto() {
        val (photoFile, photoPath) = try {
            createImageFile(requireContext())
        } catch (ex: IOException) {
            showToast("Error al crear la imagen")
            null to ""
        }

        photoFile?.let {
            currentPhotoPath = photoPath
            val photoUri = FileProvider.getUriForFile(requireContext(), "com.enlacedigital.CoordiApp.fileprovider", it)
            takePhotoLauncher.launch(photoUri)
        }
    }

    // Manejo de la foto tomada
    private fun handleCameraPhoto() {
        val file = File(currentPhotoPath)
        if (file.exists()) {
            val imageData = encodeImageToBase64(file)
            updatePhoto(imageData)
        } else {
            showToast("No se encontró la foto")
        }
    }

    // Actualización de la foto
    private fun updatePhoto(base64: String) {
        fotoONT = base64
        btnFotoPuerto.text = "Cambiar foto"
        btnFotoPuerto.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
    }

    // Validación y envío de datos
    private fun validateAndProceed() {
        val latitud = editLatitud.text.toString().takeIf { it.isNotBlank() }
        val longitud = editLongitud.text.toString().takeIf { it.isNotBlank() }
        val terminal = editTerminal.text.toString().takeIf { it.isNotBlank() }
        val puerto = spinnerPuerto.selectedItem?.takeIf { it != "Elige una opción" } as? String

        if (puerto == null || latitud == null || terminal == null || longitud == null || fotoONT == null) {
            showToast("Completa todos los campos para continuar")
            return
        }

        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            Foto_Puerto = fotoONT,
            Latitud_Terminal = latitud,
            Longitud_Terminal = longitud,
            Puerto = puerto,
            Terminal = terminal,
            Step_Registro = 5
        )
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
        val fragmentA = MenuRegistrando()
        preferencesManager.saveString("boton7","listo7")

        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragmentA)
            .commit()
    }

    // Mostrar mensaje y salir
    private fun showToastAndExit(message: String) {
        showToast(message)
        requireActivity().supportFragmentManager.popBackStack()
        (activity as? Registrando)?.goToNextStep(7)
    }

    // Mostrar mensaje
    private fun showToast(message: String) {
        (requireActivity() as? Registrando)?.toasting(message)
    }

    /**
    * Actualiza las opciones del spinner de puertos.
    */
    private fun updateSpinners() {
        val numbersArray = resources.getStringArray(R.array.numbersPuerto).toMutableList()
        numbersArray.add(0, "Elige una opción")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, numbersArray).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerPuerto.adapter = adapter
    }
}
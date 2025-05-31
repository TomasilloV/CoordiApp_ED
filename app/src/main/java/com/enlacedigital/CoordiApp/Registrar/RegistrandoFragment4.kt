package com.enlacedigital.CoordiApp.Registrar

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.MenuRegistrando
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.createImageFile
import com.enlacedigital.CoordiApp.utils.encodeImageToBase64
import java.io.File
import java.io.IOException

class RegistrandoFragment4 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    private lateinit var photoUri: Uri
    private lateinit var btnFotoFachada: Button
    private lateinit var btnFotoOS: Button
    private lateinit var editDistrito: EditText
    private lateinit var spinnerTecnologia: Spinner
    private lateinit var spinnerTipoReparacion: Spinner
    private lateinit var spinnerCambioConector: Spinner
    private lateinit var spinnerCambioModem: Spinner
    private lateinit var editMetraje: EditText


    /** Tipo de foto actualmente en uso (e.g., "fachada", "fotoOS"). */
    private var currentPhotoType: String = ""
    private var fachada: String? = null
    private var fotoOS: String? = null
    private var currentPhotoPath: String = ""

    /** Lanzador para tomar una foto con la cámara. */
    private val takePhotoLauncher: ActivityResultLauncher<Uri?> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) handleCameraPhoto()
        }

    /** Lanzador para seleccionar una foto desde la galería. */
    private val pickPhotoLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleGalleryPhoto(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registrando4, container, false)
        val btnrecargar = view.findViewById<Button>(R.id.btnrecargar)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

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
                    .replace(R.id.swiperefresh, RegistrandoFragment4())
                    .commit()
            }, 1500)

        }
        Thread.sleep(200L)
        btnrecargar.isEnabled = true
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkSession(apiService, requireContext(), null as Class<Nothing>?)

        btnFotoFachada = view.findViewById(R.id.btnFachada)
        btnFotoOS = view.findViewById(R.id.btnFotoOs)
        editDistrito = view.findViewById(R.id.editDistrito)
        spinnerTecnologia = view.findViewById(R.id.spinnerTecnologia)
        val textTipoReparacion = view.findViewById<TextView>(R.id.textTipoReparacion)
        spinnerTipoReparacion = view.findViewById(R.id.spinnerTipoReparacion)
        val textCambioConector = view.findViewById<TextView>(R.id.textCambioConector)
        spinnerCambioConector = view.findViewById(R.id.spinnerCambioConector)
        val textCambioModem = view.findViewById<TextView>(R.id.textCambioModem)
        spinnerCambioModem = view.findViewById(R.id.spinnerCambioModem)
        editMetraje = view.findViewById(R.id.editMetraje)
        setupSpinners(spinnerTecnologia)
        var QuejaMigra = preferencesManager.getString("QUEJAMIGRA")
        if (QuejaMigra == "SI")
        {
            textTipoReparacion.visibility = View.VISIBLE
            spinnerTipoReparacion.visibility = View.VISIBLE
            setupSpinners2(spinnerTipoReparacion)
        }
        else
        {
            textTipoReparacion.visibility = View.GONE
            spinnerTipoReparacion.visibility = View.GONE
        }

        setupSpinners(spinnerTipoReparacion,spinnerCambioConector,spinnerCambioModem,textCambioConector,textCambioModem)

        setupListeners()
        val instalacionOptions = resources.getStringArray(R.array.instalacion_options)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            instalacionOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        view.findViewById<Spinner>(R.id.spinnerInstalacion).adapter = adapter

        view.findViewById<Button>(R.id.next).setOnClickListener { validateAndProceed() }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(requireContext())
                    .setTitle("¿Estás seguro?")
                    .setMessage("¿Seguro que quieres salir del paso de Registro Servicio y volver al menu de pasos?")
                    .setPositiveButton("Sí") { _, _ ->
                        isEnabled = false
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.main, MenuRegistrando())
                            .commit()
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        })
    }

    private fun setupSpinners(vararg spinners: Spinner) {
        val tecnologiaOptions = listOf("Elige una opción", "FIBRA", "COBRE")
        setupSpinner(spinners[0], tecnologiaOptions)
    }
    private fun setupSpinners2(vararg spinners: Spinner) {
        val tecnologiaOptions = listOf("Elige una opción", "Cambios de conector", "Jumper óptico","Terminales atenuadas","Reaprovisionamiento","Cambio de modem")
        setupSpinner(spinners[0], tecnologiaOptions)
    }
    private fun setupSpinnerConector(vararg spinners: Spinner) {
        val tecnologiaOptions = listOf("Elige una opción", "Terminal", "Modem")
        setupSpinner(spinners[0], tecnologiaOptions)
    }
    private fun setupSpinnerModem(vararg spinners: Spinner) {
        val tecnologiaOptions = listOf("Elige una opción", "Dañado", "Defectuoso")
        setupSpinner(spinners[0], tecnologiaOptions)
    }

    private fun setupSpinner(spinner: Spinner, items: List<String?>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupSpinners(spinnerTipoReparacion: Spinner, spinnerCambioConector: Spinner, spinnerCambioModem: Spinner,
                              textCambioConector: TextView, textCambioModem: TextView) {
        spinnerTipoReparacion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTipoReparacion = spinnerTipoReparacion.selectedItem?.takeIf { it != "Elige una opción" } as? String
                if (selectedTipoReparacion == "Cambios de conector")
                {
                    toggleViewVisibility(spinnerCambioModem, textCambioModem, View.GONE)
                    toggleViewVisibility(spinnerCambioConector, textCambioConector, View.VISIBLE)
                    setupSpinnerConector(spinnerCambioConector)
                }
                else if(selectedTipoReparacion == "Cambio de modem")
                {
                    toggleViewVisibility(spinnerCambioConector, textCambioConector, View.GONE)
                    toggleViewVisibility(spinnerCambioModem, textCambioModem, View.VISIBLE)
                    setupSpinnerModem(spinnerCambioModem)
                } else
                {
                    hideViews(spinnerCambioConector, spinnerCambioModem, textCambioConector, textCambioModem)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                hideViews(spinnerCambioConector, spinnerCambioModem, textCambioConector, textCambioModem)
            }
        }
    }

    private fun hideViews(spinnerCambioConector: Spinner, spinnerCambioModem: Spinner, textCambioConector: TextView, textCambioModem: TextView)
    {
        spinnerCambioModem.visibility = View.GONE
        spinnerCambioConector.visibility = View.GONE
        textCambioModem.visibility = View.GONE
        textCambioConector.visibility = View.GONE
    }

    private fun toggleViewVisibility(view: View, textView: TextView, visibility: Int) {
        view.visibility = visibility
        textView.visibility = visibility
    }

    /**
     * Configura los listeners para los botones de interacción.
     */
    private fun setupListeners() {
        btnFotoFachada.setOnClickListener { showPhotoOptions("fachada") }
        btnFotoOS.setOnClickListener { showPhotoOptions("fotoOS") }
    }

    /**
     * Muestra las opciones para tomar una foto.
     * @param photoType Tipo de foto que se tomará (e.g., "fachada", "fotoOS").
     */
    private fun showPhotoOptions(photoType: String) {
        currentPhotoType = photoType
        takePhoto()
    }

    /**
     * Lanza la galería para seleccionar una foto.
     */
    private fun choosePhotoFromGallery() {
        pickPhotoLauncher.launch("image/*")
    }

    /**
     * Inicia el proceso para tomar una foto con la cámara.
     */
    private fun takePhoto() {
        val (photoFile, photoPath) = try {
            createImageFile(requireContext())
        } catch (ex: IOException) {
            (requireActivity() as? Registrando)?.toasting("Error al crear la imagen")
            null to ""
        }

        photoFile?.let {
            currentPhotoPath = photoPath
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "com.enlacedigital.CoordiApp.fileprovider",
                it
            )
            takePhotoLauncher.launch(photoUri)
        }
    }

    /**
     * Maneja la foto seleccionada desde la galería.
     * @param uri URI de la imagen seleccionada.
     */
    private fun handleGalleryPhoto(uri: Uri) {
        val file = try {
            requireActivity().contentResolver.openInputStream(uri)?.use { input ->
                File.createTempFile("gallery_image", ".jpg", requireContext().cacheDir).apply {
                    outputStream().use { output -> input.copyTo(output) }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

        file?.let {
            val imageData = encodeImageToBase64(it)
            updatePhoto(currentPhotoType, imageData)
        } ?: (requireActivity() as? Registrando)?.toasting("Error al manejar la imagen seleccionada")
    }

    /**
     * Maneja la foto tomada con la cámara.
     */
    private fun handleCameraPhoto() {
        val file = File(currentPhotoPath)
        if (file.exists()) {
            val imageData = encodeImageToBase64(file)
            updatePhoto(currentPhotoType, imageData)
        } else {
            (requireActivity() as? Registrando)?.toasting("No se encontró la foto")
        }
    }

    /**
     * Actualiza el contenido de las fotos según el tipo seleccionado.
     * @param photoType Tipo de foto ("fachada", "fotoOS").
     * @param base64 Imagen en formato Base64.
     */
    private fun updatePhoto(photoType: String, base64: String) {
        when (photoType) {
            "fachada" -> {
                fachada = base64
                btnFotoFachada.text = "Cambiar foto"
                btnFotoFachada.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.light_gray)
                )
            }
            "fotoOS" -> {
                fotoOS = base64
                btnFotoOS.text = "Cambiar foto"
                btnFotoOS.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.light_gray)
                )
            }
        }
    }

    /**
     * Valida los campos del formulario y continúa con el proceso de registro.
     */
    private fun validateAndProceed() {
        val instalacion = view?.findViewById<Spinner>(R.id.spinnerInstalacion)?.selectedItem
            ?.takeIf { it != "Elige una opción" } as? String
        val distritoText = editDistrito.text.toString().takeIf { it.isNotBlank() }
        val selectedTecnologia = spinnerTecnologia.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val selectedTipoReparacion = spinnerTipoReparacion.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val metraje = editMetraje.text.toString().takeIf { it.isNotBlank() }
        val selectedCambioModem = spinnerCambioModem.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val selectedCambioConector = spinnerCambioConector.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val opcionTipoReparacion:String?
        if (selectedCambioModem == null)
        {
            opcionTipoReparacion = selectedCambioModem
        }
        else if (selectedCambioConector == null)
        {
            opcionTipoReparacion = selectedCambioConector
        }
        else
        {
            opcionTipoReparacion = "No asignado"
        }
        var QuejaMigra = preferencesManager.getString("QUEJAMIGRA")
        if (QuejaMigra == "SI")
        {
            if (instalacion == null /*|| fachada == null */ || fotoOS == null || distritoText.isNullOrBlank() || selectedTecnologia == null || selectedTipoReparacion == null) {
                (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
                return
            }
        }
        else
        {
            if (instalacion == null /*|| fachada == null */ || fotoOS == null || distritoText.isNullOrBlank() || selectedTecnologia == null || metraje == null) {
                (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
                return
            }
        }
        if (distritoText.length < 7) {
            (requireActivity() as? Registrando)?.toasting("El distrito debe tener al menos 7 caracteres")
            return
        }

        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            Tipo_Instalacion = instalacion,
            Distrito = distritoText,
            Metraje = metraje?.toInt(),
            Tecnologia = selectedTecnologia,
            Tipo_sub_reparaviob = opcionTipoReparacion,
            //Foto_Casa_Cliente = fachada,
            Foto_INE = fotoOS,
            Step_Registro = 2
        )
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
        val fragmentA = MenuRegistrando()
        preferencesManager.saveString("boton4","listo4")

        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragmentA)
            .commit()
    }
}

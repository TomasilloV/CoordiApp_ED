package com.enlacedigital.CoordiApp.Registrar

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.Menu
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.createImageFile
import com.enlacedigital.CoordiApp.utils.showToast
import java.io.File
import java.io.IOException
import com.enlacedigital.CoordiApp.utils.encodeImageToBase64
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.showPhotoOptions
import com.enlacedigital.CoordiApp.utils.extractTextFromImage
import com.enlacedigital.CoordiApp.utils.startNewActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrandoFragment2 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    private lateinit var photoUri: Uri
    private lateinit var editMetraje: EditText
    private lateinit var telmexSN: TextView
    private lateinit var editTerminal: EditText
    private lateinit var spinnerPuerto: Spinner
    private lateinit var btnFotoOnt: Button
    private lateinit var btnFotoSerie: Button
    private var currentPhotoType: String = ""
    private var fotoONT: String? = null
    private var fotoSerie: String? = null
    private var currentPhotoPath: String = ""
    private lateinit var spinnerOnt: Spinner
    private lateinit var loadingLayout: FrameLayout
    private var lastSelectedOnt: String? = null
    private var idOnt: Int? = null
    private var serieOntFoto: String? = null

    private val takePhotoLauncher: ActivityResultLauncher<Uri?> = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) handleCameraPhoto()
    }

    private val pickPhotoLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleGalleryPhoto(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_registrando2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkSession(apiService, requireContext(), null as Class<Nothing>?)
        initializeViews(view)
        setupListeners()
        updateSpinners()
        getOptions("6", idTecnico = preferencesManager.getString("id_tecnico")!!.toInt() )
    }

    private fun initializeViews(view: View) {
        editMetraje = view.findViewById(R.id.editMetraje)
        editTerminal = view.findViewById(R.id.editTerminal)
        spinnerPuerto = view.findViewById(R.id.spinnerPuerto)
        btnFotoOnt = view.findViewById(R.id.btnFotoOnt)
        btnFotoSerie = view.findViewById(R.id.btnFotoSerie)
        spinnerOnt = view.findViewById(R.id.spinnerOnt)
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        telmexSN = view.findViewById(R.id.telmexSN)
        loadingLayout.setOnTouchListener { _, _ -> loadingLayout.visibility == View.VISIBLE }

        with(view) {
            findViewById<Button>(R.id.btnFotoOnt).setOnClickListener { showPhotoOptions("ont") }
            findViewById<Button>(R.id.btnFotoSerie).setOnClickListener { showPhotoOptions("serie") }
            findViewById<Button>(R.id.next).setOnClickListener { validateAndProceed() }
        }
    }

    private fun getOptions(step: String, idEstado: Int? = null, idMunicipio: Int? = null, idTecnico: Int) {
        loadingLayout.setLoadingVisibility(true)
        apiService.options(step, idEstado, idMunicipio, idTecnico)
            .enqueue(object : Callback<List<Option>> {
                override fun onResponse(
                    call: Call<List<Option>>,
                    response: Response<List<Option>>
                ) {
                    loadingLayout.setLoadingVisibility(false)
                    if (response.isSuccessful) {
                        val options = response.body() ?: emptyList()
                        updateOntSpinner(options)
                    } else {
                        requireContext().showToast("Error: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<List<Option>>, t: Throwable) {
                    loadingLayout.setLoadingVisibility(false)
                    requireContext().showToast("Failed: ${t.message}")
                }
            })
    }

    private fun updateOntSpinner(options: List<Option>) {
        val ont = options.mapNotNull { it.Num_Serie_Salida_Det }.sorted()
        setupSpinnerAndListener(spinnerOnt, ont) { selectedOnt ->
            if (selectedOnt != lastSelectedOnt) {
                lastSelectedOnt = selectedOnt
                idOnt = options.find { it.Num_Serie_Salida_Det == selectedOnt }?.idSalidas
            }
        }
    }

    private fun setupSpinnerAndListener(spinner: Spinner, data: List<String>, onItemSelected: (String?) -> Unit) {
        val options = mutableListOf("Elige una opción").apply { addAll(data) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent.getItemAtPosition(position) as? String
                onItemSelected(selected.takeIf { it != "Elige una opción" })
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupListeners() {
        btnFotoOnt.setOnClickListener {
            showPhotoOptions("ont")
            currentPhotoType = "ont"}
        btnFotoSerie.setOnClickListener {
            showPhotoOptions("serie")
            currentPhotoType = "serie" }
        view?.findViewById<Button>(R.id.next)?.setOnClickListener { validateAndProceed() }
    }

    private fun updateSpinners() {
        val numbersArray = resources.getStringArray(R.array.numbersPuerto).toMutableList()
        numbersArray.add(0, "Elige una opción")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, numbersArray).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerPuerto.adapter = adapter
    }

    private fun showPhotoOptions(photoType: String) {
        /*currentPhotoType = photoType
        showPhotoOptions(
            requireContext(),
            photoType,
            ::takePhoto,
            ::choosePhotoFromGallery
        )*/
        takePhoto()
    }

    private fun choosePhotoFromGallery() {
        pickPhotoLauncher.launch("image/*")
    }

    private fun takePhoto() {
        val (photoFile, photoPath) = try {
            createImageFile(requireContext())
        } catch (ex: IOException) {
            requireContext().showToast("Error al crear el archivo de imagen.")
            null to ""
        }

        photoFile?.let {
            currentPhotoPath = photoPath
            photoUri = FileProvider.getUriForFile(requireContext(), "com.enlacedigital.CoordiApp.fileprovider", it)
            takePhotoLauncher.launch(photoUri)
        }
    }

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
            if(currentPhotoType == "serie") processImage(it)
            val imageData = encodeImageToBase64(it)
            updatePhoto(currentPhotoType, imageData)
        } ?: requireContext().showToast("Error al manejar la imagen seleccionada.")
    }

    private fun handleCameraPhoto() {
        val file = File(currentPhotoPath)
        if (file.exists()) {
            if(currentPhotoType == "serie") processImage(file)
            val imageData = encodeImageToBase64(file)
            updatePhoto(currentPhotoType, imageData)
        } else {
            requireContext().showToast("No se encontró la foto.")
        }
    }

    private fun processImage(file: File) {
        extractTextFromImage(
            imageFile = file,
            onSuccess = { extractedText ->
                val regex = Regex("(Telmex\\s*)?S/N\\s*:?\\s*([A-Z0-9\\-]+)")
                val matchResult = regex.find(extractedText!!)
                serieOntFoto = matchResult?.groups?.get(2)?.value

                if (serieOntFoto != null) {
                    telmexSN.text = "Telmex S/N: $serieOntFoto"
                } else {
                    telmexSN.text = "No se reconoce el número de serie, intenta tomar una mejor foto"
                }
            },
            onFailure = { errorMessage -> telmexSN.text = errorMessage }
        )
    }

    private fun updatePhoto(photoType: String, base64: String) {
        when (photoType) {
            "ont" -> {
                fotoONT = base64
                btnFotoOnt.text = "Cambiar foto"
                btnFotoOnt.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
            "serie" -> {
                fotoSerie = base64
                btnFotoSerie.text = "Cambiar foto"
                btnFotoSerie.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
        }
    }

    private fun validateAndProceed() {
        val metraje = editMetraje.text.toString().takeIf { it.isNotBlank() }
        val terminal = editTerminal.text.toString().takeIf { it.isNotBlank() }
        val puerto = spinnerPuerto.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val ont = spinnerOnt.selectedItem?.takeIf { it != "Elige una opción" } as? String

        if (metraje == null || terminal == null || puerto == null || fotoONT == null || fotoSerie == null /*|| ont == null*/) {
            requireContext().showToast("Por favor, completa todas las opciones para continuar.")
            return
        }

        if (ont != serieOntFoto) {
            requireContext().showToast("El serie ONT seleccionado y de la foto son diferentes")
            return
        }

        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            Metraje = metraje.toInt(),
            Terminal = terminal,
            Puerto = puerto,
            Foto_Ont = fotoONT,
            No_Serie_ONT = fotoSerie,
            Ont = lastSelectedOnt,
            idOnt = idOnt,
            Step_Registro = 2
        )
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
    }
}
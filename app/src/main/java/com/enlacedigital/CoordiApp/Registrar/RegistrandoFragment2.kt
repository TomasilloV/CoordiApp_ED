package com.enlacedigital.CoordiApp.Registrar

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.Menu
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.createImageFile
import com.enlacedigital.CoordiApp.utils.encodeImageToBase64
import com.enlacedigital.CoordiApp.utils.extractTextFromImage
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.startNewActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException


class RegistrandoFragment2 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    private lateinit var photoUri: Uri
    private lateinit var editMetraje: EditText
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
        //fetchOptionsAndSetupSpinner(preferencesManager.getString("id_tecnico")!!.toInt())
    }

    private fun initializeViews(view: View) {
        editMetraje = view.findViewById(R.id.editMetraje)
        editTerminal = view.findViewById(R.id.editTerminal)
        spinnerPuerto = view.findViewById(R.id.spinnerPuerto)
        btnFotoOnt = view.findViewById(R.id.btnFotoOnt)
        btnFotoSerie = view.findViewById(R.id.btnFotoSerie)
        spinnerOnt = view.findViewById(R.id.spinnerOnt)
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        loadingLayout.setLoadingVisibility(false)

    }

    private fun fetchOptionsAndSetupSpinner(idTecnico: Int, idEstado: Int? = null, idMunicipio: Int? = null) {
        loadingLayout.setLoadingVisibility(true)
        val step = "6"
        apiService.options(step, idEstado, idMunicipio, idTecnico)
            .enqueue(object : Callback<List<Option>> {
                override fun onResponse(ignoredCall: Call<List<Option>>, response: Response<List<Option>>) {
                    loadingLayout.setLoadingVisibility(false)
                    if (response.isSuccessful) {
                        val options = response.body()?.mapNotNull { it.Num_Serie_Salida_Det }?.sorted() ?: emptyList()
                        val allOptions = listOf("Elige una opción", "ZTEG2429F9E2") + options
                        spinnerOnt.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allOptions).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }
                        spinnerOnt.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                val selectedOnt = (parent.getItemAtPosition(position) as? String)?.takeIf { it != "Elige una opción" }
                                if (selectedOnt != null && selectedOnt != lastSelectedOnt) {
                                    lastSelectedOnt = selectedOnt
                                    idOnt = response.body()?.find { it.Num_Serie_Salida_Det == selectedOnt }?.idSalidas
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                    } else {
                        (requireActivity() as? Registrando)?.toasting("Error: ${response.message()}")
                    }
                }

                override fun onFailure(ignoredCall: Call<List<Option>>, t: Throwable) {
                    loadingLayout.setLoadingVisibility(false)
                    (requireActivity() as? Registrando)?.toasting("Failed: ${t.message}")
                }
            })
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
        currentPhotoType = photoType
        /*showPhotoOptions(
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
            (requireActivity() as? Registrando)?.toasting("Error al crear la imagen")
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
            //if(currentPhotoType == "serie") processImage(it)
            val imageData = encodeImageToBase64(it)
            updatePhoto(currentPhotoType, imageData)
        } ?: (requireActivity() as? Registrando)?.toasting("Error al manejar la imagen seleccionada")
    }

    private fun handleCameraPhoto() {
        val file = File(currentPhotoPath)
        if (file.exists()) {
            if(currentPhotoType == "serie") processImage(file)
            val imageData = encodeImageToBase64(file)
            updatePhoto(currentPhotoType, imageData)
        } else {
            (requireActivity() as? Registrando)?.toasting("No se encontró la foto")
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
                    btnFotoSerie.text = serieOntFoto
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("No se reconoce el número de serie")
                        .setMessage("Puedes tomar una foto más legible para obtener el número de serie o continuar sin proporcionarlo")
                        .setPositiveButton("Tomar de nuevo") { _, _ ->
                            showPhotoOptions("serie")
                        }
                        .setNegativeButton("Continuar") { _, _ ->
                        }
                        .setCancelable(false)
                        .show()
                }
            },
            onFailure = { errorMessage -> AlertDialog.Builder(requireContext())
                .setTitle("Ocurrió un error")
                .setMessage(errorMessage)
                .setPositiveButton("Ok") { _, _ ->
                }
                .setCancelable(false)
                .show()  }
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
        //val ont = spinnerOnt.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val ont = serieOntFoto

        if (metraje == null || terminal == null || puerto == null || fotoONT == null || fotoSerie == null) {
            (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
            return
        }

        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            Metraje = metraje.toInt(),
            Terminal = terminal,
            Puerto = puerto,
            Foto_Ont = fotoONT,
            No_Serie_ONT = fotoSerie,
            Ont = ont,
            /*idOnt = idOnt,*/
            Step_Registro = 2
        )
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
    }
}
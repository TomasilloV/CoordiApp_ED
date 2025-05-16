package com.enlacedigital.CoordiApp.Registrar

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.createImageFile
import com.enlacedigital.CoordiApp.utils.encodeImageToBase64
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException

/**
 * Fragmento que representa la segunda etapa del proceso de registro.
 */
class RegistrandoFragment3 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    private lateinit var photoUri: Uri
    //private lateinit var spinnerPuerto: Spinner
    private lateinit var spinnerOnt: Spinner
    private lateinit var btnFotoOnt: Button
    private var fotoONT: String? = null
    private var currentPhotoType: String = ""
    private var currentPhotoPath: String = ""
    private lateinit var btnFotoSerie: Button
    private var fotoSerie: String? = null
    private lateinit var loadingLayout: FrameLayout
    private var lastSelectedOnt: String? = null
    private var idOnt: Int? = null

    /**
     * Launcher para tomar fotos con la cámara.
     */
    private val takePhotoLauncher: ActivityResultLauncher<Uri?> = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) handleCameraPhoto()
    }

    /**
     * Launcher para seleccionar fotos desde la galería.
     */
    private val pickPhotoLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleGalleryPhoto(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_registrando3, container, false)
        val btnrecargar = view.findViewById<Button>(R.id.btnrecargar)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar2)

        btnrecargar.setOnClickListener {
            btnrecargar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                parentFragmentManager.beginTransaction()
                    .replace(R.id.swiperefresh, RegistrandoFragment3())
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
        initializeViews(view)
        setupListeners()
        //updateSpinners()
        fetchOptionsAndSetupSpinner(preferencesManager.getString("id_tecnico")!!.toInt())
    }

    private fun initializeViews(view: View) {
        //spinnerPuerto = view.findViewById(R.id.spinnerPuerto)
        spinnerOnt = view.findViewById(R.id.spinnerOnt)
        btnFotoOnt = view.findViewById(R.id.btnFotoOnt)
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        loadingLayout.setLoadingVisibility(false)
        btnFotoSerie = view.findViewById(R.id.btnFotoSerie)
    }
    /**
     * Obtiene opciones desde el servicio API y configura el spinner correspondiente.
     * @param idTecnico ID del técnico.
     * @param idEstado (Opcional) ID del estado.
     * @param idMunicipio (Opcional) ID del municipio.
     */
    private fun fetchOptionsAndSetupSpinner(idTecnico: Int, idEstado: Int? = null, idMunicipio: Int? = null) {
        loadingLayout.setLoadingVisibility(true)
        val step = "6"
        apiService.options(step, idEstado, idMunicipio, idTecnico)
            .enqueue(object : Callback<List<Option>> {
                override fun onResponse(ignoredCall: Call<List<Option>>, response: Response<List<Option>>) {
                    loadingLayout.setLoadingVisibility(false)
                    if (response.isSuccessful) {
                        val options = response.body()?.mapNotNull { it.Num_Serie_Salida_Det }?.sorted() ?: emptyList()
                        val allOptions = listOf("Elige una opción") + options
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

    /**
     * Configura los listeners de los eventos de la interfaz de usuario.
     */
    private fun setupListeners() {
        btnFotoOnt.setOnClickListener { showPhotoOptions("ont") }
        btnFotoSerie.setOnClickListener { showPhotoOptions("serie") }
        view?.findViewById<Button>(R.id.next)?.setOnClickListener { validateAndProceed() }
    }

    /**
     * Actualiza las opciones del spinner de puertos.
     */
    /*private fun updateSpinners() {
        val numbersArray = resources.getStringArray(R.array.numbersPuerto).toMutableList()
        numbersArray.add(0, "Elige una opción")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, numbersArray).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerPuerto.adapter = adapter
    }*/

    /**
     * Muestra las opciones para tomar o elegir una foto.
     */
    private fun showPhotoOptions(photoType: String) {
        currentPhotoType = photoType
        takePhoto()
    }

    /**
     * Inicia la selección de una foto desde la galería.
     */
    private fun choosePhotoFromGallery() {
        pickPhotoLauncher.launch("image/*")
    }

    /**
     * Inicia la captura de una foto con la cámara.
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
            photoUri = FileProvider.getUriForFile(requireContext(), "com.enlacedigital.CoordiApp.fileprovider", it)
            takePhotoLauncher.launch(photoUri)
        }
    }

    /**
     * Maneja la selección de una foto desde la galería.
     * @param uri URI de la foto seleccionada.
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
            //if(currentPhotoType == "serie") processImage(it)
            val imageData = encodeImageToBase64(it)
            updatePhoto(currentPhotoType, imageData)
        } ?: (requireActivity() as? Registrando)?.toasting("Error al manejar la imagen seleccionada")
    }

    /**
     * Maneja la captura de una foto con la cámara.
     */
    private fun handleCameraPhoto() {
        val file = File(currentPhotoPath)
        if (file.exists()) {
            /**if (currentPhotoType == "serie") processImage(file)*/
            val imageData = encodeImageToBase64(file)
            updatePhoto(currentPhotoType, imageData)
        } else {
            (requireActivity() as? Registrando)?.toasting("No se encontró la foto")
        }
    }

    /**
     * Procesa la imagen para extraer el texto relevante.
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
    .setNegativeButton("Continuar") { _, _ -> }
    .setCancelable(false)
    .show()
    }
    },
    onFailure = { errorMessage ->
    AlertDialog.Builder(requireContext())
    .setTitle("Ocurrió un error")
    .setMessage(errorMessage)
    .setPositiveButton("Ok") { _, _ -> }
    .setCancelable(false)
    .show()
    }
    )
    }
     */
    /**
     * Actualiza la foto actual almacenada con la nueva imagen capturada o seleccionada.
     * @param photoType Tipo de foto (ont o serie).
     * @param base64 Imagen codificada en formato Base64.
     */
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

    /**
     * Valida los campos obligatorios y procede a guardar los datos.
     */
    private fun validateAndProceed() {
        //val puerto = spinnerPuerto.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val ont = spinnerOnt.selectedItem?.takeIf { it != "Elige una opción" } as? String

        if (/*puerto == null ||*/ fotoONT == null || fotoSerie == null) {
            (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
            return
        }

        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            //Puerto = puerto,
            Foto_Ont = fotoONT,
            Ont = lastSelectedOnt,
            No_Serie_ONT = fotoSerie,
            idOnt = idOnt,
            Step_Registro = 3
        )
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
    }
}

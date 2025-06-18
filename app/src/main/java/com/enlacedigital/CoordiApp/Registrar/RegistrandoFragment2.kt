package com.enlacedigital.CoordiApp.Registrar

import android.app.AlertDialog
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
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.Menu
import com.enlacedigital.CoordiApp.MenuRegistrando
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.models.ApiResponse
import com.enlacedigital.CoordiApp.models.FolioRequest
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.models.TacResponse
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragmento que representa la segunda etapa del proceso de registro.
 */
class RegistrandoFragment2 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    private lateinit var photoUri: Uri
    private lateinit var editMetraje: EditText
    // private lateinit var editTerminal: EditText
    private lateinit var spinnerOnt: Spinner
    private lateinit var editTarea: EditText
    //private lateinit var btnFotoSerie: Button
    private var currentPhotoType: String = ""
    //private var fotoSerie: String? = null
    private var currentPhotoPath: String = ""
    private lateinit var loadingLayout: FrameLayout
    private var lastSelectedOnt: String? = null
    private var idOnt: Int? = null

    /**
     * Launcher para tomar fotos con la cámara.
     */
    /*private val takePhotoLauncher: ActivityResultLauncher<Uri?> = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) handleCameraPhoto()
    }*/

    /**
     * Launcher para seleccionar fotos desde la galería.
     */
    /*private val pickPhotoLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleGalleryPhoto(it) }
    }*/

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_registrando2, container, false)
        val btnrecargar = view.findViewById<Button>(R.id.btnrecargar)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar2)
        getTac()
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
                    .replace(R.id.swiperefresh, RegistrandoFragment2())
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
    }

    private fun initializeViews(view: View) {
        editMetraje = view.findViewById(R.id.editMetraje)
        editTarea = view.findViewById(R.id.editTarea)
        // editTerminal = view.findViewById(R.id.editTerminal)
        //btnFotoSerie = view.findViewById(R.id.btnFotoSerie)
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        loadingLayout.setLoadingVisibility(false)
    }

    /**
     * Configura los listeners de los eventos de la interfaz de usuario.
     */
    private fun setupListeners() {
        /*btnFotoSerie.setOnClickListener {
            showPhotoOptions()
            currentPhotoType = "serie"
        }*/
        view?.findViewById<Button>(R.id.next)?.setOnClickListener { validateAndProceed() }
    }

    /**
     * Muestra las opciones para tomar o elegir una foto.
     */
    /*private fun showPhotoOptions() {
        currentPhotoType = "serie"
        takePhoto()
    }*/

    /**
     * Inicia la selección de una foto desde la galería.
     */
    /*private fun choosePhotoFromGallery() {
        pickPhotoLauncher.launch("image/*")
    }*/*/

    /**
     * Inicia la captura de una foto con la cámara.
     */
    /*private fun takePhoto() {
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
    }*/

    /**
     * Maneja la selección de una foto desde la galería.
     * @param uri URI de la foto seleccionada.
     */
    /*private fun handleGalleryPhoto(uri: Uri) {
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
    }*/

    /**
     * Maneja la captura de una foto con la cámara.
     */
    /*private fun handleCameraPhoto() {
        val file = File(currentPhotoPath)
        if (file.exists()) {
            /**if (currentPhotoType == "serie") processImage(file)*/
            val imageData = encodeImageToBase64(file)
            updatePhoto(currentPhotoType, imageData)
        } else {
            (requireActivity() as? Registrando)?.toasting("No se encontró la foto")
        }
    }*/

    /**
     * Procesa la imagen para extraer el texto relevante.
     * @paramfile Archivo de la imagen.
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
    /*private fun updatePhoto(photoType: String, base64: String) {
        when (photoType) {
            "serie" -> {
                fotoSerie = base64
                btnFotoSerie.text = "Cambiar foto"
                btnFotoSerie.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
        }
    }*/

    /**
     * Valida los campos obligatorios y procede a guardar los datos.
     */
    private fun validateAndProceed() {
        val tarea = view?.findViewById<EditText>(R.id.editTarea)?.text.toString().takeIf { it.isNotBlank() }
        val metraje = editMetraje.text.toString().takeIf { it.isNotBlank() }
        // val terminal = editTerminal.text.toString().takeIf { it.isNotBlank() }

        if (metraje == null /* || terminal == null || fotoSerie == null || tarea == null*/) {
            (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
            return
        }
        val boton1= preferencesManager.getString("boton1")
        val boton3= preferencesManager.getString("boton3")
        val boton4 = preferencesManager.getString("boton4")
        val boton5= preferencesManager.getString("boton5")
        val boton7= preferencesManager.getString("boton7")
        var step = 2
        if (boton1 == "listo1" && boton3 == "listo3" && boton4 == "listo4" && boton7 == "listo7")
        {
            step = 5
        }
        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            Tipo_Tarea = tarea,
            Metraje = metraje.toInt(),
            //Terminal = terminal,
            //No_Serie_ONT = fotoSerie,
            Step_Registro = step
        )
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
        val fragmentA = MenuRegistrando()
        preferencesManager.saveString("boton2","listo2")

        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragmentA)
            .commit()
    }

    private fun getTac() {
        val folio = preferencesManager.getString("folio-pisa")!!.toInt()
        val request = FolioRequest(folio)

        apiService.obtenertac(request).enqueue(object : Callback<TacResponse> {
            override fun onResponse(call: Call<TacResponse>, response: Response<TacResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null && body.items.isNotEmpty()) {
                        val item = body.items[0]

                        val handler = Handler(Looper.getMainLooper())

                        handler.postDelayed({
                            editTarea.setText(item.tipTarea)
                        }, 600)

                    } else {
                        (requireActivity() as? Registrando)?.toasting("Sin datos para este folio")
                    }

                } else {
                    (requireActivity() as? Registrando)?.toasting("Errorpaso2: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<TacResponse>, t: Throwable) {
                (requireActivity() as? Registrando)?.toasting("Fallo de red: ${t.message}")
            }
        })
    }
}

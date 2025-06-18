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
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Spinner
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
import com.enlacedigital.CoordiApp.models.ApiResponse
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.models.requestpasos
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri?>


    /**
     * Launcher para tomar fotos con la cámara.
     */
    private fun setupPhotoLauncher() {
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) handleCameraPhoto()
        }
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
        val btnRegresar = view.findViewById<Button>(R.id.btnRegresar)
        setupPhotoLauncher()

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
        setupListeners(view)
        //updateSpinners()
        fetchOptionsAndSetupSpinner(preferencesManager.getString("id_tecnico")!!.toInt(),view)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(requireContext())
                    .setTitle("¿Estás seguro?")
                    .setMessage("¿Seguro que quieres salir del paso de ONT/Modem y volver al menu de pasos?")
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
    private fun fetchOptionsAndSetupSpinner(idTecnico: Int, view: View,idEstado: Int? = null, idMunicipio: Int? = null) {
        loadingLayout.setLoadingVisibility(true)
        val step = "6"
        apiService.options(step, idEstado, idMunicipio, idTecnico)
            .enqueue(object : Callback<List<Option>> {
                override fun onResponse(ignoredCall: Call<List<Option>>, response: Response<List<Option>>) {
                    loadingLayout.setLoadingVisibility(false)
                    if (response.isSuccessful) {
                        Log.d("DebugONT",""+response.body())
                        val options = response.body()?.mapNotNull { it.Num_Serie_Salida_Det }?.sorted() ?: emptyList()
                        val allOptions = listOf("Elige una opción") + options
                        val autoCompleteTextView = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
                        val items= allOptions

                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
                        autoCompleteTextView.setAdapter(adapter)
                    } else {
                        (requireActivity() as? Registrando)?.toasting("Error: ${response.message()}")
                    }
                }

                override fun onFailure(ignoredCall: Call<List<Option>>, t: Throwable) {
                    Log.d("DebugONT",""+t.message)
                    loadingLayout.setLoadingVisibility(false)
                }
            })
    }


    /**
     * Configura los listeners de los eventos de la interfaz de usuario.
     */
    private fun setupListeners(viewt: View) {
        btnFotoOnt.setOnClickListener { showPhotoOptions("ont") }
        Log.d("BUGLOCO","si pasa0")
        btnFotoSerie.setOnClickListener { showPhotoOptions("serie") }
        Log.d("BUGLOCO","si pasa10")
        view?.findViewById<Button>(R.id.next)?.setOnClickListener { validateAndProceed(viewt) }
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
        Log.d("BUGLOCO","si pasa1")
        currentPhotoType = photoType
        Log.d("BUGLOCO","si pasa2")
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
        Log.d("BUGLOCO","si pasa3")
        val (photoFile, photoPath) = try {
            Log.d("BUGLOCO","si pasa4")
            createImageFile(requireContext())
        } catch (ex: IOException) {
            Log.d("BUGLOCO","si pasa5")
            (requireActivity() as? Registrando)?.toasting("Error al crear la imagen")
            null to ""
        }
        Log.d("BUGLOCO","si pasa6")

        photoFile?.let {
            Log.d("BUGLOCO","si pasa7")
            currentPhotoPath = photoPath
            Log.d("BUGLOCO","si pasa8")
            photoUri = FileProvider.getUriForFile(requireContext(), "com.enlacedigital.CoordiApp.fileprovider", it)
            Log.d("BUGLOCO","si pasa9"+photoUri)
            takePhotoLauncher.launch(photoUri)
            Log.d("BUGLOCO","si pasa99")
        }
        Log.d("BUGLOCO","si pasa100")
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
    private fun validateAndProceed(view: View) {
        //val puerto = spinnerPuerto.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val ont = spinnerOnt.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val autoCompleteTextView = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
        val ontnew =  autoCompleteTextView.text .toString()
        Log.d("TextONTValidate",""+ontnew)

        if (/*puerto == null ||*/ fotoONT == null || fotoSerie == null) {
            (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
            return
        }
        val boton1= preferencesManager.getString("boton1")
        val boton3= preferencesManager.getString("boton3")
        val boton4 = preferencesManager.getString("boton4")
        val boton5= preferencesManager.getString("boton5")
        val boton7= preferencesManager.getString("boton7")
        var step = 3
        if (boton1 == "listo1" && boton4 == "listo4" && boton5 == "listo5" && boton7 == "listo7")
        {
            step = 5
        }
        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            //Puerto = puerto,
            Foto_Ont = fotoONT,
            Ont = ontnew,
            No_Serie_ONT = fotoSerie,
            idOnt = idOnt,
            Step_Registro = step
        )
        Log.d("FragmentDebug",""+updateRequest.No_Serie_ONT)
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
        pasoscomp()
    }


    private fun pasoscomp()
    {
        Log.d("CobreDebug", "ENTRO AL METODO")
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fechaActual = Date()
        val fecha = formato.format(fechaActual).toString()
        val folio = preferencesManager.getString("folio-pisa")!!.toInt()
        val requestpaso = requestpasos(
            Folio_Pisa = folio,
            Paso_3 = 1,
            fecha_ultimo_avance = fecha
        )
        Log.d("CobreDebug","fecha: "+fecha)
        Log.d("CobreDebug","folio: "+folio)
        apiService.registropasos(requestpaso).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("CobreDebug", "Código HTTP: ${response.code()}")
                Log.d("CobreDebug", "Es exitoso: ${response.isSuccessful}")
                Log.d("CobreDebug", "Raw body: ${
                    response.errorBody()?.string()
                }\")\nug1")
                Log.d("CobreDebug","Mensaje: ${response.message()}\"")
                if (response.isSuccessful) {
                    Log.d("CobreDebug","Se pudoooo")
                    val fragmentA = MenuRegistrando()
                    preferencesManager.saveString("boton3","listo3")

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main, fragmentA)
                        .commit()
                } else {
                    Log.d("CobreDebug","No se pudoooo")
                    (requireActivity() as? Registrando)?.toasting("ErrorCobre: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.d("CobreDebug","No se pudoooo X1000")
                (requireActivity() as? Registrando)?.toasting("Fallo de red: ${t.message}")
            }
        })
    }
}

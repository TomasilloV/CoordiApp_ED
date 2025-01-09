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

class RegistrandoFragment2_1 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    private lateinit var photoUri: Uri
    private lateinit var btnFotoOnt: Button
    private lateinit var btnFotoSerie: Button
    private lateinit var btnFotoHoja: Button
    private var currentPhotoType: String = ""
    private var fotoONT: String? = null
    private var fotoSerie: String? = null
    private var fotoHoja: String? = null
    private var currentPhotoPath: String = ""
    private var serieOntFoto: String? = null
    private lateinit var loadingLayout: FrameLayout

    private val takePhotoLauncher: ActivityResultLauncher<Uri?> = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) handleCameraPhoto()
    }

    private val pickPhotoLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleGalleryPhoto(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_registrando2_1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkSession(apiService, requireContext(), null as Class<Nothing>?)
        initializeViews(view)
        setupListeners()
    }

    private fun initializeViews(view: View) {
        btnFotoOnt = view.findViewById(R.id.btnFotoOnt)
        btnFotoSerie = view.findViewById(R.id.btnFotoSerie)
        btnFotoHoja = view.findViewById(R.id.btnFotoHoja)
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        loadingLayout.setLoadingVisibility(false)

    }

    private fun setupListeners() {
        btnFotoOnt.setOnClickListener {
            showPhotoOptions("ont")
            currentPhotoType = "ont"}
        btnFotoSerie.setOnClickListener {
            showPhotoOptions("serie")
            currentPhotoType = "serie" }
        btnFotoHoja.setOnClickListener {
            showPhotoOptions("hoja")
            currentPhotoType = "hoja" }
        view?.findViewById<Button>(R.id.next)?.setOnClickListener { validateAndProceed() }
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
            if(currentPhotoType == "serie") processImage(it)
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
            "hoja" -> {
                fotoHoja = base64
                btnFotoHoja.text = "Cambiar foto"
                btnFotoHoja.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
        }
    }

    private fun validateAndProceed() {
        if (fotoONT == null || fotoSerie == null || fotoHoja == null) {
            (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
            return
        }

        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            Tipo_Tarea = tarea,
            Foto_Ont = fotoONT,
            No_Serie_ONT = fotoSerie,
            Ont = ont,
            /*idOnt = idOnt,*/
            Step_Registro = 2
        )
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
    }
}
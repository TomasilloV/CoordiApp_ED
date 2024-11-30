package com.enlacedigital.CoordiApp.Registrar

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
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


class RegistrandoFragment3 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var photoUri: Uri
    private lateinit var btnFotoFachada: Button
    private lateinit var btnFotoOS: Button
    private var currentPhotoType: String = ""
    private var fachada: String? = null
    private var fotoOS: String? = null
    private var currentPhotoPath: String = ""

    private val takePhotoLauncher: ActivityResultLauncher<Uri?> = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) handleCameraPhoto()
    }

    private val pickPhotoLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleGalleryPhoto(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registrando3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkSession(apiService, requireContext(), null as Class<Nothing>?)

        btnFotoFachada = view.findViewById(R.id.btnFachada)
        btnFotoOS = view.findViewById(R.id.btnFotoOs)

        setupListeners()

        val instalacionOptions = resources.getStringArray(R.array.instalacion_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, instalacionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        view.findViewById<Spinner>(R.id.spinnerInstalacion).adapter = adapter

        view.findViewById<Button>(R.id.next).setOnClickListener { validateAndProceed() }
    }

    private fun setupListeners() {
        btnFotoFachada.setOnClickListener { showPhotoOptions("fachada") }
        btnFotoOS.setOnClickListener { showPhotoOptions("fotoOS") }
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
            val imageData = encodeImageToBase64(it)
            updatePhoto(currentPhotoType, imageData)
        } ?: (requireActivity() as? Registrando)?.toasting("Error al manejar la imagen seleccionada")
    }

    private fun handleCameraPhoto() {
        val file = File(currentPhotoPath)
        if (file.exists()) {
            val imageData = encodeImageToBase64(file)
            updatePhoto(currentPhotoType, imageData)
        } else {
            (requireActivity() as? Registrando)?.toasting("No se encontró la foto")
        }
    }

    private fun updatePhoto(photoType: String, base64: String) {
        when (photoType) {
            "fachada" -> {
                fachada = base64
                btnFotoFachada.text = "Cambiar foto"
                btnFotoFachada.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
            "fotoOS" -> {
                fotoOS = base64
                btnFotoOS.text = "Cambiar foto"
                btnFotoOS.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
        }
    }

    private fun validateAndProceed() {
        val tarea = view?.findViewById<EditText>(R.id.editTarea)?.text.toString().takeIf { it.isNotBlank() }
        val instalacion = view?.findViewById<Spinner>(R.id.spinnerInstalacion)?.selectedItem?.takeIf { it != "Elige una opción" } as? String

        if (tarea == null || instalacion == null || fachada == null || fotoOS == null) {
            (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
            return
        }

            val updateRequest = ActualizarBD(
                idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
                Tipo_Tarea = tarea,
                Tipo_Instalacion = instalacion,
                Foto_Casa_Cliente = fachada,
                Foto_INE = fotoOS,
                Step_Registro = 3
            )
            (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
    }
}

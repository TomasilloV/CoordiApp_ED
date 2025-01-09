package com.enlacedigital.CoordiApp.Registrar

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.core.content.FileProvider
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.models.ActualizarBD
import java.io.File
import java.io.IOException
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.createImageFile
import com.enlacedigital.CoordiApp.utils.showPhotoOptions
import com.enlacedigital.CoordiApp.utils.encodeImageToBase64

class RegistrandoFragment4 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var photoUri: Uri
    private lateinit var btnFotoPuerto: Button
    private var currentPhotoType: String = ""
    private var fotoPuerto: String = ""
    private var currentPhotoPath: String = ""

    /**
     * Launcher para capturar fotos con la cámara.
     */
    private val takePhotoLauncher: ActivityResultLauncher<Uri?> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) handleCameraPhoto()
        }

    /**
     * Launcher para seleccionar una imagen desde la galería.
     */
    private val pickPhotoLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleGalleryPhoto(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registrando4, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verifica la sesión antes de continuar
        checkSession(apiService, requireContext(), null as Class<Nothing>?)

        // Inicializa las vistas del fragmento
        btnFotoPuerto = view.findViewById(R.id.btnFotoPuerto)
        val nextButton: Button = view.findViewById(R.id.next)
        val editTitular: EditText = view.findViewById(R.id.editTitular)
        val editApPaterno: EditText = view.findViewById(R.id.editApPaterno)
        val editMaterno: EditText = view.findViewById(R.id.editMaterno)
        val editRecibe: EditText = view.findViewById(R.id.editRecibe)
        val editCliente: EditText = view.findViewById(R.id.editCliente)

        // Configura los listeners para botones y campos
        btnFotoPuerto.setOnClickListener { showPhotoOptions() }
        nextButton.setOnClickListener {
            val titular = editTitular.text.toString().trim()
            val apPaterno = editApPaterno.text.toString().trim()
            val materno = editMaterno.text.toString().trim()
            val recibe = editRecibe.text.toString().trim()
            val cliente = editCliente.text.toString().trim()

            // Validación de campos antes de proceder
            if (titular.isEmpty() || apPaterno.isEmpty() || materno.isEmpty() || recibe.isEmpty() || cliente.isEmpty() || fotoPuerto == "") {
                (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
                return@setOnClickListener
            } else if (cliente.length <= 9) {
                (requireActivity() as? Registrando)?.toasting("Ingresa un teléfono válido")
                return@setOnClickListener
            } else if (titular.length < 3 || apPaterno.length < 3 || materno.length < 3 || recibe.length < 3) {
                (requireActivity() as? Registrando)?.toasting("Ingresa los nombres válidos")
                return@setOnClickListener
            }

            // Crea la solicitud de actualización y la envía
            val updateRequest = ActualizarBD(
                idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
                Cliente_Titular = titular,
                Apellido_Paterno_Titular = apPaterno,
                Apellido_Materno_Titular = materno,
                Cliente_Recibe = recibe,
                Telefono_Cliente = cliente,
                Foto_Puerto = fotoPuerto,
                Step_Registro = 4
            )
            (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
        }
    }

    /**
     * Muestra las opciones para tomar una foto o seleccionar una desde la galería.
     */
    private fun showPhotoOptions() {
        currentPhotoType = "fotoPuerto"
        showPhotoOptions(
            requireContext(),
            ::takePhoto,
            ::choosePhotoFromGallery
        )
    }

    /**
     * Lanza el selector de imágenes de la galería.
     */
    private fun choosePhotoFromGallery() {
        pickPhotoLauncher.launch("image/*")
    }

    /**
     * Captura una foto utilizando la cámara.
     */
    private fun takePhoto() {
        val (photoFile, photoPath) = try {
            createImageFile(requireContext())
        } catch (ex: IOException) {
            (requireActivity() as? Registrando)?.toasting("Error al crear el archivo de imagen")
            null to ""
        }

        photoFile?.let {
            currentPhotoPath = photoPath
            photoUri = FileProvider.getUriForFile(requireContext(), "com.enlacedigital.CoordiApp.fileprovider", it)
            takePhotoLauncher.launch(photoUri)
        }
    }

    /**
     * Maneja una imagen seleccionada desde la galería.
     *
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
     * Maneja una foto tomada con la cámara.
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
     * Actualiza la foto seleccionada.
     *
     * @param photoType Tipo de foto (ej. "fotoPuerto").
     * @param base64 Cadena codificada en base64 de la imagen.
     */
    private fun updatePhoto(photoType: String, base64: String) {
        when (photoType) {
            "fotoPuerto" -> {
                fotoPuerto = base64
                btnFotoPuerto.text = "Cambiar foto"
                btnFotoPuerto.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
        }
    }
}

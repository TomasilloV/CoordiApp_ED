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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.content.FileProvider
import com.enlacedigital.CoordiApp.LocationHelper
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AlertDialog
import com.enlacedigital.CoordiApp.MenuRegistrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.models.ApiResponse
import com.enlacedigital.CoordiApp.models.ONTCOBRE
import com.enlacedigital.CoordiApp.models.TacResponse
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.encodeImageToBase64
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.createImageFile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.toString

class RegistrandoFragmentCobre : Fragment(R.layout.fragment_registrando_cobre) {

    // Propiedades
    private lateinit var editSKU: EditText
    private lateinit var  editSerieCobre: EditText
    private lateinit var btnFotoONTCobre1: Button
    private lateinit var btnFotoONTCobre2: Button
    private lateinit var loadingLayout: FrameLayout
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri?>
    private var currentPhotoType: String = ""
    private var fotoCobre1: String? = null
    private var fotoCobre2: String? = null
    private var currentPhotoPath: String = ""
    private val preferencesManager = PreferencesHelper.getPreferencesManager()
    private val apiService = ApiServiceHelper.getApiService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupPhotoLauncher()
        val view = inflater.inflate(R.layout.fragment_registrando_cobre, container, false)
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
        setupListeners(view)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(requireContext())
                    .setTitle("¿Estás seguro?")
                    .setMessage("¿Seguro que quieres salir del paso de ONT/Cobre y volver al menu de pasos?")
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

    // Inicialización de vistas
    private fun initializeViews(view: View) {
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        editSKU = view.findViewById(R.id.editSKU)
        editSerieCobre = view.findViewById(R.id.editOntCobre)
        btnFotoONTCobre1 = view.findViewById(R.id.btnFotoOntCobre1)
        btnFotoONTCobre2 = view.findViewById(R.id.btnFotoOntCobre2)
        loadingLayout.setLoadingVisibility(false)
    }

    // Configuración de listeners
    private fun setupListeners(viewt: View) {
        btnFotoONTCobre1.setOnClickListener { showPhotoOptions("Cobre1") }
        btnFotoONTCobre2.setOnClickListener { showPhotoOptions("Cobre2") }
        view?.findViewById<Button>(R.id.next)?.setOnClickListener { validateAndProceed(viewt) }
    }

    // Configuración del lanzador de fotos
    private fun setupPhotoLauncher() {
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) handleCameraPhoto()
        }
    }

    private fun showPhotoOptions(photoType: String) {
        currentPhotoType = photoType
        takePhoto()
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
            updatePhoto(currentPhotoType, imageData)
        } else {
            showToast("No se encontró la foto")
        }
    }

    // Actualización de la foto
    private fun updatePhoto(photoType: String, base64: String) {
        when (photoType) {
            "Cobre1" -> {
                fotoCobre1 = base64
                btnFotoONTCobre1.text = "Cambiar foto"
                btnFotoONTCobre1.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
            "Cobre2" -> {
                fotoCobre2 = base64
                btnFotoONTCobre2.text = "Cambiar foto"
                btnFotoONTCobre2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
            }
        }
    }

    // Validación y envío de datos
    private fun validateAndProceed(view: View) {
        val folio = preferencesManager.getString("folio-pisa")!!.toInt()
        val SKU = editSKU.text.toString().takeIf { it.isNotBlank() }
        val SerieCobre = editSerieCobre.text.toString().takeIf { it.isNotBlank() }
        if (SerieCobre == null || fotoCobre1 == null || fotoCobre2 == null) {
            showToast("Completa todos los campos para continuar")
            return
        }
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fechaActual = Date()
        val fecha = formato.format(fechaActual)

        val updateRequest = ONTCOBRE(
            FK_Folio_Pisa_Cobre = folio,
            Fecha = fecha,
            Num_Serie_Ont_Cobre = SerieCobre,
            Foto_Ont_Cobre_Detras = fotoCobre2,
            Foto_Ont_Cobre_Delante = fotoCobre1
        )

        apiService.ontCobre(updateRequest).enqueue(object : Callback<ApiResponse> {
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
                    preferencesManager.saveString("botonCobre","listoCobre")

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
}
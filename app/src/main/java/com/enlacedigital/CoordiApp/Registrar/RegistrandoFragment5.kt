package com.enlacedigital.CoordiApp.Registrar

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.core.content.FileProvider
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.models.ActualizarBD
import java.io.File
import java.io.IOException
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.enlacedigital.CoordiApp.MenuRegistrando
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.createImageFile
import com.enlacedigital.CoordiApp.utils.showPhotoOptions
import com.enlacedigital.CoordiApp.utils.encodeImageToBase64

class RegistrandoFragment5 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var photoUri: Uri
    private var currentPhotoType: String = ""
    private var currentPhotoPath: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registrando5, container, false)
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
                    .replace(R.id.swiperefresh, RegistrandoFragment5())
                    .commit()
            }, 1500)

        }
        Thread.sleep(200L)
        btnrecargar.isEnabled = true
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(requireContext())
                    .setTitle("¿Estás seguro?")
                    .setMessage("¿Seguro que quieres salir del paso de Cliente y volver al menu de pasos?")
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

        // Verifica la sesión antes de continuar
        checkSession(apiService, requireContext(), null as Class<Nothing>?)

        // Inicializa las vistas del fragmento
        val nextButton: Button = view.findViewById(R.id.next)
        val editTitular: EditText = view.findViewById(R.id.editTitular)
        val editApPaterno: EditText = view.findViewById(R.id.editApPaterno)
        val editMaterno: EditText = view.findViewById(R.id.editMaterno)
        val editRecibe: EditText = view.findViewById(R.id.editRecibe)
        val editCliente: EditText = view.findViewById(R.id.editCliente)

        // Configura los listeners para botones y campos
        nextButton.setOnClickListener {
            val titular = editTitular.text.toString().trim()
            val apPaterno = editApPaterno.text.toString().trim()
            val materno = editMaterno.text.toString().trim()
            val recibe = editRecibe.text.toString().trim()
            val cliente = editCliente.text.toString().trim()

            // Validación de campos antes de proceder
            if (titular.isEmpty() || apPaterno.isEmpty() || materno.isEmpty() || recibe.isEmpty() || cliente.isEmpty()) {
                (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
                return@setOnClickListener
            } else if (cliente.length <= 9) {
                (requireActivity() as? Registrando)?.toasting("Ingresa un teléfono válido")
                return@setOnClickListener
            } else if (titular.length < 3 || apPaterno.length < 3 || materno.length < 3 || recibe.length < 3) {
                (requireActivity() as? Registrando)?.toasting("Ingresa los nombres válidos")
                return@setOnClickListener
            }
            val boton1= preferencesManager.getString("boton1")
            val boton3= preferencesManager.getString("boton3")
            val boton4 = preferencesManager.getString("boton4")
            val boton5= preferencesManager.getString("boton5")
            val boton7= preferencesManager.getString("boton7")
            var step = 4
            if (boton1 == "listo1" && boton3 == "listo3" && boton4 == "listo4" && boton7 == "listo7")
            {
                step = 5
            }
            // Crea la solicitud de actualización y la envía
            val updateRequest = ActualizarBD(
                idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
                Cliente_Titular = titular,
                Apellido_Paterno_Titular = apPaterno,
                Apellido_Materno_Titular = materno,
                Cliente_Recibe = recibe,
                Telefono_Cliente = cliente,
                Step_Registro = step
            )
            Log.d("dataDebug",""+updateRequest)
            (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
            val fragmentA = MenuRegistrando()
            preferencesManager.saveString("boton5","listo5")

            parentFragmentManager.beginTransaction()
                .replace(R.id.main, fragmentA)
                .commit()
        }
    }
}
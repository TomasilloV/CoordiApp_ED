package com.enlacedigital.CoordiApp.Registrar

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrandoFragment5 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private var selectedCodigoPostal: Int? = null
    private lateinit var loadingLayout: FrameLayout

    private lateinit var spinnerEstado: Spinner
    private lateinit var spinnerCiudad: Spinner
    private lateinit var spinnerColonia: Spinner
    private lateinit var editCalle: EditText
    private lateinit var editNumeroExterior: EditText
    private lateinit var nextButton: Button
    private lateinit var textCiudad: TextView
    private lateinit var textColonia: TextView

    private var lastSelectedEstado: String? = null
    private var lastSelectedCiudad: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registrando5, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        getOptions("5e")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews(view: View) {
        spinnerEstado = view.findViewById(R.id.spinnerEstado)
        spinnerCiudad = view.findViewById(R.id.spinnerCiudad)
        spinnerColonia = view.findViewById(R.id.spinnerColonia)
        editCalle = view.findViewById(R.id.editCalle)
        editNumeroExterior = view.findViewById(R.id.editNumeroExterior)
        nextButton = view.findViewById(R.id.next)

        textCiudad = view.findViewById(R.id.textCiudad)
        textColonia = view.findViewById(R.id.textColonia)
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        loadingLayout.setOnTouchListener { _, _ -> loadingLayout.visibility == VISIBLE }
    }

    private fun setupListeners() {
        nextButton.setOnClickListener {
            handleNextButtonClick()
        }
    }

    private fun handleNextButtonClick() {
        val estado = spinnerEstado.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val ciudad = spinnerCiudad.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val colonia = spinnerColonia.selectedItem?.takeIf { it != "Elige una opción" } as? String
        val calle = editCalle.text.toString().takeIf { it.isNotBlank() }
        val numeroExterior = editNumeroExterior.text.toString().takeIf { it.isNotBlank() }

        if (estado == null || ciudad == null || colonia == null || calle == null || numeroExterior == null || selectedCodigoPostal == null) {
            (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
            return
        }


            val updateRequest = ActualizarBD(
                idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
                Direccion_Cliente = "$calle $numeroExterior, $colonia, $selectedCodigoPostal, $ciudad, $estado",
                Step_Registro = 5
            )
            (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
    }

    private fun getOptions(step: String, idEstado: Int? = null, idMunicipio: Int? = null) {
        loadingLayout.setLoadingVisibility(true)
        apiService.options(step, idEstado, idMunicipio).enqueue(object : Callback<List<Option>> {
            override fun onResponse(ignoredCall: Call<List<Option>>, response: Response<List<Option>>) {
                loadingLayout.setLoadingVisibility(false)
                if (response.isSuccessful) {
                    val options = response.body() ?: emptyList()
                    handleOptionsResponse(step, options, idEstado, idMunicipio)
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

    private fun handleOptionsResponse(step: String, options: List<Option>, idEstado: Int?, idMunicipio: Int?) {
        when (step) {
            "5e" -> updateEstadoSpinner(options)
            "5m" -> {
                updateCiudadSpinner(options, idEstado)
                textCiudad.visibility = VISIBLE
                spinnerCiudad.visibility = VISIBLE
            }
            "5c" -> {
                updateColoniaSpinner(options, idMunicipio)
                textColonia.visibility = VISIBLE
                spinnerColonia.visibility = VISIBLE
            }
        }
    }

    private fun updateEstadoSpinner(options: List<Option>) {
        val estados = options.mapNotNull { it.nameEstado }.distinct().sorted()
        setupSpinnerAndListener(spinnerEstado, estados) { selectedEstado ->
            if (selectedEstado != lastSelectedEstado) {
                lastSelectedEstado = selectedEstado
                val idEstado = options.find { it.nameEstado == selectedEstado }?.idEstado
                if (idEstado != null) {
                    getOptions("5m", idEstado = idEstado)
                } else {
                    hideCiudadColonia()
                }
            }
        }
    }

    private fun updateCiudadSpinner(options: List<Option>, idEstado: Int?) {
        val ciudades = options.filter { it.estadoMunicipio == idEstado }
            .mapNotNull { it.nameMunicipio }
            .distinct()
            .sorted()

        setupSpinnerAndListener(spinnerCiudad, ciudades) { selectedCiudad ->
            if (selectedCiudad != lastSelectedCiudad) {
                lastSelectedCiudad = selectedCiudad
                val idMunicipio = options.find { it.nameMunicipio == selectedCiudad }?.idMunicipio
                if (idMunicipio != null) {
                    getOptions("5c", idMunicipio = idMunicipio)
                } else {
                    hideColonia()
                }
            }
        }
    }

    private fun updateColoniaSpinner(options: List<Option>, idMunicipio: Int?) {
        val colonias = options.filter { it.idMunicipio == idMunicipio }
            .mapNotNull { it.nameColonia }
            .distinct()
            .sorted()

        setupSpinnerAndListener(spinnerColonia, colonias) { selectedColonia ->
            selectedCodigoPostal = options.find { it.nameColonia == selectedColonia }?.CodigoPostal
        }
    }

    private fun setupSpinnerAndListener(
        spinner: Spinner,
        data: List<String>,
        onItemSelected: (String?) -> Unit
    ) {
        val options = mutableListOf("Elige una opción").apply { addAll(data) }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position) as? String
                onItemSelected(selected.takeIf { it != "Elige una opción" })
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun hideCiudadColonia() {
        textCiudad.visibility = GONE
        textColonia.visibility = GONE
        spinnerCiudad.visibility = GONE
        spinnerColonia.visibility = GONE
    }

    private fun hideColonia() {
        textColonia.visibility = GONE
        spinnerColonia.visibility = GONE
    }
}

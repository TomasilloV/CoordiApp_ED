package com.enlacedigital.CoordiApp.Registrar

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility

class RegistrandoFragment6 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var spinnerOnt: Spinner
    private lateinit var editFolio: EditText
    private lateinit var nextButton: Button
    private lateinit var loadingLayout: FrameLayout

    private var lastSelectedOnt: String? = null
    private var idOnt: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registrando6, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*initViews(view)
        setupListeners()
        val idTecnico = preferencesManager.getString("id_tecnico")!!.toInt()
        getOptions("6", idTecnico = idTecnico )*/
    }/*

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews(view: View) {
        spinnerOnt = view.findViewById(R.id.spinnerOnt)
        editFolio = view.findViewById(R.id.editFolio)
        editFolio.setText(preferencesManager.getString("folio") ?: "")
        nextButton = view.findViewById(R.id.next)
        loadingLayout = view.findViewById(R.id.loadingOverlay)
        loadingLayout.setOnTouchListener { _, _ -> loadingLayout.visibility == View.VISIBLE }
    }

    private fun getOptions(
        step: String,
        idEstado: Int? = null,
        idMunicipio: Int? = null,
        idTecnico: Int
    ) {
        loadingLayout.setLoadingVisibility(true)
        apiService.options(step, idEstado, idMunicipio, idTecnico)
            .enqueue(object : Callback<List<Option>> {
                override fun onResponse(
                    call: Call<List<Option>>,
                    response: Response<List<Option>>
                ) {
                    loadingLayout.setLoadingVisibility(false)
                    if (response.isSuccessful) {
                        val options = response.body() ?: emptyList()
                        updateOntSpinner(options)
                    } else {
                        showToast("Error: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<List<Option>>, t: Throwable) {
                    loadingLayout.setLoadingVisibility(false)
                    showToast("Failed: ${t.message}")
                }
            })
    }

    private fun updateOntSpinner(options: List<Option>) {
        val ont = options.mapNotNull { it.Num_Serie_Salida_Det }.distinct().sorted()
        setupSpinnerAndListener(spinnerOnt, ont) { selectedOnt ->
            if (selectedOnt != lastSelectedOnt) {
                lastSelectedOnt = selectedOnt
                idOnt = options.find { it.Num_Serie_Salida_Det == selectedOnt }?.idSalidas
            }
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
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent.getItemAtPosition(position) as? String
                onItemSelected(selected.takeIf { it != "Elige una opción" })
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupListeners() {
        nextButton.setOnClickListener {
            handleNextButtonClick()
        }
    }

    private fun handleNextButtonClick() {
        val ont = spinnerOnt.selectedItem?.takeIf { it != "Elige una opción" } as? String

        if (ont == null) {
            showToast(this, "Por favor, completa todas las opciones para continuar.")
            return
        }

        val updateRequest = ActualizarBD(
            idtecnico_instalaciones_coordiapp = preferencesManager.getString("id")!!,
            Ont = lastSelectedOnt,
            idOnt = idOnt,
            Step_Registro = 6
        )
        (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
    }*/
}
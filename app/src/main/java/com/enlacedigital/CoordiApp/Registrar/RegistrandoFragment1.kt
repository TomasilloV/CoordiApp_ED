package com.enlacedigital.CoordiApp.Registrar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface ActualizadBDListener {
    fun updateTechnicianData(requestData: ActualizarBD)
}

class RegistrandoFragment1 : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    private lateinit var spinnerDivision: Spinner
    private lateinit var spinnerArea: Spinner
    private lateinit var spinnerCope: Spinner
    private lateinit var editDistrito: EditText
    private lateinit var spinnerTecnologia: Spinner
    private lateinit var spinnerEstatus: Spinner
    private lateinit var textArea: TextView
    private lateinit var textCope: TextView

    private var options: List<Option> = listOf()
    private var divisionMap: Map<Int, List<Option>> = mapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registrando1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkSession(apiService, requireContext(), null as Class<Nothing>?)
        setupViews(view)
        getOptions()
    }

    private fun setupViews(view: View) {
        spinnerCope = view.findViewById(R.id.spinnerCope)
        spinnerDivision = view.findViewById(R.id.spinnerDivision)
        spinnerArea = view.findViewById(R.id.spinnerArea)
        editDistrito = view.findViewById(R.id.editDistrito)
        spinnerTecnologia = view.findViewById(R.id.spinnerTecnologia)
        spinnerEstatus = view.findViewById(R.id.spinnerEstatus)
        textArea = view.findViewById(R.id.textArea)
        textCope = view.findViewById(R.id.textCope)
        val nextButton = view.findViewById<Button>(R.id.next)

        setupSpinners(spinnerTecnologia, spinnerEstatus)
        setupDivisionSpinner(spinnerDivision, spinnerArea, spinnerCope, textArea, textCope)
        setupButtonClick(nextButton, editDistrito, spinnerTecnologia, spinnerEstatus, spinnerCope, spinnerDivision, spinnerArea)
    }

    private fun setupSpinners(vararg spinners: Spinner) {
        val tecnologiaOptions = listOf("Elige una opción", "FIBRA", "COBRE")
        val estatusOptions = listOf("Elige una opción", "OBJETADA", "COMPLETADA")
        setupSpinner(spinners[0], tecnologiaOptions)
        setupSpinner(spinners[1], estatusOptions)
    }

    private fun setupDivisionSpinner(spinnerDivision: Spinner, spinnerArea: Spinner, spinnerCope: Spinner,
                                     textArea: TextView, textCope: TextView) {
        spinnerDivision.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedDivisionId = divisionMap.keys.elementAt(position - 1)
                    updateAreasSpinner(selectedDivisionId, spinnerArea)
                    toggleViewVisibility(spinnerArea, textArea, View.VISIBLE)
                    toggleViewVisibility(spinnerCope, textCope, View.GONE)
                } else {
                    hideAreaAndCopeViews(spinnerArea, spinnerCope, textArea, textCope)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                hideAreaAndCopeViews(spinnerArea, spinnerCope, textArea, textCope)
            }
        }

        spinnerArea.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedAreaId = (parent?.adapter as? ArrayAdapter<*>)?.getItem(position)?.let { name ->
                        options.find { it.nameArea == name }?.idAreas
                    }
                    selectedAreaId?.let { updateCopesSpinner(it, spinnerCope) }
                    toggleViewVisibility(spinnerCope, textCope, View.VISIBLE)
                } else {
                    hideCopeView(spinnerCope, textCope)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                hideCopeView(spinnerCope, textCope)
            }
        }
    }

    private fun setupButtonClick(nextButton: Button, editDistrito: EditText, spinnerTecnologia: Spinner, spinnerEstatus: Spinner,
                                 spinnerCope: Spinner, spinnerDivision: Spinner, spinnerArea: Spinner) {
        nextButton.setOnClickListener {
            val distritoText = editDistrito.text.toString().takeIf { it.isNotBlank() }
            val selectedTecnologia = spinnerTecnologia.selectedItem?.takeIf { it != "Elige una opción" } as? String
            val selectedEstatus = spinnerEstatus.selectedItem?.takeIf { it != "Elige una opción" } as? String
            val selectedCopeId = options.find { it.COPE == spinnerCope.selectedItem?.toString() }?.idCope
            val selectedDivisionId = (spinnerDivision.selectedItem as? String)?.let { name ->
                options.find { it.nameDivision == name }?.idDivision
            }
            val selectedAreaId = (spinnerArea.selectedItem as? String)?.let { name ->
                options.find { it.nameArea == name }?.idAreas
            }

            if (!validateFields(distritoText, selectedTecnologia, selectedEstatus, selectedCopeId, selectedDivisionId, selectedAreaId)) {
                return@setOnClickListener
            }
            val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fechaActual = Date()
            val fecha = formato.format(fechaActual)
            preferencesManager.getString("id")!!.let { id ->
                val updateRequest = ActualizarBD(
                    idtecnico_instalaciones_coordiapp = id,
                    Distrito = distritoText,
                    Tecnologia = selectedTecnologia,
                    Estatus_Orden = selectedEstatus,
                    FK_Cope = selectedCopeId,
                    FK_Tecnico_apps = preferencesManager.getString("id_tecnico")!!.toInt(),
                    Fecha_Coordiapp = fecha,
                    Step_Registro = 1
                )
                (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
            } ?: (requireActivity() as? Registrando)?.toasting("Inicia sesión para continuar")
        }
    }

    private fun validateFields(distritoText: String?, selectedTecnologia: String?, selectedEstatus: String?,
                               selectedCopeId: Int?, selectedDivisionId: Int?, selectedAreaId: Int?): Boolean {
        return when {
            distritoText.isNullOrBlank() || selectedTecnologia == null || selectedEstatus == null ||
                    selectedCopeId == null || selectedDivisionId == null || selectedAreaId == null -> {
                (requireActivity() as? Registrando)?.toasting("Completa todos los campos para continuar")
                false
            }
            distritoText.length < 7 -> {
                (requireActivity() as? Registrando)?.toasting("El distrito debe tener al menos 7 caracteres")
                false
            }
            else -> true
        }
    }

    private fun toggleViewVisibility(view: View, textView: TextView, visibility: Int) {
        view.visibility = visibility
        textView.visibility = visibility
    }

    private fun hideAreaAndCopeViews(spinnerArea: Spinner, spinnerCope: Spinner, textArea: TextView, textCope: TextView) {
        spinnerArea.visibility = View.GONE
        spinnerCope.visibility = View.GONE
        textArea.visibility = View.GONE
        textCope.visibility = View.GONE
    }

    private fun hideCopeView(spinnerCope: Spinner, textCope: TextView) {
        spinnerCope.visibility = View.GONE
        textCope.visibility = View.GONE
    }

    private fun setupSpinner(spinner: Spinner, items: List<String?>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun updateAreasSpinner(divisionId: Int, spinnerArea: Spinner) {
        val areas = options.filter { it.idDivision == divisionId }
        val areaOptions = listOf("Elige una opción") + areas.map { it.nameArea }.distinct()
        setupSpinner(spinnerArea, areaOptions)
    }

    private fun updateCopesSpinner(areaId: Int, spinnerCope: Spinner) {
        val copes = options.filter { it.idAreas == areaId }
        val copeOptions = listOf("Elige una opción") + copes.map { it.COPE }
        setupSpinner(spinnerCope, copeOptions)
    }

    private fun getOptions() {
        apiService.options("1").enqueue(object : Callback<List<Option>> {
            override fun onResponse(ignoredCall: Call<List<Option>>, response: Response<List<Option>>) {
                if (response.isSuccessful) {
                    options = response.body() ?: emptyList()
                    divisionMap = options.groupBy { it.idDivision!! }
                    updateSpinners()
                } else {
                    (requireActivity() as? Registrando)?.toasting("Error: ${response.message()}")
                }
            }

            override fun onFailure(ignoredCall: Call<List<Option>>, t: Throwable) {
                (requireActivity() as? Registrando)?.toasting("Failed: ${t.message}")
            }
        })
    }

    private fun updateSpinners() {
        val divisionOptions = listOf("Elige una opción") + divisionMap.values.flatten().map { it.nameDivision }.distinct()
        setupSpinner(view?.findViewById(R.id.spinnerDivision)!!, divisionOptions)
    }
}

package com.enlacedigital.CoordiApp.Registrar

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.R
import com.enlacedigital.CoordiApp.Registrando
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.models.FolioRequest
import com.enlacedigital.CoordiApp.models.TacResponse
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Interface que define un método para actualizar los datos de un técnico en el sistema.
 */
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
        val view = inflater.inflate(R.layout.fragment_registrando1, container, false)
        val btnrecargar = view.findViewById<Button>(R.id.btnrecargar)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        btnrecargar.setOnClickListener {
            btnrecargar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                parentFragmentManager.beginTransaction()
                    .replace(R.id.swiperefresh, RegistrandoFragment1())
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
        setupViews(view)
        getOptions()
        getTac()
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

    /**
     * Configura los spinners con las opciones de tecnología y estatus.
     * @param spinners los spinners a configurar.
     */
    private fun setupSpinners(vararg spinners: Spinner) {
        val tecnologiaOptions = listOf("Elige una opción", "FIBRA", "COBRE")
        val estatusOptions = listOf("Elige una opción", "OBJETADA", "COMPLETADA")
        setupSpinner(spinners[0], tecnologiaOptions)
        setupSpinner(spinners[1], estatusOptions)
    }

    /**
     * Configura el spinner de divisiones y sus respectivas acciones.
     * @param spinnerDivision el spinner para divisiones.
     * @param spinnerArea el spinner para áreas.
     * @param spinnerCope el spinner para copes.
     * @param textArea el TextView asociado al spinner de área.
     * @param textCope el TextView asociado al spinner de cope.
     */
    private fun setupDivisionSpinner(spinnerDivision: Spinner, spinnerArea: Spinner, spinnerCope: Spinner,
                                     textArea: TextView, textCope: TextView) {
        spinnerDivision.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /**
             * Evento cuando se selecciona un ítem en el spinner de división.
             */
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
            /**
             * Evento cuando se selecciona un ítem en el spinner de área.
             */
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

    /**
     * Configura la acción del botón "Siguiente".
     * @param nextButton el botón "Siguiente".
     * @param editDistrito el campo de texto para el distrito.
     * @param spinnerTecnologia el spinner para seleccionar tecnología.
     * @param spinnerEstatus el spinner para seleccionar estatus.
     * @param spinnerCope el spinner para seleccionar COPE.
     * @param spinnerDivision el spinner para seleccionar división.
     * @param spinnerArea el spinner para seleccionar área.
     */
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
            Log.d("Paso1Debug","id: "+preferencesManager.getString("id"))
            Log.d("Paso1Debug","id: "+preferencesManager.getString("id_tecnico"))
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
                Log.d("Paso1Debug","updateRequest: "+updateRequest)
                (activity as? ActualizadBDListener)?.updateTechnicianData(updateRequest)
            } ?: (requireActivity() as? Registrando)?.toasting("Inicia sesión para continuar")
        }
    }

    /**
     * Valida los campos del formulario antes de enviarlos.
     * @param distritoText el texto del distrito.
     * @param selectedTecnologia la tecnología seleccionada.
     * @param selectedEstatus el estatus seleccionado.
     * @param selectedCopeId el ID de COPE seleccionado.
     * @param selectedDivisionId el ID de división seleccionado.
     * @param selectedAreaId el ID de área seleccionado.
     * @return verdadero si todos los campos son válidos, falso en caso contrario.
     */
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

    /**
     * Cambia la visibilidad de una vista y su TextView asociado.
     * @param view la vista cuyo estado se debe cambiar.
     * @param textView el TextView asociado.
     * @param visibility el estado de visibilidad a establecer (View.VISIBLE o View.GONE).
     */
    private fun toggleViewVisibility(view: View, textView: TextView, visibility: Int) {
        view.visibility = visibility
        textView.visibility = visibility
    }

    /**
     * Oculta las vistas de área y COPE.
     * @param spinnerArea el spinner de área.
     * @param spinnerCope el spinner de COPE.
     * @param textArea el TextView de área.
     * @param textCope el TextView de COPE.
     */
    private fun hideAreaAndCopeViews(spinnerArea: Spinner, spinnerCope: Spinner, textArea: TextView, textCope: TextView) {
        spinnerArea.visibility = View.GONE
        spinnerCope.visibility = View.GONE
        textArea.visibility = View.GONE
        textCope.visibility = View.GONE
    }

    /**
     * Oculta las vistas de COPE.
     * @param spinnerCope el spinner de COPE.
     * @param textCope el TextView de COPE.
     */
    private fun hideCopeView(spinnerCope: Spinner, textCope: TextView) {
        spinnerCope.visibility = View.GONE
        textCope.visibility = View.GONE
    }

    /**
     * Configura un spinner con una lista de opciones.
     * @param spinner el spinner a configurar.
     * @param items las opciones que se mostrarán en el spinner.
     */
    private fun setupSpinner(spinner: Spinner, items: List<String?>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    /**
     * Actualiza el spinner de áreas basado en la división seleccionada.
     * @param divisionId el ID de la división seleccionada.
     * @param spinnerArea el spinner de áreas.
     */
    private fun updateAreasSpinner(divisionId: Int, spinnerArea: Spinner) {
        val areas = options.filter { it.idDivision == divisionId }
        val areaOptions = listOf("Elige una opción") + areas.map { it.nameArea }.distinct()
        setupSpinner(spinnerArea, areaOptions)
    }

    /**
     * Actualiza el spinner de COPE basado en el área seleccionada.
     * @param areaId el ID del área seleccionada.
     * @param spinnerCope el spinner de COPE.
     */
    private fun updateCopesSpinner(areaId: Int, spinnerCope: Spinner) {
        val copes = options.filter { it.idAreas == areaId }
        val copeOptions = listOf("Elige una opción") + copes.map { it.COPE }
        setupSpinner(spinnerCope, copeOptions)
    }

    /**
     * Obtiene las opciones para llenar los spinners de división, área y COPE.
     */
    private fun getOptions() {
        apiService.options("1").enqueue(object : Callback<List<Option>> {
            override fun onResponse(ignoredCall: Call<List<Option>>, response: Response<List<Option>>) {
                if (response.isSuccessful) {
                    options = response.body() ?: emptyList()
                    divisionMap = options.groupBy { it.idDivision!! }
                    updateSpinners()
                } else {
                    (requireActivity() as? Registrando)?.toasting("Errorpaso1options: ${response.message()}")
                }
            }

            override fun onFailure(ignoredCall: Call<List<Option>>, t: Throwable) {
                (requireActivity() as? Registrando)?.toasting("Failed: ${t.message}")
            }
        })
    }

    private fun getTac() {
        val folio = preferencesManager.getString("folio-pisa")!!.toInt()
        val request = FolioRequest(folio)

        apiService.obtenertac(request).enqueue(object : Callback<TacResponse> {
            override fun onResponse(call: Call<TacResponse>, response: Response<TacResponse>) {
                Log.d("ValidarDebug1", "Código HTTP: ${response.code()}")
                Log.d("ValidarDebug1", "Es exitoso: ${response.isSuccessful}")
                Log.d("ValidarDebug1", "Mensaje: ${response.message()}")
                Log.d("ValidarDebug1", "Raw body: ${response.errorBody()?.string()}")
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null && body.items.isNotEmpty()) {
                        val item = body.items[0]

                        val handler = Handler(Looper.getMainLooper())

                        handler.postDelayed({
                            val valor = item.nomDivision
                            val index = (spinnerDivision.adapter as ArrayAdapter<String>).getPosition(valor)
                            if (index >= 0) spinnerDivision.setSelection(index)
                        }, 0)

                        handler.postDelayed({
                            val valor1 = item.nomArea
                            val index1 = (spinnerArea.adapter as ArrayAdapter<String>).getPosition(valor1)
                            if (index1 >= 0) spinnerArea.setSelection(index1)
                        }, 200)

                        handler.postDelayed({
                            val valor2 = item.nomCt
                            val index2 = (spinnerCope.adapter as ArrayAdapter<String>).getPosition(valor2)
                            if (index2 >= 0) spinnerCope.setSelection(index2)
                        }, 400)

                        handler.postDelayed({
                            editDistrito.setText(item.distrito)
                        }, 600)

                        handler.postDelayed(
                            {
                                val valor3 = item.tecnologia
                                val index3 = (spinnerTecnologia.adapter as ArrayAdapter<String>).getPosition(valor3)
                                if (index3 >= 0) spinnerTecnologia.setSelection(index3)
                            },800
                        )
                    } else {
                        (requireActivity() as? Registrando)?.toasting("Sin datos para este folio")
                    }

                } else {
                    (requireActivity() as? Registrando)?.toasting("Errorpaso1tac: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<TacResponse>, t: Throwable) {
                (requireActivity() as? Registrando)?.toasting("Fallo de red: ${t.message}")
            }
        })
    }


    /**
     * Actualiza el spinner de divisiones con las opciones obtenidas.
     */
    private fun updateSpinners() {
        val divisionOptions = listOf("Elige una opción") + divisionMap.values.flatten().map { it.nameDivision }.distinct()
        setupSpinner(view?.findViewById(R.id.spinnerDivision)!!, divisionOptions)
    }

}

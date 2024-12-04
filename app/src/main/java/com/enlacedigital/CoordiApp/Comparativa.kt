package com.enlacedigital.CoordiApp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.enlacedigital.CoordiApp.Adapter.FoliosFaltantesAdapter
import com.enlacedigital.CoordiApp.models.ComparativaRequest
import com.enlacedigital.CoordiApp.models.ComparativaResponse
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.showToast
import com.enlacedigital.CoordiApp.utils.startNewActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class Comparativa : AppCompatActivity() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var recyclerView: RecyclerView
    private lateinit var foliosBTN: Button
    private lateinit var loadingLayout: FrameLayout
    private var foliosList: List<ComparativaResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparativa)
        loadingLayout = findViewById(R.id.loadingOverlay)
        loadingLayout.setLoadingVisibility(false)

        val spinnerAnio: Spinner = findViewById(R.id.spinner_anio)
        val spinnerMes: Spinner = findViewById(R.id.spinner_mes)
        val barChart: BarChart = findViewById(R.id.barChart)
        foliosBTN = findViewById(R.id.folios)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupYearSpinner(spinnerAnio, spinnerMes)
        setupMonthSpinner(spinnerAnio, spinnerMes, barChart)

        foliosBTN.setOnClickListener {
            if (foliosBTN.text == "VER FOLIOS" && foliosList.isNotEmpty()) {
                foliosBTN.text = "VER GRÁFICA"
                barChart.isVisible = false
                recyclerView.isVisible = true
            } else if (foliosBTN.text == "VER FOLIOS" && foliosList.isEmpty()) {
                showToast("No hay folios para mostrar")
                foliosBTN.text = "VER FOLIOS"
                barChart.isVisible = true
                recyclerView.isVisible = false
            } else {
                foliosBTN.text = "VER FOLIOS"
                barChart.isVisible = true
                recyclerView.isVisible = false
            }
        }
    }

    private fun setupYearSpinner(spinnerAnio: Spinner, spinnerMes: Spinner) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (2023..currentYear).toList().reversed()

        spinnerAnio.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerAnio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedYear = parent.getItemAtPosition(position) as Int
                val months = resources.getStringArray(R.array.meses_es)
                val validMonths = if (selectedYear == currentYear) {
                    months.slice(0 until Calendar.getInstance().get(Calendar.MONTH) + 1).reversed()
                } else {
                    months.reversed()
                }

                spinnerMes.adapter = ArrayAdapter(this@Comparativa, android.R.layout.simple_spinner_item, validMonths).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupMonthSpinner(spinnerAnio: Spinner, spinnerMes: Spinner, barChart: BarChart) {
        val months = resources.getStringArray(R.array.meses_es)

        spinnerMes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedYear = spinnerAnio.selectedItem as Int
                val selectedMonth = parent.getItemAtPosition(position).toString()
                val mesIndex = months.indexOf(selectedMonth) + 1
                val idTecnico = preferencesManager.getString("id_tecnico")!!.toInt()
                loadingLayout.setLoadingVisibility(true)


                apiService.getComparativa(ComparativaRequest(selectedYear, mesIndex, idTecnico, 1)).enqueue(object : Callback<List<ComparativaResponse>> {
                    override fun onResponse(ignoredCall: Call<List<ComparativaResponse>>, response: Response<List<ComparativaResponse>>) {
                        response.body()?.firstOrNull()?.let { comparativaResponse ->
                            updateBarChart(barChart, comparativaResponse.Registros_Telmex!!, comparativaResponse.Registros_ED!!)
                        }
                    }

                    override fun onFailure(ignoredCall: Call<List<ComparativaResponse>>, t: Throwable) {
                        showToast(  "Error al obtener la comparativa: ${t.message}")
                        loadingLayout.setLoadingVisibility(false)
                        startNewActivity(Menu::class.java)
                    }
                })

                apiService.getComparativa(ComparativaRequest(selectedYear, mesIndex, idTecnico, 2))
                    .enqueue(object : Callback<List<ComparativaResponse>> {
                        override fun onResponse(ignoredCall: Call<List<ComparativaResponse>>, response: Response<List<ComparativaResponse>>) {
                            if (response.isSuccessful) {
                                response.body()?.let { folios ->
                                    foliosList = folios

                                    if (foliosList.isNotEmpty()) {
                                        val adapter = FoliosFaltantesAdapter(foliosList)
                                        recyclerView.adapter = adapter
                                    } else {
                                        foliosBTN.text = "VER FOLIOS"
                                        barChart.isVisible = true
                                        recyclerView.isVisible = false
                                    }
                                }
                            }
                            loadingLayout.setLoadingVisibility(false)
                        }

                        override fun onFailure(ignoredCall: Call<List<ComparativaResponse>>, t: Throwable) {
                            showToast("Error al obtener la comparativa: ${t.message}")
                            loadingLayout.setLoadingVisibility(false)
                            startNewActivity(Menu::class.java)
                        }
                    })

            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateBarChart(barChart: BarChart, registrosTelmex: Int, registrosED: Int) {
        val entries = listOf(
            BarEntry(0f, registrosTelmex.toFloat()), // Telmex
            BarEntry(1f, registrosED.toFloat())      // ED
        )

        val dataSet = BarDataSet(entries, "Comparativa de Registros").apply {
            colors = listOf(
                resources.getColor(R.color.light_blue, theme),
                resources.getColor(R.color.cute_green, theme)
            )
            valueTextSize = 18f
            valueTextColor = resources.getColor(android.R.color.white, theme)
        }

        barChart.apply {
            data = BarData(dataSet)
            legend.isEnabled = false
            description.isEnabled = false
            setFitBars(true)
            setDrawValueAboveBar(false)
            setPinchZoom(false)
            setScaleEnabled(false)
            setBackgroundColor(resources.getColor(android.R.color.transparent, theme))
            animateY(1000)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(listOf("Tac", "CoordiApp"))
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                textSize = 18f
                textColor = resources.getColor(android.R.color.white, theme)
                setDrawGridLines(false)
            }
            barChart.setExtraOffsets(10f, 10f, 10f, 10f)

            axisLeft.textColor = resources.getColor(android.R.color.white, theme)
            axisRight.textColor = resources.getColor(android.R.color.white, theme)
            axisLeft.setDrawGridLines(false)
            axisRight.setDrawGridLines(false)

            invalidate()
        }
    }
}

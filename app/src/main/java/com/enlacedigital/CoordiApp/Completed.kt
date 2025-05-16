package com.enlacedigital.CoordiApp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.FrameLayout
import com.enlacedigital.CoordiApp.Adapter.CompletedAdapter
import com.enlacedigital.CoordiApp.models.Folios
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.showToast
import com.enlacedigital.CoordiApp.utils.checkSession
import com.enlacedigital.CoordiApp.utils.startNewActivity

/**
 * Actividad que muestra los folios completados por el técnico.
 * Esta actividad maneja la carga de los folios y la paginación.
 */
class Completed : AppCompatActivity() {

    /** Gestor de preferencias compartidas */
    val preferencesManager = PreferencesHelper.getPreferencesManager()

    /** Servicio API para realizar peticiones */
    val apiService = ApiServiceHelper.getApiService()

    /** Vista del RecyclerView donde se mostrarán los datos */
    private lateinit var recyclerView: RecyclerView

    /** Adaptador para el RecyclerView */
    private lateinit var itemAdapter: CompletedAdapter

    /** Vista de carga para mostrar mientras se obtienen los datos */
    private lateinit var loadingLayout: FrameLayout

    /** Flag para evitar solicitudes concurrentes de datos */
    private var isLoading = false

    /** Página actual de los datos que se están solicitando */
    private var page = 1

    /** Límite de items por página */
    private val limit = 10

    /** Flag para indicar si se ha llegado al final de los datos */
    private var isLastPage = false

    /**
     * Inicializa la actividad, verifica la sesión y carga los datos.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed)

        // Inicializa vistas y configura eventos
        initViews()

        // Verifica que la sesión sea válida
        checkSession(apiService, this@Completed, null as Class<Nothing>?)

        // Recupera el id del técnico de las preferencias
        val idTecnicoString = preferencesManager.getString("id_tecnico")
        if (idTecnicoString.isNullOrEmpty()) {
            showToast("Por favor, inicia sesión nuevamente.")
            startNewActivity(Login::class.java)
            return
        }
        val idTecnico = idTecnicoString.toInt()

        // Carga los folios completados del técnico
        completed(idTecnico, page)
    }

    /**
     * Inicializa las vistas y los componentes de la actividad.
     */
    private fun initViews() {
        loadingLayout = findViewById(R.id.loading_layout)

        // Configura el RecyclerView
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@Completed)
            itemAdapter = CompletedAdapter(mutableListOf())
            adapter = itemAdapter
            addOnScrollListener(createOnScrollListener()) // Agrega el listener para la paginación
        }

    }

    /**
     * Crea un listener para la paginación al hacer scroll en el RecyclerView.
     */
    private fun createOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            /**
             * Detecta el momento en que el usuario llega al final del RecyclerView
             * para cargar más datos.
             */
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                layoutManager?.let {
                    val visibleItemCount = it.childCount
                    val totalItemCount = it.itemCount
                    val firstVisibleItemPosition = it.findFirstVisibleItemPosition()

                    // Condición para cargar más datos al llegar al final de la lista
                    if (!isLoading && !isLastPage &&
                        (visibleItemCount + firstVisibleItemPosition >= totalItemCount) &&
                        firstVisibleItemPosition >= 0) {
                        page++  // Incrementa la página para la siguiente carga
                        completed(preferencesManager.getString("id_tecnico")!!.toInt(), page)
                    }
                }
            }
        }
    }

    /**
     * Realiza la solicitud para obtener los folios completados.
     *
     * @param idTecnico ID del técnico para filtrar los folios.
     * @param page Página actual para la paginación.
     */
    private fun completed(idTecnico: Int, page: Int) {
        isLoading = true
        loadingLayout.setLoadingVisibility(true)

        apiService.getCompletados(idTecnico, page, limit).enqueue(object : Callback<Folios> {
            /**
             * Maneja la respuesta exitosa de la solicitud.
             */
            override fun onResponse(call: Call<Folios>, response: Response<Folios>) {
                isLoading = false
                loadingLayout.setLoadingVisibility(false)

                if (response.isSuccessful) {
                    handleSuccessResponse(response.body())
                } else {
                    showToast("Error en la respuesta del servidor")
                }
            }

            /**
             * Maneja el fallo de la solicitud de la API.
             */
            override fun onFailure(call: Call<Folios>, t: Throwable) {
                isLoading = false
                loadingLayout.setLoadingVisibility(false)
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    /**
     * Procesa la respuesta exitosa de la solicitud de los folios.
     *
     * @param apiResponse Respuesta de la API con los folios.
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun handleSuccessResponse(apiResponse: Folios?) {
        apiResponse?.items?.let { items ->
            if (items.isNotEmpty()) {
                val previousSize = itemAdapter.items.size
                (itemAdapter.items).addAll(items)  // Agrega nuevos ítems a la lista
                itemAdapter.notifyItemRangeInserted(previousSize, items.size)

                // Marca la última página si no hay suficientes ítems
                if (items.size < limit) isLastPage = true
            } else {
                isLastPage = true
            }
        } ?: run {
            // Si la respuesta es nula, marca la última página
            isLastPage = true
            showToast("No hay más datos disponibles")
        }
    }
}
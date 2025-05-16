package com.enlacedigital.CoordiApp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.FrameLayout
import com.enlacedigital.CoordiApp.Adapter.NoCompletasAdapter
import com.enlacedigital.CoordiApp.models.Folios
import com.enlacedigital.CoordiApp.models.FoliosDetalle
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.showToast
import com.enlacedigital.CoordiApp.utils.startNewActivity

/**
 * Actividad que muestra una lista paginada de elementos incompletos asignados a un técnico.
 *
 * Esta clase utiliza un RecyclerView para mostrar los datos y realiza llamadas a la API para obtener
 * los datos en función del técnico y la paginación.
 */
class NoCompletas : AppCompatActivity() {

    // Preferencias y servicio de API
    private val preferencesManager = PreferencesHelper.getPreferencesManager()
    private val apiService = ApiServiceHelper.getApiService()

    // Componentes de UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: NoCompletasAdapter
    private lateinit var loadingLayout: FrameLayout

    // Variables de paginación
    private var isLoading = false
    private var page = 1
    private val limit = 10
    private var isLastPage = false

    /**
     * Método llamado al crear la actividad.
     * Configura la interfaz de usuario, inicializa el RecyclerView y realiza la primera llamada a la API.
     *
     * @param savedInstanceState El estado de la actividad si ha sido recreada.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incompleted)

        // Inicializa la vista de carga
        loadingLayout = findViewById(R.id.loading_layout)

        // Configura el RecyclerView con su LayoutManager y Adaptador
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@NoCompletas)
            itemAdapter = NoCompletasAdapter(mutableListOf())
            adapter = itemAdapter
            addOnScrollListener(createOnScrollListener()) // Listener para paginación
        }

        // Verifica la sesión del usuario
        checkSession(apiService, this, null as Class<Nothing>?)

        // Obtiene el ID del técnico desde las preferencias y carga los datos iniciales
        val idTecnico = preferencesManager.getString("id_tecnico")!!.toInt()
        fetchData(idTecnico, page)
    }

    /**
     * Crea un [RecyclerView.OnScrollListener] para manejar la paginación.
     *
     * Este listener detecta cuando el usuario alcanza el final de la lista y solicita más datos.
     *
     * @return Un objeto [RecyclerView.OnScrollListener].
     */
    private fun createOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                layoutManager?.let {
                    val visibleItemCount = it.childCount
                    val totalItemCount = it.itemCount
                    val firstVisibleItemPosition = it.findFirstVisibleItemPosition()

                    // Verifica si se debe cargar más datos
                    if (!isLoading && !isLastPage &&
                        (visibleItemCount + firstVisibleItemPosition >= totalItemCount) &&
                        firstVisibleItemPosition >= 0
                    ) {
                        page++
                        fetchData(preferencesManager.getString("id_tecnico")!!.toInt(), page)
                    }
                }
            }
        }
    }

    /**
     * Realiza una llamada a la API para obtener los registros incompletos de un técnico.
     *
     * @param tecnicoId ID del técnico para la consulta.
     * @param page Número de página a solicitar.
     */
    private fun fetchData(tecnicoId: Int, page: Int) {
        isLoading = true
        loadingLayout.setLoadingVisibility(true) // Muestra el layout de carga

        apiService.getNoCompletados(tecnicoId, page, limit).enqueue(object : Callback<Folios> {
            override fun onResponse(call: Call<Folios>, response: Response<Folios>) {
                loadingLayout.setLoadingVisibility(false)
                isLoading = false

                if (response.isSuccessful) {
                    response.body()?.items?.let { items ->
                        if (items.isNotEmpty()) {
                            updateAdapterData(items)
                        } else {
                            isLastPage = true
                            showToast("No hay datos disponibles")
                            startNewActivity(Menu::class.java)
                        }
                    } ?: run {
                        isLastPage = true
                        showToast("No hay más datos disponibles")
                    }
                } else {
                    showToast("Error en la respuesta")
                    finish()
                }
            }

            override fun onFailure(call: Call<Folios>, t: Throwable) {
                loadingLayout.setLoadingVisibility(false)
                isLoading = false
                showToast("Error en la solicitud: ${t.message}")
                finish()
            }
        })
    }

    /**
     * Actualiza el adaptador del RecyclerView con nuevos elementos.
     *
     * @param newItems Lista de nuevos elementos a agregar.
     */
    private fun updateAdapterData(newItems: List<FoliosDetalle>) {
        val previousSize = itemAdapter.items.size
        itemAdapter.items.addAll(newItems)
        itemAdapter.notifyItemRangeInserted(previousSize, newItems.size)

        if (newItems.size < limit) {
            isLastPage = true
        }
    }
}
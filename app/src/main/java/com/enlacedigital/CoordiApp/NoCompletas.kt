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

class NoCompletas : AppCompatActivity() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: NoCompletasAdapter
    private lateinit var loadingLayout: FrameLayout

    private var isLoading = false
    private var page = 1
    private val limit = 10
    private var isLastPage = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incompleted)
        loadingLayout = findViewById(R.id.loading_layout)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@NoCompletas)
            itemAdapter = NoCompletasAdapter(mutableListOf())
            adapter = itemAdapter
            addOnScrollListener(createOnScrollListener())
        }

        checkSession(apiService, this, null as Class<Nothing>?)
        val idTecnico = preferencesManager.getString("id_tecnico")!!.toInt()
        fetchData(idTecnico, page)
    }

    private fun createOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                layoutManager?.let {
                    val visibleItemCount = it.childCount
                    val totalItemCount = it.itemCount
                    val firstVisibleItemPosition = it.findFirstVisibleItemPosition()

                    if (!isLoading && !isLastPage &&
                        (visibleItemCount + firstVisibleItemPosition >= totalItemCount) &&
                        firstVisibleItemPosition >= 0) {
                        page++
                        fetchData(preferencesManager.getString("id_tecnico")!!.toInt(), page)
                    }
                }
            }
        }
    }

    private fun fetchData(tecnicoId: Int, page: Int) {
        isLoading = true
        loadingLayout.setLoadingVisibility(true)

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
                            showToast("No hay más datos disponibles")
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

    private fun updateAdapterData(newItems: List<FoliosDetalle>) {
        val previousSize = itemAdapter.items.size
        itemAdapter.items.addAll(newItems)
        itemAdapter.notifyItemRangeInserted(previousSize, newItems.size)

        if (newItems.size < limit) {
            isLastPage = true
        }
    }
}
package com.enlacedigital.CoordiApp

import com.enlacedigital.CoordiApp.Adapter.CompletedAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.FrameLayout
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


class Completed : AppCompatActivity() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemAdapter: CompletedAdapter
    private lateinit var loadingLayout: FrameLayout

    private var isLoading = false
    private var page = 1
    private val limit = 10
    private var isLastPage = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed)

        initViews()
        checkSession(apiService, this@Completed, null as Class<Nothing>?)

        val idTecnicoString = preferencesManager.getString("id_tecnico")
        if (idTecnicoString.isNullOrEmpty()) {
            showToast("Por favor, inicia sesión nuevamente.")
            startNewActivity(Login::class.java)
            return
        }
        val idTecnico = idTecnicoString.toInt()
        completed(idTecnico, page)
    }

    private fun initViews() {
        loadingLayout = findViewById(R.id.loading_layout)
        recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@Completed)
            itemAdapter = CompletedAdapter(mutableListOf())
            adapter = itemAdapter
            addOnScrollListener(createOnScrollListener())
        }

        findViewById<Button>(R.id.nuevoRegistro).setOnClickListener {
            startActivity(Intent(this, Registrando::class.java))
        }
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
                        completed(preferencesManager.getString("id_tecnico")!!.toInt(), page)
                    }
                }
            }
        }
    }

    private fun completed(idTecnico: Int, page: Int) {
        isLoading = true
        loadingLayout.setLoadingVisibility(true)
        apiService.getCompletados(idTecnico, page, limit).enqueue(object : Callback<Folios> {
            override fun onResponse(call: Call<Folios>, response: Response<Folios>) {
                isLoading = false
                loadingLayout.setLoadingVisibility(false)
                if (response.isSuccessful) {
                    handleSuccessResponse(response.body())
                } else {
                    showToast("Error en la respuesta del servidor")
                }
            }

            override fun onFailure(call: Call<Folios>, t: Throwable) {
                isLoading = false
                loadingLayout.setLoadingVisibility(false)
                showToast("Error en la solicitud: ${t.message}")
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleSuccessResponse(apiResponse: Folios?) {
        apiResponse?.items?.let { items ->
            if (items.isNotEmpty()) {
                val previousSize = itemAdapter.items.size
                (itemAdapter.items).addAll(items)
                itemAdapter.notifyItemRangeInserted(previousSize, items.size)
                if (items.size < limit) isLastPage = true
            } else {
                isLastPage = true
            }
        } ?: run {
            isLastPage = true
            showToast("No hay más datos disponibles")
        }
    }
}


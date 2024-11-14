package com.enlacedigital.CoordiApp.singleton

import android.app.Application
import android.content.Context
import com.enlacedigital.CoordiApp.ApiService
import com.enlacedigital.CoordiApp.PreferencesManager
import com.enlacedigital.CoordiApp.createRetrofitService

class InitSingleton : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiServiceHelper.init(this)
        PreferencesHelper.init(this)
    }
}

object PreferencesHelper {

    private lateinit var preferencesManager: PreferencesManager

    fun init(context: Context) {
        preferencesManager = PreferencesManager(context.applicationContext)
    }

    fun getPreferencesManager(): PreferencesManager {
        if (!PreferencesHelper::preferencesManager.isInitialized) {
            throw IllegalStateException("PreferencesHelper no ha sido inicializado. Llama a init() primero.")
        }
        return preferencesManager
    }
}

object ApiServiceHelper {

    private lateinit var apiService: ApiService

    // Método para inicializar Retrofit y el ApiService
    fun init(context: Context) {
        apiService = createRetrofitService(context)
    }

    // Método para obtener la instancia del ApiService
    fun getApiService(): ApiService {
        if (!::apiService.isInitialized) {
            throw IllegalStateException("ApiServiceHelper no ha sido inicializado. Llama a init() primero.")
        }
        return apiService
    }
}

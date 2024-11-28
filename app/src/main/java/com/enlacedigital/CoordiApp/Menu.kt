package com.enlacedigital.CoordiApp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.enlacedigital.CoordiApp.models.LogoutResponse
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.showToast
import com.enlacedigital.CoordiApp.utils.startNewActivity
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Menu : AppCompatActivity() {

    val preferencesManager = PreferencesHelper.getPreferencesManager()
    val apiService = ApiServiceHelper.getApiService()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        setupListeners()
        val tecnico: TextView = findViewById(R.id.tecnico)
        tecnico.text = preferencesManager.getString("tecnico")
        val version: TextView = findViewById(R.id.version)
        version.text = "v" + packageManager.getPackageInfo(packageName, 0).versionName
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.registered).setOnClickListener {
            startActivity(Intent(this, Completed::class.java))
        }

        findViewById<ImageButton>(R.id.incompleted).setOnClickListener {
            startActivity(Intent(this, NoCompletas::class.java))
        }

        findViewById<ImageButton>(R.id.logout).setOnClickListener {
            performLogout()
        }

        findViewById<ImageButton>(R.id.compare).setOnClickListener {
            startActivity(Intent(this, Comparativa::class.java))
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            val apiService = createRetrofitService(this@Menu)
            val call = apiService.logout()

            call.enqueue(object : Callback<LogoutResponse> {
                override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) {
                    if (response.isSuccessful) {
                        preferencesManager.clearSession()
                        startNewActivity(Login::class.java)
                    } else {
                        showToast("Error en la respuesta del servidor")
                    }
                }

                override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                    showToast("Error en la conexión")
                }
            })
        }
    }
}
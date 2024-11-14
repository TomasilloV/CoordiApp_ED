package com.enlacedigital.CoordiApp

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.enlacedigital.CoordiApp.models.LoginResponse
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.checkLocationPermission
import com.enlacedigital.CoordiApp.utils.setLoadingVisibility
import com.enlacedigital.CoordiApp.utils.showToast
import com.enlacedigital.CoordiApp.utils.hideKeyboardOnOutsideTouch
import com.enlacedigital.CoordiApp.utils.startNewActivity
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class Login : AppCompatActivity() {

    val preferencesManager = PreferencesHelper.getPreferencesManager()
    private val requestPERMISSION = 1
    private lateinit var loadingLayout: FrameLayout

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        checkLocationPermission(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ), requestPERMISSION
        )
        val versionText: TextView = findViewById(R.id.version)
        versionText.text = "v" + packageManager.getPackageInfo(packageName, 0).versionName
        val loginButton = findViewById<Button>(R.id.loginButton)
        val userEditText = findViewById<EditText>(R.id.user)
        loadingLayout = findViewById(R.id.loading_layout)

        loginButton.setOnClickListener {
            val username = userEditText.text.toString()
            if (username.isEmpty()) {
                showToast("Por favor ingrese un nombre de usuario")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                loadingLayout.setLoadingVisibility(true)
                val apiService = createRetrofitService(this@Login)
                val call = apiService.login(username)

                call.enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        handleLoginResponse(response)
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        loadingLayout.setLoadingVisibility(false)
                        showToast("Error. Asegúrate de tener una conexión a internet estable")
                    }
                })
            }
        }
    }

    private fun handleLoginResponse(response: Response<LoginResponse>) {
        loadingLayout.setLoadingVisibility(false)
        if (response.isSuccessful) {
            val loginResponse = response.body()
            if (loginResponse?.mensaje == "Datos Correctos") {
                loginResponse.info.let { item ->
                    preferencesManager.saveString("id_tecnico", item.idTecnico.toString())
                    val tecnico = item.Nombre_T + " " + item.Apellidos_T
                    preferencesManager.saveString("tecnico", tecnico)
                    startNewActivity(Menu::class.java)
                }
            } else {
                showToast("Usuario o datos incorrectos")
            }
        } else {
            showToast("Error en la respuesta del servidor")
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        hideKeyboardOnOutsideTouch(ev)
        return super.dispatchTouchEvent(ev)
    }
}

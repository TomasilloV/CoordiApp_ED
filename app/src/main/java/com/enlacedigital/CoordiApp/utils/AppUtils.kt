package com.enlacedigital.CoordiApp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.ApiService
import com.enlacedigital.CoordiApp.Login
import com.enlacedigital.CoordiApp.PreferencesManager
import com.enlacedigital.CoordiApp.models.SessionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun getPreferencesManager(context: Context): PreferencesManager {
    return PreferencesManager(context.applicationContext)
}

    // Función para cambiar la visibilidad de una vista
    fun FrameLayout.setLoadingVisibility(visible: Boolean) {
        this.visibility = if (visible) View.VISIBLE else View.GONE
    }

    // Función para mostrar un mensaje toast
    fun Context.showToast(message: String) {
        Toast.makeText(this.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    // Extensión para ocultar el teclado
    fun Context.hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Función para ocultar el teclado al tocar fuera de un EditText
    fun Activity.hideKeyboardOnOutsideTouch(event: MotionEvent) {
        val view = currentFocus
        if (view is View) {
            val outRect = android.graphics.Rect()
            view.getGlobalVisibleRect(outRect)
            if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                view.clearFocus()
                hideKeyboard(view)
            }
        }
    }

    // Función para verificar permisos y solicitarlos si es necesario
    fun Activity.checkPermission(
        permissions: List<String>,
        requestCode: Int
    ) {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                requestCode
            )
        }
    }

    // Función para iniciar una nueva actividad y finalizar la actual
    fun <T> Context.startNewActivity(target: Class<T>) {
        val intent = Intent(this, target).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        if (this is Activity) {
            (this).finish()
        }
    }

    // Función para iniciar una nueva actividad y finalizar la actual
    fun <T> Fragment.startNewActivity(target: Class<T>) {
        val intent = Intent(requireContext(), target).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    //revisar sesión válida
    fun <T> checkSession(apiService: ApiService, context: Context, successActivity: Class<T>?) {
        val call = apiService.sessionCheck()
        call.enqueue(object : Callback<SessionResponse> {
            override fun onResponse(
                call: Call<SessionResponse>,
                response: Response<SessionResponse>
            ) {
                if (response.isSuccessful) {
                    val respuesta = response.body()

                    if (respuesta != null && respuesta.mensaje == "Sesión válida") {
                        if (successActivity != null) {
                            context.startNewActivity(successActivity)
                        }
                    } else {
                        context.startNewActivity(Login::class.java)
                        context.showToast("Inicia sesión para continuar")
                    }
                } else {
                    context.startNewActivity(Login::class.java)
                }
            }

            override fun onFailure(call: Call<SessionResponse>, t: Throwable) {
                context.startNewActivity(Login::class.java)
                context.showToast("Error, intenta de nuevo en unos momentos")
            }
        })
    }

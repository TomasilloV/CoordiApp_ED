package com.enlacedigital.CoordiApp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveString(name: String, key: String) {
        sharedPreferences.edit().putString(name, key).apply()
    }

    fun getString(name: String): String? {
        return sharedPreferences.getString(name, null)
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}
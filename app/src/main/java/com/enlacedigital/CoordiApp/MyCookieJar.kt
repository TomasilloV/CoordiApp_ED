package com.enlacedigital.CoordiApp

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import android.util.Log
import com.enlacedigital.CoordiApp.models.SerializableCookie
import java.io.*
import java.util.*

class MyCookieJar(context: Context) : CookieJar {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("CookiePrefs", Context.MODE_PRIVATE)
    private val cookieStore = mutableMapOf<String, MutableList<SerializableCookie>>()

    init {
        loadCookiesFromPreferences()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val key = "https://erp.ed-intra.com/API/"
        Log.d("MyCookieJar", "Guardando cookies para $key: $cookies")

        val serializableCookies = cookies.map { cookie ->
            SerializableCookie(
                name = cookie.name,
                value = cookie.value,
                expiresAt = cookie.expiresAt,
                domain = cookie.domain,
                path = cookie.path,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly
            )
        }

        cookieStore[key]?.addAll(serializableCookies) ?: cookieStore.put(key, serializableCookies.toMutableList())
        saveCookiesToPreferences()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val key = "https://erp.ed-intra.com/API/"
        val serializableCookies = cookieStore[key]?.toList() ?: emptyList()

        Log.d("MyCookieJar", "Cargando cookies para $key: $serializableCookies")

        return serializableCookies.map {
            val cookieBuilder = Cookie.Builder()
                .name(it.name)
                .value(it.value)
                .expiresAt(it.expiresAt)
                .domain(it.domain)
                .path(it.path)

            if (it.secure) {
                cookieBuilder.secure()
            }

            if (it.httpOnly) {
                cookieBuilder.httpOnly()
            }

            cookieBuilder.build()
        }
    }

    // Guardar cookies en SharedPreferences
    private fun saveCookiesToPreferences() {
        val editor = sharedPreferences.edit()
        for ((key, cookies) in cookieStore) {
            val serializedCookies = cookies.map { serializeCookie(it) }
            editor.putStringSet(key, serializedCookies.toSet())
        }
        editor.apply()
    }

    // Cargar cookies desde SharedPreferences
    private fun loadCookiesFromPreferences() {
        val allEntries = sharedPreferences.all
        for ((key, value) in allEntries) {
            if (value is Set<*>) {
                val cookies = value.mapNotNull { deserializeCookie(it as String) }
                cookieStore[key] = cookies.toMutableList()
            }
        }
    }

    // Serializar una cookie a String para almacenarla
    private fun serializeCookie(cookie: SerializableCookie): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(cookie)
        objectOutputStream.close()
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

    // Deserializar una cookie desde un String almacenado
    private fun deserializeCookie(cookieString: String): SerializableCookie? {
        return try {
            val bytes = Base64.getDecoder().decode(cookieString)
            val byteArrayInputStream = ByteArrayInputStream(bytes)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)
            objectInputStream.readObject() as SerializableCookie
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

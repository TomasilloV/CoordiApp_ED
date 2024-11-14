package com.enlacedigital.CoordiApp

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.net.ssl.SSLSession
import java.util.concurrent.TimeUnit
import java.security.cert.CertificateException

fun createRetrofitService(context: Context): ApiService {
    val client = createCustomOkHttpClient(context)

    val retrofit = Retrofit.Builder()
        .baseUrl("https://vps.ed-intra.com/API/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(ApiService::class.java)
}

// Configuración estándar de OkHttpClient
private fun createCustomOkHttpClient(context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .cookieJar(MyCookieJar(context))
        .addInterceptor(loggingInterceptor())
        .build()
}

// Configuración para registrar las peticiones y respuestas HTTP
private fun loggingInterceptor(): Interceptor {
    return HttpLoggingInterceptor { message -> Log.d("HTTP", message) }
        .apply { level = HttpLoggingInterceptor.Level.BODY }
}
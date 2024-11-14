package com.enlacedigital.CoordiApp

import com.enlacedigital.CoordiApp.Registrar.Checking
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.models.ApiResponse
import com.enlacedigital.CoordiApp.models.ComparativaRequest
import com.enlacedigital.CoordiApp.models.ComparativaResponse
import com.enlacedigital.CoordiApp.models.Folios
import com.enlacedigital.CoordiApp.models.LoginResponse
import com.enlacedigital.CoordiApp.models.LogoutResponse
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.models.SessionResponse
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    //Para las gráficas de telmex y Ed
    @POST("comparativa")
    fun getComparativa(@Body request: ComparativaRequest): Call<List<ComparativaResponse>>

    //Comprobar sesión válida
    @GET("validar-sesion")
    fun sessionCheck(): Call<SessionResponse>

    //Era para verificar la versión por si se instala directo con APK
    @GET("appVersion.json")
    fun checkVersion(): Call<JsonObject?>?

    @FormUrlEncoded
    @POST("iniciar-sesion")
    fun login(@Field("Usuario_App") usuarioApp: String): Call<LoginResponse>

    @GET("cerrar-sesion")
    fun logout(): Call<LogoutResponse>

    @FormUrlEncoded
    @POST("ver-completos")
    fun getCompletados(
        @Field("idTecnico") idTecnico: Int,
        @Field("page") page: Int,
        @Field("limit") limit: Int
    ): Call<Folios>

    @FormUrlEncoded
    @POST("ver-no-completos")
    fun getNoCompletados(
        @Field("idTecnico") idTecnico: Int,
        @Field("page") page: Int,
        @Field("limit") limit: Int
    ): Call<Folios>

    //Obtener opciones para los spinner
    @GET("obtener-opciones")
    fun options(
        @Query("step") step: String,
        @Query("idEstado") idEstado: Int? = null,
        @Query("idMunicipio") idMunicipio: Int? = null,
        @Query("idTecnico") idTecnico: Int? = null
    ): Call<List<Option>>

    //Verificar folio existente o crearlo
    @FormUrlEncoded
    @POST("buscar-crear-folio")
    fun checkAndInsert(
        @Field("Folio_Pisa") folioPisa: String,
        @Field("Telefono") telefono: String,
        @Field("Latitud") latitud: String,
        @Field("Longitud") longitud: String,
        @Field("idTecnico") idTecnico: String?,
        @Field("fecha") fecha: String?,
        ): Call<Checking>

    @POST("actualizar")
    fun updateTechnicianData(
        @Body requestData: ActualizarBD
    ): Call<ApiResponse>
}
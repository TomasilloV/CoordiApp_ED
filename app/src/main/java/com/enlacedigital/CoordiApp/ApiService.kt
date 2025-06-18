package com.enlacedigital.CoordiApp

import com.enlacedigital.CoordiApp.Registrar.Checking
import com.enlacedigital.CoordiApp.models.ActualizarBD
import com.enlacedigital.CoordiApp.models.ApiResponse
import com.enlacedigital.CoordiApp.models.ComparativaRequest
import com.enlacedigital.CoordiApp.models.ComparativaResponse
import com.enlacedigital.CoordiApp.models.FolioRequest
import com.enlacedigital.CoordiApp.models.Folios
import com.enlacedigital.CoordiApp.models.LoginResponse
import com.enlacedigital.CoordiApp.models.LogoutResponse
import com.enlacedigital.CoordiApp.models.ONTCOBRE
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.models.SessionResponse
import com.enlacedigital.CoordiApp.models.TacResponse
import com.enlacedigital.CoordiApp.models.materiales
import com.enlacedigital.CoordiApp.models.pasos
import com.enlacedigital.CoordiApp.models.requestpasos
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interfaz que define los métodos de comunicación con el backend
 * a través de Retrofit. Estos métodos están destinados a realizar
 * diversas operaciones relacionadas con la gestión de sesiones,
 * comparativas, autenticación y actualización de datos del técnico.
 */
interface ApiService {

    /**
     * Obtiene la comparación de datos entre Telmex y Ed.
     *
     * @param request Objeto que contiene los parámetros necesarios para la comparación.
     * @return Una llamada Retrofit con una lista de respuestas de comparación.
     */
    @POST("api/comparativa")
    fun getComparativa(@Body request: ComparativaRequest): Call<List<ComparativaResponse>>

    /**
     * Verifica si la sesión del usuario es válida.
     *
     * @return Una llamada Retrofit que devuelve la respuesta de validación de sesión.
     */
    @GET("login-coordiapp/verificar-sesion")
    fun sessionCheck(): Call<SessionResponse>

    /**
     * Verifica la versión de la aplicación instalada, útil cuando se instala a través de un APK.
     *
     * @return Una llamada Retrofit que devuelve un objeto JSON con la versión de la app.
     */
    @GET("appVersion")
    fun checkVersion(): Call<JsonObject?>?

    /**
     * Inicia sesión en la aplicación utilizando el nombre de usuario proporcionado.
     *
     * @param usuarioApp Nombre del usuario que inicia sesión.
     * @return Una llamada Retrofit que devuelve la respuesta con los detalles del login.
     */
    @GET("login-coordiapp/iniciar-sesion/{username}")
    @Headers("Accept: application/json", "Content-Type: application/json")
    fun login(
        @Path("username") usuarioApp: String,
    ): Call<LoginResponse>

    /**
     * Cierra la sesión actual del usuario.
     *
     * @return Una llamada Retrofit que devuelve la respuesta de cierre de sesión.
     */
    @GET("login-coordiapp/cerrar-sesion")
    fun logout(): Call<LogoutResponse>

    /**
     * Obtiene los folios completados por el técnico especificado.
     *
     * @param idTecnico Identificador del técnico.
     * @param page Número de página para la paginación.
     * @param limit Número de elementos por página.
     * @return Una llamada Retrofit que devuelve los folios completados.
     */
    @GET("coordiapp/completadas-tecnico/{idTecnico}")
    fun getCompletados(
        @Path("idTecnico") idTecnico: Int
    ): Call<Folios>

    /**
     * Obtiene los folios no completados por el técnico especificado.
     *
     * @param idTecnico Identificador del técnico.
     * @param page Número de página para la paginación.
     * @param limit Número de elementos por página.
     * @return Una llamada Retrofit que devuelve los folios no completados.
     */
    @GET("coordiapp/incompletas-tecnico/{idTecnico}")
    fun getNoCompletados(
        @Path("idTecnico") idTecnico: Int
    ): Call<Folios>

    /**
     * Obtiene los folios no completados por el técnico especificado.
     *
     * @param FK_Tecnico_Salida_Det Identificador del técnico.
     * @param page Número de página para la paginación.
     * @param limit Número de elementos por página.
     * @return Una llamada Retrofit que devuelve los folios no completados.
     */
    @GET("materiales/{FK_Tecnico_Salida_Det}")
    fun vermateriales(
        @Path("FK_Tecnico_Salida_Det") FK_Tecnico_Salida_Det: Int
    ): Call<materiales>

    /**
     * Obtiene las opciones para los spinner en la aplicación según el paso y los filtros dados.
     *
     * @param step Paso del flujo en el cual se requiere las opciones.
     * @param idEstado Opcional, id del estado para filtrar las opciones.
     * @param idMunicipio Opcional, id del municipio para filtrar las opciones.
     * @param idTecnico Opcional, id del técnico para filtrar las opciones.
     * @return Una llamada Retrofit que devuelve una lista de opciones.
     */
    @GET("coordiapp/opciones")
    fun options(
        @Query("step") step: String,
        @Query("idEstado") idEstado: Int? = null,
        @Query("idMunicipio") idMunicipio: Int? = null,
        @Query("idTecnico") idTecnico: Int? = null
    ): Call<List<Option>>

    /**
     * Verifica si un folio existe, o en caso contrario, crea un nuevo folio.
     *
     * @param folioPisa Folio de Pisa.
     * @param telefono Número de teléfono asociado al folio.
     * @param latitud Latitud de la ubicación.
     * @param longitud Longitud de la ubicación.
     * @param idTecnico Opcional, id del técnico que gestiona el folio.
     * @param fecha Opcional, fecha asociada al folio.
     * @return Una llamada Retrofit que devuelve una respuesta de verificación o creación del folio.
     */
    /*@FormUrlEncoded
    @POST("coordiapp/get-orden")
    fun checkAndInsert(
        @Field("Folio_Pisa") folioPisa: String,
        @Field("Telefono") telefono: String,
        @Field("Latitud") latitud: String,
        @Field("Longitud") longitud: String,
        @Field("idTecnico") idTecnico: String?,
        @Field("fecha") fecha: String?,
    ): Call<Checking>*/

    @GET("coordiapp/get-orden/{Folio_Pisa}/{Telefono}/{Latitud}/{Longitud}/{FK_Tecnico_apps}/{Fecha_Coordiapp}")
    fun checkAndInsert(
        @Path("Folio_Pisa") folio: String,
        @Path("Telefono") telefono: String?,
        @Path("Latitud") latitud: String?,
        @Path("Longitud") longitud: String?,
        @Path("FK_Tecnico_apps") idTecnico: String?,
        @Path("Fecha_Coordiapp") fecha: String?,
    ): Call<Checking>

    /*@POST("coordiapp/get-orden")
    fun checkAndInsert(
        @Body request: OrdenRequest
    ): Call<Checking>*/

    /**
     * Actualiza los datos del técnico en la base de datos.
     *
     * @param requestData Objeto que contiene los datos que se deben actualizar.
     * @return Una llamada Retrofit que devuelve la respuesta de la actualización.
     */
    @POST("api/actualizar")
    fun updateTechnicianData(
        @Body requestData: ActualizarBD
    ): Call<ApiResponse>

    @POST("api/ontCobre")
    fun ontCobre(
        @Body requestData: ONTCOBRE
    ): Call <ApiResponse>

    @POST("api/bolsa-tac")
    fun obtenertac(
        @Body request: FolioRequest
    ): Call<TacResponse>

    @POST("api/registro-pasos")
    fun registropasos(
        @Body requestData: requestpasos
    ): Call <ApiResponse>

    @GET("registro-pasos/{Folio_Pisa}")
    fun pasosRegistros(
        @Path("Folio_Pisa") Folio_Pisa: Int?
    ): Call<pasos>
}

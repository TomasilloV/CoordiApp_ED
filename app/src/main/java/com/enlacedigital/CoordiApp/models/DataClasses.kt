package com.enlacedigital.CoordiApp.models

import java.io.Serializable

// ====================================================
// ==== Para poder obtener las cookies del session ====
// ====================================================

data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean
) : Serializable

// ====================================================
// ====== Para comparar registros de telmex y ED ======
// ====================================================

data class ComparativaRequest(
    val anio: Int,
    val mes: Int,
    val idTecnico: Int,
    val opcion: Int
)

data class ComparativaResponse(
    val Anio: Int? = null,
    val Mes: Int? = null,
    val Registros_Telmex: Int? = null,
    val Registros_ED: Int? = null,
    val Folios_Telmex: String? = null,
    val TELEFONOS_TELMEX: String? = null,
)


// ====================================================
// ==== Para los registros completos o incompletos ====
// ====================================================

data class Folios(
    val mensaje: String,
    val items: List<FoliosDetalle>?
)

data class FoliosDetalle(
    val Folio_Pisa: String?,
    val FK_Cope: String?,
    val Tecnologia: String?,
    val Telefono: String?,
    val Contratista: String?,
    val Tipo_Tarea: String?,
    val Estatus_Orden: String?,
    val Step_Registro: String?,
    val Tecnico: String?,
    val COPE: String?,
    val Fecha_Coordiapp: String?
)

// ====================================================
// ================ Respuesta de Login ================
// ====================================================

data class SessionResponse(
    val mensaje: String,
    val usuario: LoginDetalle?
)

data class LoginResponse(
    val mensaje: String,
    val info: LoginDetalle
)

data class LoginDetalle(
    val idTecnico: Int?,
    val Nombre_T: String?,
    val Apellidos_T: String?,
    val Celular_T: String?,
    val Usuario_App: String?,
    val Fecha_Ingreso_T: String?,
    val FK_Contratista_Tecnico: Int?,
    val NExpediente: Int?,
    val Cope_T: String?,
    val FK_Cope_Tecnico: Int?
)

data class LogoutResponse(
    val mensaje: String
)

// ====================================================
// ========= Crear nuevo folio y/o actualizar =========
// ====================================================

data class ActualizarBD(
    val idtecnico_instalaciones_coordiapp: String,
    val FK_Cope: Int? = null,
    val Foto_Ont: String? = null,
    val Fecha_Coordiapp: String? = null,
    val Foto_Casa_Cliente: String? = null,
    val Foto_INE: String? = null,
    val FK_Tecnico_apps: Int? = null,
    val No_Serie_ONT: String? = null,
    val Distrito: String? = null,
    val Puerto: String? = null,
    val Terminal: String? = null,
    val Tipo_Tarea: String? = null,
    val Estatus_Orden: String? = null,
    val Foto_Puerto: String? = null,
    val Metraje: Int? = null,
    val fk_distrito: Int? = null,
    val Tecnologia: String? = null,
    val Tipo_Instalacion: String? = null,
    val FK_Auditor: Int? = null,
    val Fecha_Asignacion_Auditor: String? = null,
    val Cliente_Titular: String? = null,
    val Cliente_Recibe: String? = null,
    val Step_Registro: Int? = null,
    val Direccion_Cliente: String? = null,
    val Telefono_Cliente: String? = null,
    val Apellido_Paterno_Titular: String? = null,
    val Apellido_Materno_Titular: String? = null,
    val Alfa_Ont: String? = null,
    val Ont: String? = null,
    val idOnt: Int? = null,
    val Latitud_Terminal: String? = null,
    val Longitud_Terminal: String? = null
)

data class ApiResponse(
    val mensaje: String
)

// ====================================================
// ===== Obtener opciones de spinners en registro =====
// ====================================================

data class Option(
    val idDivision: Int? = null,
    val nameDivision: String? = null,
    val idAreas: Int? = null,
    val nameArea: String? = null,
    val idCope: Int? = null,
    val COPE: String? = null,
    val nameColonia: String? = null,
    val CodigoPostal: Int? = null,
    val estadoMunicipio: Int? = null,
    val nameMunicipio: String? = null,
    val idMunicipio: Int? = null,
    val idEstado: Int? = null,
    val nameEstado: String? = null,
    val idSalidas: Int? = null,
    val Num_Serie_Salida_Det: String? = null,
    val Producto: String? = null,
    val Modelo: String? = null,
)

data class materiales(
    val mensaje: String,
    val items: List<materialesdetalle>?
)

data class materialesdetalle(
    val Producto: String? = null,
    val Modelo: String? = null,
    val Num_Serie_Salida_Det: String? = null,
)


data class DistritosDetalle(
    val id_distrito: Int? = null,
    val distrito: String? = null,
    val tipo_instalacion: String? = null,
    val fk_cope: Int? = null
)

data class textAnnotationsDetalle(
    val description: String? = null
)
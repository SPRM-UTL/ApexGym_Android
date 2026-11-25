package com.sprm.apexgym.core.network.api

import com.sprm.apexgym.core.network.client.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class NotificacionDto(
    val id: Int = 0,
    val empresaId: Int = 1,
    val titulo: String = "",
    val mensaje: String = "",
    val tipo: String = "GENERAL",
    val destinatarioId: Int = 0,
    val destinatarioTipo: String = "CLIENTE",
    val remitenteId: Int? = null,
    val sucursalId: Int? = null,
    val estado: String = ESTADO_PENDIENTE,
    val leidaAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val estaPendiente: Boolean get() = estado == ESTADO_PENDIENTE
    val estaLeida: Boolean get() = estado == ESTADO_LEIDA
    val estaIgnorada: Boolean get() = estado == ESTADO_IGNORADA

    companion object {
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_LEIDA = "LEIDA"
        const val ESTADO_IGNORADA = "IGNORADA"
        const val TIPO_ACTIVIDAD_EMPLEADO = "ACTIVIDAD_EMPLEADO"
    }
}

data class CrearNotificacionRequest(
    val titulo: String,
    val mensaje: String,
    val destinatarioId: Int,
    val destinatarioTipo: String,
    val tipo: String = "GENERAL",
    val sucursalId: Int? = null
)

data class NotificacionCountDto(
    val count: Int = 0
)

/** Contrato OpenAPI del módulo de notificaciones (PLAN-007). */
interface NotificacionApiService {

    @POST("api/notificaciones")
    suspend fun crearNotificacion(
        @Body request: CrearNotificacionRequest
    ): Response<ApiResponse<NotificacionDto>>

    @GET("api/notificaciones")
    suspend fun listarNotificaciones(
        @Query("estado") estado: String? = null,
        @Query("tipo") tipo: String? = null
    ): Response<ApiResponse<List<NotificacionDto>>>

    @GET("api/notificaciones/no-leidas/count")
    suspend fun contarNoLeidas(): Response<ApiResponse<NotificacionCountDto>>

    @PATCH("api/notificaciones/{id}/leida")
    suspend fun marcarLeida(
        @Path("id") id: Int
    ): Response<ApiResponse<NotificacionDto>>

    @PATCH("api/notificaciones/{id}/ignorada")
    suspend fun marcarIgnorada(
        @Path("id") id: Int
    ): Response<ApiResponse<NotificacionDto>>
}

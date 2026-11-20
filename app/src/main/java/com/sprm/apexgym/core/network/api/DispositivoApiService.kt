package com.sprm.apexgym.core.network.api

import com.sprm.apexgym.core.network.client.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Request para POST /api/dispositivos/token */
data class RegistrarTokenDispositivoRequest(
    val token: String,
    val plataforma: String = PLATAFORMA_ANDROID
) {
    companion object {
        const val PLATAFORMA_ANDROID = "ANDROID"
    }
}

/** Dispositivo registrado que devuelve el backend. */
data class DispositivoRegistradoDto(
    val id: Int = 0,
    val tipo: String = "",
    val plataforma: String = "",
    val updatedAt: String? = null
)

/** Contrato OpenAPI del registro de tokens FCM (PLAN-007). */
interface DispositivoApiService {

    @POST("api/dispositivos/token")
    suspend fun registrarToken(
        @Body request: RegistrarTokenDispositivoRequest
    ): Response<ApiResponse<DispositivoRegistradoDto>>
}

package com.sprm.apexgym.core.network.api

import com.sprm.apexgym.core.db.models.EstadoCliente
import com.sprm.apexgym.core.network.client.ApiResponse
import com.sprm.apexgym.core.network.client.LoginRequest
import com.sprm.apexgym.core.network.client.LoginResponse
import com.sprm.apexgym.core.network.client.RegistroRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/usuarios/verificarCredenciales")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/clientes/registrar")
    suspend fun registrarCliente(
        @Body datos: RegistroRequest
    ): Response<Unit>

    @GET("api/estados-cliente")
    suspend fun probarcookie(): Response<ApiResponse<List<EstadoCliente>>>
}
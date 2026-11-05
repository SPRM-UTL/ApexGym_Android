package com.sprm.apexgym.core.network.api

import com.sprm.apexgym.core.db.models.EstadoCliente
import com.sprm.apexgym.core.db.models.Usuario
import com.sprm.apexgym.core.network.client.ActualizarMiPerfilRequest
import com.sprm.apexgym.core.network.client.ApiResponse
import com.sprm.apexgym.core.network.client.CorreoRequest
import com.sprm.apexgym.core.network.client.LoginRequest
import com.sprm.apexgym.core.network.client.LoginResponse
import com.sprm.apexgym.core.network.client.MiMembresiaResponseDto
import com.sprm.apexgym.core.network.client.MiPerfilResponseDto
import com.sprm.apexgym.core.network.client.ProductoClienteDto
import com.sprm.apexgym.core.network.client.RegistroRequest
import com.sprm.apexgym.core.network.client.ServicioClienteDto
import com.sprm.apexgym.core.network.client.TipoMembresiaDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {
    @POST("api/clientes/verificarCredenciales")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/clientes/verificarCorreo")
    suspend fun verificarCorreo(@Body request: CorreoRequest): Response<ApiResponse<Usuario>>

    @POST("api/clientes/registrar")
    suspend fun registrarCliente(
        @Body datos: RegistroRequest
    ): Response<Unit>

    @POST("api/usuarios/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/estados-cliente")
    suspend fun probarcookie(): Response<ApiResponse<List<EstadoCliente>>>

    @GET("api/productos/productosClientes/traer")
    suspend fun obtenerProductosCliente(): Response<ApiResponse<List<ProductoClienteDto>>>

    @GET("api/servicios/serviciosClientes/traer")
    suspend fun obtenerServiciosCliente(): Response<ApiResponse<List<ServicioClienteDto>>>

    @GET("api/tipos-membresia/membresiasCliente")
    suspend fun obtenerTiposMembresiaCliente(): Response<ApiResponse<List<TipoMembresiaDto>>>

    @GET("api/clientes-membresias/mi-membresia")
    suspend fun obtenerMiMembresia(): Response<ApiResponse<MiMembresiaResponseDto>>

    @GET("api/clientes/mi-perfil")
    suspend fun obtenerMiPerfil(): Response<ApiResponse<MiPerfilResponseDto>>

    @PUT("api/clientes/mi-perfil")
    suspend fun actualizarMiPerfil(
        @Body request: ActualizarMiPerfilRequest
    ): Response<ApiResponse<MiPerfilResponseDto>>
}

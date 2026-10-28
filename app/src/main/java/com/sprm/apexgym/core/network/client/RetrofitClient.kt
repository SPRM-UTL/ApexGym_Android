package com.sprm.apexgym.core.network.client

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class ApiResponse<T>(
    val success: Boolean,
    val status: Int,
    val data: T
)

/** Request para POST /api/clientes/verificarCredenciales */
data class LoginRequest(
    val email: String,
    val contrasenia: String
)

/** Request para POST /api/clientes/verificarCorreo */
data class VerificarCorreoRequest(
    val correo: String
)

/** Request para POST /api/clientes/registrar */
data class RegistroClienteRequest(
    // Datos de cuenta (Usuario)
    val usuario: String,
    val email: String,
    val contrasenia: String,
    val fotoUrl: String? = null,
    // Datos personales (Cliente)
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String? = null,
    val fechaNacimiento: String,
    val genero: String,
    val telefono: String,
    val direccion: String? = null,
    val contactoEmergenciaNombre: String? = null,
    val contactoEmergenciaTelefono: String? = null
)

data class LoginResponse(
    val success: Boolean,
    val status: Int,
    val data: UserData
)

data class UserData(
    val id: Int,
    val nombre: String?,
    val token: String?
)

interface AuthApiService {
    @POST("api/clientes/verificarCredenciales")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/clientes/verificarCorreo")
    suspend fun verificarCorreo(@Body request: VerificarCorreoRequest): Response<ApiResponse<Any>>

    @POST("api/clientes/registrar")
    suspend fun registrarCliente(@Body request: RegistroClienteRequest): Response<ApiResponse<Any>>
}

object RetrofitClient {
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.100.60:3000")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}

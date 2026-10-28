package com.sprm.apexgym.core.network.client

import android.content.Context
import com.sprm.apexgym.core.network.api.AuthApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class ApiResponse<T>(
    val data: T,
    val statusCode: Int,
    val message: String,
    val responseFlag: Int
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
    val usuario: String,
    val email: String,
    val contrasenia: String,
    val fotoUrl: String? = null,
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

object RetrofitClient {

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    val cookieJar: SessionCookieJar by lazy {
        SessionCookieJar(appContext)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .cookieJar(cookieJar)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.100.60:3000")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}
package com.sprm.apexgym.core.network.client

import android.content.Context
import com.sprm.apexgym.core.network.api.AuthApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class ApiResponse<T>(
    val data: T?,
    val statusCode: Int,
    val message: String?,
    val responseFlag: Int
)

/** Request para POST /api/clientes/verificarCredenciales */
data class LoginRequest(
    val email: String,
    val contrasenia: String,
    val checkbox: Boolean = false
)

/** Request para POST /api/clientes/verificarCorreo */
data class CorreoRequest(
    val correo: String
)

/** Request para POST /api/clientes/registrar */
data class RegistroRequest(
    var usuario: String,
    val email: String,
    val contrasenia: String,
    var fotoUrl: String,
    val nombre: String,
    var apellidoPaterno: String,
    var apellidoMaterno: String,
    var fechaNacimiento: String,
    var genero: String,
    val telefono: String,
    val direccion: String,
    var contactoEmergenciaNombre: String,
    var contactoEmergenciaTelefono: String
)

data class UsuarioRemoto(
    val id: Int = 0,
    val nombre: String? = null,
    val email: String? = null,
    val fotoUrl: String? = null
)

data class AuthDataResponse(
    val usuario: UsuarioRemoto? = null,
    val nuevoUsuario: UsuarioRemoto? = null
)

data class LoginResponse(
    val data: AuthDataResponse? = null,
    val statusCode: Int = 200,
    val message: String? = null,
    val responseFlag: Int = 0
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
            .baseUrl("http://192.168.100.10:3000/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}

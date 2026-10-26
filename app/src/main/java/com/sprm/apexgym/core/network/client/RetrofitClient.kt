package com.sprm.apexgym.core.network.client

import android.content.Context
import com.sprm.apexgym.BuildConfig
import com.sprm.apexgym.core.db.models.EstadoCliente
import com.sprm.apexgym.core.network.api.AuthApiService
import com.sprm.apexgym.core.network.client.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class ApiResponse<T>(
    val data: T,
    val statusCode: Int,
    val message: String,
    val responseFlag: Int
)

data class LoginRequest(
    val email: String,
    val contrasenia: String,
    val checkbox: Boolean = false
)

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

data class LoginResponse(
    val success: Boolean,
    val status: Int,
    val data: UserData
)

data class UserData(
    val id: Int,
    val nombre: String?,
    val token: String
)
object RetrofitClient {

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    val cookieJar: SessionCookieJar by lazy {
        SessionCookieJar(appContext)
    }
    private val okHttpClient : OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .cookieJar(cookieJar)
            .build()
    }

    private val retrofit : Retrofit by lazy {
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
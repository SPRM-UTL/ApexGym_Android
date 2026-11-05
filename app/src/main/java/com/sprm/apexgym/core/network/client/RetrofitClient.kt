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

data class CategoriaProductoDto(
    val id: Int = 0,
    val nombre: String? = null
)

data class ProductoClienteDto(
    val id: Int = 0,
    val categoriaProductoId: Int? = null,
    val codigoBarras: String? = null,
    val nombre: String = "",
    val descripcion: String? = null,
    val precioVenta: Double = 0.0,
    val precioCompra: Double? = null,
    val stockActual: Int = 0,
    val stockMinimo: Int = 1,
    val estado: String? = null,
    val disponibleCompra: Boolean = true,
    val imagenUrl: String? = null,
    val imagenPublicId: String? = null,
    val categoria: CategoriaProductoDto? = null
)

data class ServicioClienteDto(
    val id: Int = 0,
    val nombre: String = "",
    val descripcion: String? = null,
    val precio: Double = 0.0,
    val estado: String? = null,
    val disponibleCompra: Boolean = true,
    val imagenUrl: String? = null,
    val imagenPublicId: String? = null
)

data class TipoMembresiaDto(
    val id: Int = 0,
    val nombre: String = "",
    val descripcion: String? = null,
    val duracionDias: Int = 30,
    val precio: Double = 0.0,
    val estado: String? = null
)

data class EstadoMembresiaDto(
    val id: Int = 0,
    val nombre: String = "",
    val descripcion: String? = null,
    val color: String? = null
)

data class ClienteMembresiaDto(
    val id: Int = 0,
    val clienteId: Int = 0,
    val tipoMembresiaId: Int = 0,
    val estadoMembresiaId: Int = 0,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val renovacionAutomatica: Boolean = false,
    val tipoMembresia: TipoMembresiaDto? = null,
    val estadoMembresia: EstadoMembresiaDto? = null
)

data class ClientePerfilDto(
    val id: Int = 0,
    val nombre: String? = null,
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val correo: String? = null,
    val telefono: String? = null,
    val fechaRegistro: String? = null
)

data class MiMembresiaResponseDto(
    val cliente: ClientePerfilDto? = null,
    val membresiaActiva: ClienteMembresiaDto? = null,
    val historial: List<ClienteMembresiaDto>? = null
)

data class EstadoClienteDto(
    val id: Int = 0,
    val nombre: String? = null,
    val descripcion: String? = null,
    val color: String? = null
)

data class ClienteDetallePerfilDto(
    val id: Int = 0,
    val estadoClienteId: Int = 0,
    val nombre: String? = null,
    val apellidoPaterno: String? = null,
    val apellidoMaterno: String? = null,
    val fechaNacimiento: String? = null,
    val genero: String? = null,
    val telefono: String? = null,
    val correo: String? = null,
    val direccion: String? = null,
    val contactoEmergenciaNombre: String? = null,
    val contactoEmergenciaTelefono: String? = null,
    val fechaRegistro: String? = null,
    val estadoCliente: EstadoClienteDto? = null
)

data class MiPerfilResponseDto(
    val usuario: UsuarioRemoto? = null,
    val cliente: ClienteDetallePerfilDto? = null
)

data class ActualizarMiPerfilRequest(
    val usuario: String,
    val email: String,
    val contraseniaActual: String? = null,
    val nuevaContrasenia: String? = null,
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val fechaNacimiento: String,
    val genero: String,
    val telefono: String,
    val direccion: String,
    val contactoEmergenciaNombre: String,
    val contactoEmergenciaTelefono: String
)

object RetrofitClient {

    const val BASE_URL = "http://192.168.100.10:3000/"

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun resolverUrlImagen(ruta: String?): String? {
        if (ruta.isNullOrBlank()) return null
        if (ruta.startsWith("http://") || ruta.startsWith("https://") || ruta.startsWith("data:")) {
            return ruta
        }
        val baseSinSlash = BASE_URL.trimEnd('/')
        val rutaNormalizada = if (ruta.startsWith("/")) ruta else "/$ruta"
        return "$baseSinSlash$rutaNormalizada"
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
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}

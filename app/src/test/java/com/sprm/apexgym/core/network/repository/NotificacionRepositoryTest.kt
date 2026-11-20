package com.sprm.apexgym.core.network.repository

import com.sprm.apexgym.core.network.api.CrearNotificacionRequest
import com.sprm.apexgym.core.network.api.NotificacionApiService
import com.sprm.apexgym.core.network.api.NotificacionCountDto
import com.sprm.apexgym.core.network.api.NotificacionDto
import com.sprm.apexgym.core.network.client.ApiResponse
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class NotificacionRepositoryTest {

    private val notificacionPendiente = NotificacionDto(
        id = 7,
        titulo = "Renovación de membresía",
        mensaje = "Tu membresía vence en 15 días.",
        tipo = "MEMBRESIA",
        estado = NotificacionDto.ESTADO_PENDIENTE,
        createdAt = "2026-11-20T10:00:00.000Z"
    )

    @Test
    fun `listar devuelve exito y envia el filtro de estado`() = runBlocking {
        val api = FakeNotificacionApi().apply {
            listarResponse = exito(listOf(notificacionPendiente))
        }

        val resultado = NotificacionRepository(api).listar(estado = "PENDIENTE")

        assertTrue(resultado is ResultadoNotificaciones.Exito)
        val datos = (resultado as ResultadoNotificaciones.Exito).datos
        assertEquals(1, datos.size)
        assertEquals(7, datos.first().id)
        assertEquals("PENDIENTE", api.estadoRecibido)
    }

    @Test
    fun `listar devuelve el mensaje del backend cuando responseFlag no es cero`() = runBlocking {
        val api = FakeNotificacionApi().apply {
            listarResponse = Response.success(
                ApiResponse<List<NotificacionDto>>(
                    data = null,
                    statusCode = 403,
                    message = "La cuenta no tiene un destinatario asociado",
                    responseFlag = 1
                )
            )
        }

        val resultado = NotificacionRepository(api).listar()

        assertTrue(resultado is ResultadoNotificaciones.Error)
        assertEquals(
            "La cuenta no tiene un destinatario asociado",
            (resultado as ResultadoNotificaciones.Error).mensaje
        )
    }

    @Test
    fun `contarNoLeidas mapea el contador del envelope`() = runBlocking {
        val api = FakeNotificacionApi().apply {
            countResponse = exito(NotificacionCountDto(count = 3))
        }

        val resultado = NotificacionRepository(api).contarNoLeidas()

        assertEquals(3, (resultado as ResultadoNotificaciones.Exito).datos)
    }

    @Test
    fun `marcarLeida devuelve la notificacion actualizada`() = runBlocking {
        val actualizada = notificacionPendiente.copy(
            estado = NotificacionDto.ESTADO_LEIDA,
            leidaAt = "2026-11-20T11:00:00.000Z"
        )
        val api = FakeNotificacionApi().apply {
            leidaResponse = exito(actualizada)
        }

        val resultado = NotificacionRepository(api).marcarLeida(7)

        val datos = (resultado as ResultadoNotificaciones.Exito).datos
        assertEquals(NotificacionDto.ESTADO_LEIDA, datos.estado)
        assertEquals(7, api.idRecibido)
    }

    @Test
    fun `marcarIgnorada convierte una respuesta HTTP fallida en error`() = runBlocking {
        val api = FakeNotificacionApi().apply {
            ignoradaResponse = Response.error(
                404,
                "".toResponseBody("application/json".toMediaType())
            )
        }

        val resultado = NotificacionRepository(api).marcarIgnorada(99)

        assertTrue(resultado is ResultadoNotificaciones.Error)
        assertEquals(
            "No se pudo completar la operación",
            (resultado as ResultadoNotificaciones.Error).mensaje
        )
    }

    @Test
    fun `listar convierte un fallo de red en mensaje de conexion`() = runBlocking {
        val api = FakeNotificacionApi().apply {
            listarError = IOException("sin red")
        }

        val resultado = NotificacionRepository(api).listar()

        assertTrue(resultado is ResultadoNotificaciones.Error)
        assertTrue(
            (resultado as ResultadoNotificaciones.Error).mensaje.contains("conexión")
        )
    }

    @Test
    fun `crear envia la peticion al endpoint`() = runBlocking {
        val api = FakeNotificacionApi().apply {
            crearResponse = exito(notificacionPendiente)
        }
        val request = CrearNotificacionRequest(
            titulo = "Aviso",
            mensaje = "Mensaje",
            destinatarioId = 15,
            destinatarioTipo = "CLIENTE"
        )

        val resultado = NotificacionRepository(api).crear(request)

        assertTrue(resultado is ResultadoNotificaciones.Exito)
        assertEquals(request, api.requestRecibido)
    }

    private fun <T> exito(datos: T): Response<ApiResponse<T>> =
        Response.success(ApiResponse(datos, 200, "Operación exitosa", 0))

    private class FakeNotificacionApi : NotificacionApiService {
        var listarResponse: Response<ApiResponse<List<NotificacionDto>>> =
            Response.success(ApiResponse(emptyList(), 200, "ok", 0))
        var countResponse: Response<ApiResponse<NotificacionCountDto>> =
            Response.success(ApiResponse(NotificacionCountDto(), 200, "ok", 0))
        var leidaResponse: Response<ApiResponse<NotificacionDto>> =
            Response.success(ApiResponse(null, 200, "ok", 0))
        var ignoradaResponse: Response<ApiResponse<NotificacionDto>> =
            Response.success(ApiResponse(null, 200, "ok", 0))
        var crearResponse: Response<ApiResponse<NotificacionDto>> =
            Response.success(ApiResponse(null, 200, "ok", 0))

        var listarError: Exception? = null
        var estadoRecibido: String? = null
        var idRecibido: Int? = null
        var requestRecibido: CrearNotificacionRequest? = null

        override suspend fun crearNotificacion(
            request: CrearNotificacionRequest
        ): Response<ApiResponse<NotificacionDto>> {
            requestRecibido = request
            return crearResponse
        }

        override suspend fun listarNotificaciones(
            estado: String?,
            tipo: String?
        ): Response<ApiResponse<List<NotificacionDto>>> {
            estadoRecibido = estado
            listarError?.let { throw it }
            return listarResponse
        }

        override suspend fun contarNoLeidas(): Response<ApiResponse<NotificacionCountDto>> =
            countResponse

        override suspend fun marcarLeida(id: Int): Response<ApiResponse<NotificacionDto>> {
            idRecibido = id
            return leidaResponse
        }

        override suspend fun marcarIgnorada(id: Int): Response<ApiResponse<NotificacionDto>> {
            idRecibido = id
            return ignoradaResponse
        }
    }
}

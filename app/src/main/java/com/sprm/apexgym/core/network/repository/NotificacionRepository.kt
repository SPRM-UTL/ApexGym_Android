package com.sprm.apexgym.core.network.repository

import com.sprm.apexgym.core.network.api.CrearNotificacionRequest
import com.sprm.apexgym.core.network.api.NotificacionApiService
import com.sprm.apexgym.core.network.api.NotificacionDto
import com.sprm.apexgym.core.network.client.ApiResponse
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.network.mensajeUsuario
import kotlinx.coroutines.CancellationException
import retrofit2.Response

sealed class ResultadoNotificaciones<out T> {
    data class Exito<T>(val datos: T) : ResultadoNotificaciones<T>()
    data class Error(val mensaje: String) : ResultadoNotificaciones<Nothing>()
}

/**
 * Acceso a los endpoints de notificaciones del backend.
 * Centraliza el envelope (`responseFlag == 0`) y los mensajes de error para las pantallas.
 */
class NotificacionRepository(
    private val api: NotificacionApiService = RetrofitClient.notificacionService
) {

    suspend fun listar(
        estado: String? = null,
        tipo: String? = null
    ): ResultadoNotificaciones<List<NotificacionDto>> =
        ejecutar { api.listarNotificaciones(estado = estado, tipo = tipo) }

    suspend fun contarNoLeidas(): ResultadoNotificaciones<Int> =
        ejecutar { api.contarNoLeidas() }.map { it.count }

    suspend fun marcarLeida(id: Int): ResultadoNotificaciones<NotificacionDto> =
        ejecutar { api.marcarLeida(id) }

    suspend fun marcarIgnorada(id: Int): ResultadoNotificaciones<NotificacionDto> =
        ejecutar { api.marcarIgnorada(id) }

    suspend fun crear(request: CrearNotificacionRequest): ResultadoNotificaciones<NotificacionDto> =
        ejecutar { api.crearNotificacion(request) }

    private suspend fun <T> ejecutar(
        llamada: suspend () -> Response<ApiResponse<T>>
    ): ResultadoNotificaciones<T> {
        return try {
            val response = llamada()
            val datos = response.dataExitosa()
            if (datos != null) {
                ResultadoNotificaciones.Exito(datos)
            } else {
                val mensajeEnvelope = response.body()?.message?.takeIf { it.isNotBlank() }
                ResultadoNotificaciones.Error(
                    mensajeEnvelope
                        ?: response.mensajeDeError()
                        ?: "No se pudo completar la operación"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoNotificaciones.Error(e.mensajeUsuario())
        }
    }
}

private inline fun <T, R> ResultadoNotificaciones<T>.map(
    transformar: (T) -> R
): ResultadoNotificaciones<R> = when (this) {
    is ResultadoNotificaciones.Exito -> ResultadoNotificaciones.Exito(transformar(datos))
    is ResultadoNotificaciones.Error -> ResultadoNotificaciones.Error(mensaje)
}

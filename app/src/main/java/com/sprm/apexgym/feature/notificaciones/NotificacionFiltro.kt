package com.sprm.apexgym.feature.notificaciones

import com.sprm.apexgym.core.network.api.NotificacionDto

/** Aplica el filtro de estado y la búsqueda de texto sobre las notificaciones ya cargadas. */
fun filtrarNotificaciones(
    notificaciones: List<NotificacionDto>,
    estado: String?,
    texto: String
): List<NotificacionDto> {
    val termino = texto.trim()
    return notificaciones.filter { notificacion ->
        val coincideEstado = estado == null || notificacion.estado == estado
        val coincideTexto = termino.isEmpty() ||
            notificacion.titulo.contains(termino, ignoreCase = true) ||
            notificacion.mensaje.contains(termino, ignoreCase = true) ||
            notificacion.tipo.contains(termino, ignoreCase = true)
        coincideEstado && coincideTexto
    }
}

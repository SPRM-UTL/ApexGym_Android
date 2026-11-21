package com.sprm.apexgym.feature.notificaciones

import com.sprm.apexgym.core.network.api.NotificacionDto

/** Aplica el filtro de estado, tipo opcional y la búsqueda de texto sobre las notificaciones ya cargadas. */
fun filtrarNotificaciones(
    notificaciones: List<NotificacionDto>,
    estado: String?,
    texto: String,
    tipo: String? = null
): List<NotificacionDto> {
    val termino = texto.trim()
    val tipoFiltro = tipo?.trim()?.takeIf { it.isNotEmpty() }
    return notificaciones.filter { notificacion ->
        val coincideEstado = estado == null || notificacion.estado == estado
        val coincideTipo = tipoFiltro == null || notificacion.tipo.equals(tipoFiltro, ignoreCase = true)
        val coincideTexto = termino.isEmpty() ||
            notificacion.titulo.contains(termino, ignoreCase = true) ||
            notificacion.mensaje.contains(termino, ignoreCase = true) ||
            notificacion.tipo.contains(termino, ignoreCase = true)
        coincideEstado && coincideTipo && coincideTexto
    }
}

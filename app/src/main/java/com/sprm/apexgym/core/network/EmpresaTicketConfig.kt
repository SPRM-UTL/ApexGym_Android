package com.sprm.apexgym.core.network

import com.sprm.apexgym.core.network.api.EmpresaTicketInfoDto
import com.sprm.apexgym.core.network.client.RetrofitClient

object EmpresaTicketConfig {

    const val DEFAULT_MENSAJE_TICKET = "¡Gracias por entrenar en ApexGym!"

    @Volatile
    private var mensajeCache: String? = null

    fun obtenerMensajeInicial(): String = mensajeCache ?: DEFAULT_MENSAJE_TICKET

    suspend fun obtenerMensajeTicket(): String {
        val info = obtenerInfoEmpresa()
        val mensaje = info?.mensajeTicket?.trim()?.takeIf { it.isNotBlank() }
        if (mensaje != null) {
            mensajeCache = mensaje
            return mensaje
        }
        return mensajeCache ?: DEFAULT_MENSAJE_TICKET
    }

    private suspend fun obtenerInfoEmpresa(): EmpresaTicketInfoDto? {
        try {
            val resPublica = RetrofitClient.authService.obtenerInfoTicketEmpresa().dataExitosa()
            if (resPublica != null) return resPublica
        } catch (_: Exception) {
        }
        return try {
            RetrofitClient.authService.obtenerEmpresaActual().dataExitosa()
        } catch (_: Exception) {
            null
        }
    }
}

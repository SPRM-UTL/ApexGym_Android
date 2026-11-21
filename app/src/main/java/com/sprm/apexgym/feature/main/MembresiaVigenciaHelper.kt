package com.sprm.apexgym.feature.main

import com.sprm.apexgym.core.network.client.ClientePerfilDto
import com.sprm.apexgym.core.network.client.MiMembresiaResponseDto
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object MembresiaVigenciaHelper {

    const val DIAS_ALERTA_POR_VENCER = 5L

    data class ResumenHeroMembresia(
        val badgeTexto: String,
        val subtituloTexto: String,
        val esVigente: Boolean,
        val porVencer: Boolean
    )

    data class ProgresoVigencia(
        val diasTotales: Long,
        val diasTranscurridos: Long,
        val diasRestantes: Long,
        val porcentajeCompletado: Int
    )

    fun calcularDiasRestantes(fechaFinIso: String?, hoy: LocalDate = LocalDate.now()): Long? {
        if (fechaFinIso.isNullOrBlank()) return null
        return try {
            val fechaStr = fechaFinIso.trim().take(10)
            val fin = LocalDate.parse(fechaStr)
            ChronoUnit.DAYS.between(hoy, fin)
        } catch (_: Exception) {
            null
        }
    }

    fun calcularProgreso(
        fechaInicioIso: String?,
        fechaFinIso: String?,
        duracionDiasPlan: Int?,
        hoy: LocalDate = LocalDate.now()
    ): ProgresoVigencia? {
        val inicio = try {
            fechaInicioIso?.trim()?.take(10)?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }
        } catch (_: Exception) {
            null
        }
        val fin = try {
            fechaFinIso?.trim()?.take(10)?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }
        } catch (_: Exception) {
            null
        }

        if (inicio != null && fin != null && !fin.isBefore(inicio)) {
            val diasTotales = (ChronoUnit.DAYS.between(inicio, fin) + 1L).coerceAtLeast(1L)
            val diasRestantesRaw = ChronoUnit.DAYS.between(hoy, fin)
            val diasRestantes = diasRestantesRaw.coerceAtLeast(0L)
            val diasTranscurridos = when {
                hoy.isBefore(inicio) -> 0L
                hoy.isAfter(fin) -> diasTotales
                else -> (ChronoUnit.DAYS.between(inicio, hoy) + 1L).coerceIn(1L, diasTotales)
            }
            val porcentaje = ((diasTranscurridos * 100L) / diasTotales).toInt().coerceIn(0, 100)
            return ProgresoVigencia(
                diasTotales = diasTotales,
                diasTranscurridos = diasTranscurridos,
                diasRestantes = diasRestantes,
                porcentajeCompletado = porcentaje
            )
        }

        if (duracionDiasPlan != null && duracionDiasPlan > 0) {
            val diasTotales = duracionDiasPlan.toLong()
            val diasRestantes = (calcularDiasRestantes(fechaFinIso, hoy) ?: diasTotales).coerceIn(0L, diasTotales)
            val diasTranscurridos = (diasTotales - diasRestantes).coerceIn(0L, diasTotales)
            val porcentaje = ((diasTranscurridos * 100L) / diasTotales).toInt().coerceIn(0, 100)
            return ProgresoVigencia(
                diasTotales = diasTotales,
                diasTranscurridos = diasTranscurridos,
                diasRestantes = diasRestantes,
                porcentajeCompletado = porcentaje
            )
        }

        return null
    }

    fun formatearFechaCorta(fechaIso: String?): String {
        if (fechaIso.isNullOrBlank()) return "—"
        return try {
            val fechaStr = fechaIso.trim().take(10)
            val date = LocalDate.parse(fechaStr)
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "MX"))
            val raw = date.format(formatter).replace(".", "")
            val partes = raw.split(" ").filter { it.isNotBlank() }
            if (partes.size == 3) {
                val dia = partes[0]
                val mes = partes[1].replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale("es", "MX")) else ch.toString()
                }
                val anio = partes[2]
                "$dia $mes $anio"
            } else {
                raw
            }
        } catch (_: Exception) {
            fechaIso.trim().take(10)
        }
    }

    fun formatearPrecioMxn(monto: Double): String {
        val formato = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (monto % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$${formato.format(monto)} MXN"
    }

    fun formatearTextoDiasRestantes(diasRestantes: Long): String {
        return when {
            diasRestantes > 1L -> "$diasRestantes días restantes"
            diasRestantes == 1L -> "1 día restante"
            diasRestantes == 0L -> "Vence hoy"
            else -> "Vencida"
        }
    }

    fun construirNombreCompleto(cliente: ClientePerfilDto?): String? {
        if (cliente == null) return null
        return listOfNotNull(cliente.nombre, cliente.apellidoPaterno, cliente.apellidoMaterno)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .takeIf { it.isNotEmpty() }
    }

    fun construirResumenHero(
        data: MiMembresiaResponseDto?,
        cargando: Boolean,
        errorCarga: Boolean,
        hoy: LocalDate = LocalDate.now()
    ): ResumenHeroMembresia {
        if (cargando && data == null) {
            return ResumenHeroMembresia(
                badgeTexto = "CONSULTANDO...",
                subtituloTexto = "Cargando vigencia de tu membresía...",
                esVigente = false,
                porVencer = false
            )
        }

        if (errorCarga && data == null) {
            return ResumenHeroMembresia(
                badgeTexto = "SIN CONEXIÓN",
                subtituloTexto = "No se pudo consultar tu vigencia • Toca para reintentar",
                esVigente = false,
                porVencer = false
            )
        }

        val activa = data?.membresiaActiva
        if (activa != null) {
            val badgeTexto = activa.tipoMembresia?.nombre
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.uppercase(Locale.getDefault())
                ?: "MEMBRESÍA ACTIVA"

            val fechaFin = formatearFechaCorta(activa.fechaFin)
            val dias = calcularDiasRestantes(activa.fechaFin, hoy)

            val subtituloTexto = when {
                dias != null && dias > 1L -> "Activa hasta $fechaFin • $dias días restantes"
                dias == 1L -> "Activa hasta $fechaFin • 1 día restante"
                dias == 0L -> "Activa hasta $fechaFin • Vence hoy"
                dias != null && dias < 0L -> "Venció el $fechaFin • Toca para ver detalle"
                fechaFin != "—" -> "Activa hasta $fechaFin • Toca para ver detalle"
                else -> "Plan de ${activa.tipoMembresia?.duracionDias ?: 30} días • Toca para ver detalle"
            }

            return ResumenHeroMembresia(
                badgeTexto = badgeTexto,
                subtituloTexto = subtituloTexto,
                esVigente = dias == null || dias >= 0L,
                porVencer = dias != null && dias in 0L..DIAS_ALERTA_POR_VENCER
            )
        }

        val ultima = data?.historial?.firstOrNull()
        val subtituloInactiva = if (ultima != null && !ultima.fechaFin.isNullOrBlank()) {
            "Última vigencia: ${formatearFechaCorta(ultima.fechaFin)} • Ver detalle y planes"
        } else {
            "Sin plan vigente • Toca para ver detalle y planes"
        }

        return ResumenHeroMembresia(
            badgeTexto = "SIN MEMBRESÍA ACTIVA",
            subtituloTexto = subtituloInactiva,
            esVigente = false,
            porVencer = false
        )
    }
}

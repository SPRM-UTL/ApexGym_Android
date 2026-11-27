package com.sprm.apexgym.feature.notificaciones

import java.util.Locale

object NotificacionFormatter {

    private val REGEX_TITULO_ANDROID = Regex(
        """^Android:\s*(.+?)\s*\((POST|PUT|PATCH|DELETE)\)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val REGEX_PREFIJO_ANDROID = Regex("""^Android:\s*""", RegexOption.IGNORE_CASE)
    private val REGEX_METODO_RUTA = Regex("""\s*\((POST|PUT|PATCH|DELETE)\s+([^)]+)\)""", RegexOption.IGNORE_CASE)
    private val REGEX_DESDE_ANDROID = Regex("""\s*desde la app Android(?:\s*\([^)]*\))?""", RegexOption.IGNORE_CASE)
    private val REGEX_SUCURSAL = Regex("""\s*\((Sucursal\s*#\d+)\)\.?$""", RegexOption.IGNORE_CASE)
    private val REGEX_REGISTRO_EN = Regex(
        """\b(registr[oó]|actualiz[oó]|modific[oó]|elimin[oó])\s+un\s+registro\s+en\s+([^.(]+?)(?=\s*(?:\(|desde|\.|en\s+Sucursal|$))""",
        RegexOption.IGNORE_CASE
    )

    fun formatearTitulo(titulo: String?, mensaje: String? = null, tipo: String? = null): String {
        val limpio = titulo?.trim().orEmpty()
        if (limpio.isBlank()) return "Notificación"

        val match = REGEX_TITULO_ANDROID.matchEntire(limpio)
        if (match != null) {
            val recursoRaw = match.groupValues[1].trim()
            val metodo = match.groupValues[2].uppercase(Locale.US)
            val textoMsg = mensaje.orEmpty()
            return construirTituloAmigable(recursoRaw, metodo, textoMsg)
        }

        if (tipo.equals("ACTIVIDAD_EMPLEADO", ignoreCase = true) ||
            REGEX_PREFIJO_ANDROID.containsMatchIn(limpio)
        ) {
            return limpio.replace(REGEX_PREFIJO_ANDROID, "").trim().ifBlank { "Actividad reciente" }
        }

        return limpio
    }

    fun formatearMensaje(mensaje: String?, titulo: String? = null, tipo: String? = null): String {
        val raw = mensaje?.trim().orEmpty()
        if (raw.isBlank()) return ""

        val esActividad = tipo.equals("ACTIVIDAD_EMPLEADO", ignoreCase = true) ||
            REGEX_METODO_RUTA.containsMatchIn(raw) ||
            raw.contains("desde la app Android", ignoreCase = true) ||
            titulo.orEmpty().startsWith("Android:", ignoreCase = true)

        if (!esActividad) return raw

        val matchRuta = REGEX_METODO_RUTA.find(raw)
        val ruta = matchRuta?.groupValues?.getOrNull(2).orEmpty().lowercase(Locale.US)

        var resultado = raw.replace(REGEX_DESDE_ANDROID, " ")
        resultado = resultado.replace(REGEX_METODO_RUTA, " ")

        resultado = REGEX_REGISTRO_EN.replace(resultado) { m ->
            val verbo = m.groupValues[1].lowercase(Locale.getDefault())
            val recurso = m.groupValues[2].trim()
            fraseAccionNatural(verbo, recurso, ruta)
        }

        val sucursalMatch = REGEX_SUCURSAL.find(resultado.trim())
        if (sucursalMatch != null) {
            val sucursal = sucursalMatch.groupValues[1]
            resultado = resultado.trim().substring(0, sucursalMatch.range.first).trimEnd(' ', '.') +
                " en $sucursal."
        }

        resultado = resultado
            .replace(Regex("""\s{2,}"""), " ")
            .replace(" .", ".")
            .trim()

        if (resultado.isNotEmpty() && !resultado.endsWith(".")) {
            resultado += "."
        }
        return resultado
    }

    private fun construirTituloAmigable(recursoRaw: String, metodo: String, mensaje: String): String {
        val recurso = recursoRaw.trim()
        val recursoLower = recurso.lowercase(Locale.getDefault())
        val ruta = REGEX_METODO_RUTA.find(mensaje)?.groupValues?.getOrNull(2).orEmpty().lowercase(Locale.US)

        when {
            ruta.contains("validar-y-registrar") -> return "Acceso registrado"
            ruta.contains("/aprobar") -> return "Pedido de compra aprobado"
            ruta.contains("/rechazar") -> return "Pedido de compra rechazado"
            ruta.contains("/transferir") -> return "Transferencia de stock"
            ruta.contains("/sincronizar-offline") -> return "Ventas offline sincronizadas"
            ruta.contains("/cancelar") -> return "Venta cancelada"
            ruta.contains("/validar") && recursoLower.contains("corte") -> return "Corte de caja validado"
            ruta.contains("/validar") && recursoLower.contains("movimiento") -> return "Movimiento de caja validado"
            ruta.contains("/validar") -> return "Validación en $recurso"
        }

        return when (metodo) {
            "POST" -> when {
                recursoLower == "asistencia" || recursoLower == "asistencias" -> "Asistencia registrada"
                recursoLower.contains("apertura") -> "Apertura de caja registrada"
                recursoLower.contains("corte") -> "Corte de caja realizado"
                recursoLower.contains("movimiento") -> "Movimiento de caja registrado"
                recursoLower == "caja" || recursoLower == "cajas" -> "Nueva caja creada"
                recursoLower == "venta" || recursoLower == "ventas" -> "Nueva venta registrada"
                recursoLower == "visita" || recursoLower == "visitas" -> "Visita registrada"
                recursoLower == "cliente" || recursoLower == "clientes" -> "Nuevo socio registrado"
                recursoLower == "empleado" || recursoLower == "empleados" -> "Nuevo empleado registrado"
                recursoLower.contains("pedido") -> "Nuevo pedido de compra"
                recursoLower.contains("almac") -> "Nuevo almacén registrado"
                else -> "Registro en $recurso"
            }
            "PUT", "PATCH" -> when {
                recursoLower.contains("almac") -> "Inventario de almacén actualizado"
                recursoLower.contains("movimiento") -> "Movimiento de caja actualizado"
                recursoLower.contains("corte") -> "Corte de caja actualizado"
                else -> "Actualización en $recurso"
            }
            "DELETE" -> "Eliminación en $recurso"
            else -> recurso
        }
    }

    private fun fraseAccionNatural(verbo: String, recurso: String, ruta: String): String {
        val recursoLower = recurso.lowercase(Locale.getDefault())
        if (verbo.startsWith("registr")) {
            return when {
                ruta.contains("validar-y-registrar") -> "registró un acceso"
                recursoLower.contains("asistencia") -> "registró una asistencia"
                recursoLower.contains("apertura") -> "abrió una caja"
                recursoLower.contains("corte") -> "realizó un corte de caja"
                recursoLower.contains("movimiento") -> "registró un movimiento de caja"
                recursoLower == "caja" -> "creó una caja"
                recursoLower.contains("venta") -> "registró una venta"
                recursoLower.contains("visita") -> "registró una visita"
                recursoLower.contains("pedido") -> "solicitó un pedido de compra"
                else -> "registró actividad en $recurso"
            }
        }
        if (verbo.startsWith("actualiz") || verbo.startsWith("modific")) {
            return when {
                ruta.contains("/validar") && recursoLower.contains("movimiento") -> "validó un movimiento de caja"
                ruta.contains("/validar") && recursoLower.contains("corte") -> "validó un corte de caja"
                recursoLower.contains("almac") -> "actualizó inventario en $recurso"
                else -> "actualizó información en $recurso"
            }
        }
        if (verbo.startsWith("elimin")) {
            return "eliminó un registro en $recurso"
        }
        return "$verbo en $recurso"
    }
}

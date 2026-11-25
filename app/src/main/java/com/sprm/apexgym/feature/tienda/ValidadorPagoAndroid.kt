package com.sprm.apexgym.feature.tienda

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/**
 * Validación pura en Kotlin (compatible con pruebas unitarias JVM sin Android SDK)
 * para los métodos de pago soportados en tienda cliente y POS.
 */
object ValidadorPagoAndroid {

    const val MARCA_VISA = "VISA"
    const val MARCA_MASTERCARD = "MASTERCARD"
    const val MARCA_AMEX = "AMEX"
    const val MARCA_OTRA = "OTRA"

    const val MAX_LONGITUD_FOLIO_VOUCHER = 100

    private val REGEX_VENCIMIENTO = Regex("^(0[1-9]|1[0-2])/(\\d{2})$")
    private val REGEX_CVV = Regex("^[0-9]{3,4}$")
    private val REGEX_TITULAR = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s.]+$")
    private val REGEX_BANCO = Regex("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s.\\-]+$")
    private val REGEX_REFERENCIA = Regex("^[a-zA-Z0-9\\-\\s]+$")
    private val REGEX_EMAIL_RFC_SIMPLE = Regex(
        "^[A-Za-z0-9.!#\$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    )
    private val REGEX_HTML = Regex("<[^>]*>")

    data class ResultadoValidacionPago(
        val valido: Boolean,
        val mensajeError: String? = null
    )

    data class ResultadoEfectivo(
        val valido: Boolean,
        val montoRecibido: Double = 0.0,
        val cambio: Double = 0.0,
        val mensajeError: String? = null
    )

    data class ResultadoTarjeta(
        val valido: Boolean,
        val tipoTarjeta: String = "DEBITO",
        val marcaTarjeta: String = MARCA_OTRA,
        val ultimosDigitos: String = "",
        val mensajeError: String? = null
    )

    fun redondearDosDecimales(valor: Double): Double {
        if (!valor.isFinite()) return 0.0
        return BigDecimal.valueOf(valor)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun calcularCambio(montoRecibido: Double?, total: Double): Double {
        if (montoRecibido == null || !montoRecibido.isFinite() || !total.isFinite()) return 0.0
        if (montoRecibido < 0.0 || total < 0.0) return 0.0
        val recibidoBd = BigDecimal.valueOf(montoRecibido).setScale(2, RoundingMode.HALF_UP)
        val totalBd = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP)
        if (recibidoBd < totalBd) return 0.0
        return recibidoBd.subtract(totalBd).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun validarEfectivo(montoRecibido: Double?, total: Double): ResultadoEfectivo {
        if (!total.isFinite() || total <= 0.0) {
            return ResultadoEfectivo(
                valido = false,
                mensajeError = "El total de la venta debe ser mayor a cero."
            )
        }
        if (montoRecibido == null || !montoRecibido.isFinite()) {
            return ResultadoEfectivo(
                valido = false,
                mensajeError = "Ingresa un monto recibido válido en efectivo."
            )
        }
        val recibidoBd = BigDecimal.valueOf(montoRecibido).setScale(2, RoundingMode.HALF_UP)
        val totalBd = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP)
        if (recibidoBd < totalBd) {
            return ResultadoEfectivo(
                valido = false,
                montoRecibido = recibidoBd.toDouble(),
                cambio = 0.0,
                mensajeError = "El efectivo recibido no cubre el total a cobrar."
            )
        }
        val cambioBd = recibidoBd.subtract(totalBd).setScale(2, RoundingMode.HALF_UP)
        return ResultadoEfectivo(
            valido = true,
            montoRecibido = recibidoBd.toDouble(),
            cambio = cambioBd.toDouble()
        )
    }

    fun detectarMarcaTarjeta(numeroTarjeta: String): String {
        val digitos = numeroTarjeta.filter { it.isDigit() }
        if (digitos.isEmpty()) return MARCA_OTRA
        if (digitos.startsWith("4")) return MARCA_VISA
        if (digitos.startsWith("34") || digitos.startsWith("37")) return MARCA_AMEX
        val prefijo2 = digitos.take(2).toIntOrNull()
        val prefijo4 = digitos.take(4).toIntOrNull()
        if ((prefijo2 != null && prefijo2 in 51..55) ||
            (prefijo4 != null && prefijo4 in 2221..2720)
        ) {
            return MARCA_MASTERCARD
        }
        return MARCA_OTRA
    }

    fun validarFolioVoucher(
        folioVoucher: String?,
        foliosExistentes: Set<String> = emptySet(),
        obligatorio: Boolean = false
    ): ResultadoValidacionPago {
        val limpio = folioVoucher?.trim().orEmpty()
        if (limpio.isEmpty()) {
            return if (obligatorio) {
                ResultadoValidacionPago(false, "Ingresa el folio o autorización del voucher.")
            } else {
                ResultadoValidacionPago(true)
            }
        }
        if (limpio.length > MAX_LONGITUD_FOLIO_VOUCHER) {
            return ResultadoValidacionPago(
                false,
                "El folio de voucher no puede exceder $MAX_LONGITUD_FOLIO_VOUCHER caracteres."
            )
        }
        if (contieneHtmlOCaracteresDeControl(limpio)) {
            return ResultadoValidacionPago(
                false,
                "El folio de voucher contiene caracteres no permitidos."
            )
        }
        val duplicado = foliosExistentes.any { it.trim().equals(limpio, ignoreCase = true) }
        if (duplicado) {
            return ResultadoValidacionPago(
                false,
                "El folio de voucher ya fue registrado anteriormente."
            )
        }
        return ResultadoValidacionPago(true)
    }

    fun validarTarjeta(
        tipoTarjeta: String,
        numeroTarjeta: String,
        vencimiento: String,
        cvv: String,
        titular: String? = null,
        folioVoucher: String? = null,
        foliosExistentes: Set<String> = emptySet(),
        fechaReferencia: YearMonth = YearMonth.now()
    ): ResultadoTarjeta {
        val tipoNormalizado = tipoTarjeta.trim().uppercase()
        if (tipoNormalizado != "DEBITO" && tipoNormalizado != "CREDITO") {
            return ResultadoTarjeta(
                valido = false,
                mensajeError = "El tipo de tarjeta debe ser DEBITO o CREDITO."
            )
        }

        val sinSeparadores = numeroTarjeta.trim().replace(" ", "").replace("-", "")
        if (sinSeparadores.isEmpty() || !sinSeparadores.all { it.isDigit() }) {
            return ResultadoTarjeta(
                valido = false,
                tipoTarjeta = tipoNormalizado,
                mensajeError = "El número de tarjeta solo puede contener dígitos."
            )
        }

        val marca = detectarMarcaTarjeta(sinSeparadores)
        val longitudEsperada = if (marca == MARCA_AMEX) 15 else 16
        if (sinSeparadores.length != longitudEsperada) {
            val msg = if (marca == MARCA_AMEX) {
                "Las tarjetas AMEX requieren exactamente 15 dígitos."
            } else {
                "Ingresa los 16 dígitos de tu tarjeta."
            }
            return ResultadoTarjeta(
                valido = false,
                tipoTarjeta = tipoNormalizado,
                marcaTarjeta = marca,
                mensajeError = msg
            )
        }

        if (titular != null) {
            val titularLimpio = titular.trim()
            if (titularLimpio.isBlank()) {
                return ResultadoTarjeta(
                    valido = false,
                    tipoTarjeta = tipoNormalizado,
                    marcaTarjeta = marca,
                    mensajeError = "Ingresa el nombre del titular de la tarjeta."
                )
            }
            if (titularLimpio.length !in 3..60 || !REGEX_TITULAR.matches(titularLimpio)) {
                return ResultadoTarjeta(
                    valido = false,
                    tipoTarjeta = tipoNormalizado,
                    marcaTarjeta = marca,
                    mensajeError = "El nombre del titular solo puede contener letras y espacios."
                )
            }
        }

        val vencLimpio = vencimiento.trim()
        val matchVenc = REGEX_VENCIMIENTO.matchEntire(vencLimpio)
            ?: return ResultadoTarjeta(
                valido = false,
                tipoTarjeta = tipoNormalizado,
                marcaTarjeta = marca,
                mensajeError = "Ingresa una fecha de vencimiento válida (MM/AA)."
            )

        val mes = matchVenc.groupValues[1].toInt()
        val anio = 2000 + matchVenc.groupValues[2].toInt()
        val fechaTarjeta = YearMonth.of(anio, mes)
        if (fechaTarjeta.isBefore(fechaReferencia)) {
            return ResultadoTarjeta(
                valido = false,
                tipoTarjeta = tipoNormalizado,
                marcaTarjeta = marca,
                mensajeError = "La tarjeta ingresada se encuentra vencida."
            )
        }

        val cvvLimpio = cvv.trim()
        if (!REGEX_CVV.matches(cvvLimpio)) {
            return ResultadoTarjeta(
                valido = false,
                tipoTarjeta = tipoNormalizado,
                marcaTarjeta = marca,
                mensajeError = "Ingresa el código de seguridad (CVV) de 3 o 4 dígitos."
            )
        }

        if (folioVoucher != null) {
            val resFolio = validarFolioVoucher(
                folioVoucher = folioVoucher,
                foliosExistentes = foliosExistentes,
                obligatorio = true
            )
            if (!resFolio.valido) {
                return ResultadoTarjeta(
                    valido = false,
                    tipoTarjeta = tipoNormalizado,
                    marcaTarjeta = marca,
                    mensajeError = resFolio.mensajeError
                )
            }
        }

        return ResultadoTarjeta(
            valido = true,
            tipoTarjeta = tipoNormalizado,
            marcaTarjeta = marca,
            ultimosDigitos = sinSeparadores.takeLast(4)
        )
    }

    fun validarTransferencia(
        banco: String?,
        referencia: String?
    ): ResultadoValidacionPago {
        val bancoLimpio = banco?.trim().orEmpty()
        if (bancoLimpio.isEmpty()) {
            return ResultadoValidacionPago(
                false,
                "Indica el banco desde el que realizas la transferencia."
            )
        }
        if (contieneHtmlOCaracteresDeControl(bancoLimpio) ||
            bancoLimpio.length !in 2..60 ||
            !REGEX_BANCO.matches(bancoLimpio)
        ) {
            return ResultadoValidacionPago(
                false,
                "El nombre del banco contiene caracteres no permitidos."
            )
        }

        val referenciaLimpia = referencia?.trim().orEmpty()
        if (referenciaLimpia.isEmpty()) {
            return ResultadoValidacionPago(
                false,
                "Ingresa la clave de rastreo o referencia de transferencia."
            )
        }
        if (contieneHtmlOCaracteresDeControl(referenciaLimpia) ||
            referenciaLimpia.length !in 4..40 ||
            !REGEX_REFERENCIA.matches(referenciaLimpia)
        ) {
            return ResultadoValidacionPago(
                false,
                "La referencia de transferencia contiene caracteres no permitidos."
            )
        }

        return ResultadoValidacionPago(true)
    }

    fun validarMercadoPago(
        correo: String?,
        obligatorio: Boolean = true
    ): ResultadoValidacionPago {
        val correoLimpio = correo?.trim().orEmpty()
        if (correoLimpio.isEmpty()) {
            return if (obligatorio) {
                ResultadoValidacionPago(false, "Ingresa un correo electrónico válido.")
            } else {
                ResultadoValidacionPago(true)
            }
        }
        if (correoLimpio.length > 100 ||
            correoLimpio.any { it.isWhitespace() } ||
            contieneHtmlOCaracteresDeControl(correoLimpio) ||
            correoLimpio.any { it in "<>'\"/\\;`" }
        ) {
            return ResultadoValidacionPago(false, "Ingresa un correo electrónico válido.")
        }
        val partes = correoLimpio.split("@")
        if (partes.size != 2) {
            return ResultadoValidacionPago(false, "Ingresa un correo electrónico válido.")
        }
        val local = partes[0]
        if (local.startsWith(".") || local.endsWith(".") || local.contains("..")) {
            return ResultadoValidacionPago(false, "Ingresa un correo electrónico válido.")
        }
        if (!REGEX_EMAIL_RFC_SIMPLE.matches(correoLimpio)) {
            return ResultadoValidacionPago(false, "Ingresa un correo electrónico válido.")
        }
        return ResultadoValidacionPago(true)
    }

    fun contieneHtmlOCaracteresDeControl(texto: String): Boolean {
        if (texto.any { it.code < 0x20 || it.code == 0x7F }) return true
        if (texto.contains('<') || texto.contains('>')) return true
        return REGEX_HTML.containsMatchIn(texto)
    }
}

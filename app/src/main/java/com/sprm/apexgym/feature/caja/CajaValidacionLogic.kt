package com.sprm.apexgym.feature.caja

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

object CajaValidacionLogic {

    const val MONTO_MAX_APERTURA = 200_000.0
    const val MONTO_MAX_MOVIMIENTO = 50_000.0
    const val MAX_CARACTERES_OBSERVACIONES = 500
    const val MAX_CARACTERES_CONCEPTO = 255
    const val TOLERANCIA_CORTE = 0.01

    fun esEstadoCajaActivo(estado: String?): Boolean {
        val norm = estado?.trim()?.uppercase(Locale.ROOT) ?: return false
        return norm == "ACTIVO" || norm == "ACTIVA"
    }

    fun esAperturaAbierta(estado: String?): Boolean =
        estado?.trim()?.uppercase(Locale.ROOT) == "ABIERTA"

    fun requiereJustificacionCorte(diferencia: Double, tolerancia: Double = TOLERANCIA_CORTE): Boolean =
        abs(diferencia) > tolerancia

    fun esCorteCuadrado(diferencia: Double, tolerancia: Double = TOLERANCIA_CORTE): Boolean =
        abs(diferencia) <= tolerancia

    fun validarMontoApertura(monto: Double?, observaciones: String?): String? {
        if (monto == null || monto < 0.0) return "Ingresa un monto inicial válido (mayor o igual a 0)"
        if (monto > MONTO_MAX_APERTURA) return "El monto inicial no puede exceder $200,000.00 MXN"
        if ((observaciones?.trim()?.length ?: 0) > MAX_CARACTERES_OBSERVACIONES) {
            return "Las observaciones no deben exceder $MAX_CARACTERES_OBSERVACIONES caracteres"
        }
        return null
    }

    fun validarMovimientoCaja(
        tipo: String?,
        monto: Double?,
        concepto: String?,
        saldoDisponible: Double?
    ): String? {
        val tipoNorm = tipo?.trim()?.uppercase(Locale.ROOT)
        if (tipoNorm != "ENTRADA" && tipoNorm != "SALIDA") {
            return "Selecciona el tipo de movimiento"
        }
        if (monto == null || monto <= 0.0) return "Ingresa un monto mayor a \$0.00"
        if (monto > MONTO_MAX_MOVIMIENTO) {
            return "El monto máximo por movimiento es de \$50,000.00 MXN"
        }
        val conceptoTrim = concepto?.trim().orEmpty()
        if (conceptoTrim.isEmpty()) return "Ingresa el concepto del movimiento"
        if (conceptoTrim.length > MAX_CARACTERES_CONCEPTO) {
            return "El concepto no debe exceder $MAX_CARACTERES_CONCEPTO caracteres"
        }
        if (tipoNorm == "SALIDA" && saldoDisponible != null && monto > saldoDisponible + 0.009) {
            return "El retiro excede el saldo disponible en caja"
        }
        return null
    }

    fun formatearFechaAperturaUtc(epochMillis: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(epochMillis).truncatedTo(ChronoUnit.SECONDS).toString()
}

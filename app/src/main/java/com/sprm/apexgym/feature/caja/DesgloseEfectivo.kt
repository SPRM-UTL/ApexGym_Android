package com.sprm.apexgym.feature.caja

import kotlin.math.roundToLong

enum class TipoDenominacion(val etiqueta: String) {
    BILLETE("Billete"),
    MONEDA("Moneda")
}

data class DenominacionEfectivo(
    val clave: String,
    val valor: Double,
    val etiqueta: String,
    val tipo: TipoDenominacion
)

data class ResumenDesglose(
    val total: Double,
    val totalBilletes: Double,
    val totalMonedas: Double,
    val piezasTotales: Int
)

/**
 * Catálogo y cálculo de desglose de efectivo (MXN) compatible con el campo
 * `desgloseInicio` del backend y el módulo web de Apertura de Caja.
 */
object DesgloseEfectivo {

    const val MAX_CANTIDAD_POR_DENOMINACION = 1000

    val DENOMINACIONES: List<DenominacionEfectivo> = listOf(
        DenominacionEfectivo("1000", 1000.0, "$1,000", TipoDenominacion.BILLETE),
        DenominacionEfectivo("500", 500.0, "$500", TipoDenominacion.BILLETE),
        DenominacionEfectivo("200", 200.0, "$200", TipoDenominacion.BILLETE),
        DenominacionEfectivo("100", 100.0, "$100", TipoDenominacion.BILLETE),
        DenominacionEfectivo("50", 50.0, "$50", TipoDenominacion.BILLETE),
        DenominacionEfectivo("20", 20.0, "$20", TipoDenominacion.BILLETE),
        DenominacionEfectivo("10", 10.0, "$10", TipoDenominacion.MONEDA),
        DenominacionEfectivo("5", 5.0, "$5", TipoDenominacion.MONEDA),
        DenominacionEfectivo("2", 2.0, "$2", TipoDenominacion.MONEDA),
        DenominacionEfectivo("1", 1.0, "$1", TipoDenominacion.MONEDA),
        DenominacionEfectivo("0.5", 0.5, "$0.50", TipoDenominacion.MONEDA)
    )

    fun normalizarCantidad(cantidad: Int): Int =
        cantidad.coerceIn(0, MAX_CANTIDAD_POR_DENOMINACION)

    fun calcularResumen(conteo: Map<String, Int>): ResumenDesglose {
        var totalBilletes = 0.0
        var totalMonedas = 0.0
        var piezasTotales = 0

        for (denominacion in DENOMINACIONES) {
            val cantidad = normalizarCantidad(conteo[denominacion.clave] ?: 0)
            if (cantidad <= 0) continue

            val subtotal = denominacion.valor * cantidad
            piezasTotales += cantidad
            if (denominacion.tipo == TipoDenominacion.BILLETE) {
                totalBilletes += subtotal
            } else {
                totalMonedas += subtotal
            }
        }

        val billetesRedondeado = redondearDosDecimales(totalBilletes)
        val monedasRedondeado = redondearDosDecimales(totalMonedas)
        val total = redondearDosDecimales(billetesRedondeado + monedasRedondeado)

        return ResumenDesglose(
            total = total,
            totalBilletes = billetesRedondeado,
            totalMonedas = monedasRedondeado,
            piezasTotales = piezasTotales
        )
    }

    /**
     * Serializa el conteo no nulo al formato JSON esperado por `desgloseInicio`
     * (por ejemplo, `{"500":1,"200":3}`). Devuelve `null` si no hay piezas contadas.
     */
    fun serializarJson(conteo: Map<String, Int>): String? {
        val pares = DENOMINACIONES.mapNotNull { denominacion ->
            val cantidad = normalizarCantidad(conteo[denominacion.clave] ?: 0)
            if (cantidad > 0) "\"${denominacion.clave}\":$cantidad" else null
        }
        return if (pares.isEmpty()) null else pares.joinToString(prefix = "{", postfix = "}", separator = ",")
    }

    fun calcularDiferencia(totalDesglose: Double, fondoInicial: Double?): Double? {
        if (fondoInicial == null) return null
        return redondearDosDecimales(totalDesglose - fondoInicial)
    }

    private fun redondearDosDecimales(valor: Double): Double =
        (valor * 100.0).roundToLong() / 100.0
}

package com.sprm.apexgym.feature.tienda

import java.math.BigDecimal
import java.math.RoundingMode

data class TotalesVentaPos(
    val subtotal: Double,
    val iva: Double,
    val total: Double
)

/**
 * Réplica exacta del cálculo de `VentaDao.js` (descuento unitario porcentual,
 * subtotal por línea e IVA del 16% con redondeo a 2 decimales).
 */
object CalculoVentaPos {

    const val TASA_IVA = 0.16

    fun redondearDosDecimales(valor: Double): Double {
        if (!valor.isFinite()) return 0.0
        return BigDecimal.valueOf(valor)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun precioConDescuento(precioVenta: Double, descuentoPorcentaje: Double): Double {
        val precioBase = precioVenta.coerceAtLeast(0.0)
        val pct = descuentoPorcentaje.coerceIn(0.0, 100.0)
        val descuentoUnitario = redondearDosDecimales(precioBase * (pct / 100.0))
        return redondearDosDecimales((precioBase - descuentoUnitario).coerceAtLeast(0.0))
    }

    fun calcularTotales(items: List<Pair<Double, Int>>): TotalesVentaPos {
        val subtotalBd = items.fold(BigDecimal.ZERO) { acc, (precioUnitario, cantidad) ->
            val linea = BigDecimal.valueOf(redondearDosDecimales(precioUnitario))
                .multiply(BigDecimal.valueOf(cantidad.coerceAtLeast(0).toLong()))
                .setScale(2, RoundingMode.HALF_UP)
            acc.add(linea)
        }.setScale(2, RoundingMode.HALF_UP)

        val ivaBd = subtotalBd.multiply(BigDecimal.valueOf(TASA_IVA))
            .setScale(2, RoundingMode.HALF_UP)
        val totalBd = subtotalBd.add(ivaBd).setScale(2, RoundingMode.HALF_UP)

        return TotalesVentaPos(
            subtotal = subtotalBd.toDouble(),
            iva = ivaBd.toDouble(),
            total = totalBd.toDouble()
        )
    }
}

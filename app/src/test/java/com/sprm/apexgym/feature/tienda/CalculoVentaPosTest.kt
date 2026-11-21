package com.sprm.apexgym.feature.tienda

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculoVentaPosTest {

    @Test
    fun `precioConDescuento aplica porcentaje y redondea al centavo igual que VentaDao`() {
        assertEquals(899.0, CalculoVentaPos.precioConDescuento(899.0, 0.0), 0.001)
        assertEquals(809.1, CalculoVentaPos.precioConDescuento(899.0, 10.0), 0.001)
        assertEquals(382.5, CalculoVentaPos.precioConDescuento(450.0, 15.0), 0.001)
        assertEquals(0.0, CalculoVentaPos.precioConDescuento(100.0, 120.0), 0.001)
    }

    @Test
    fun `calcularTotales calcula subtotal iva 16 por ciento y total exacto`() {
        val items = listOf(
            CalculoVentaPos.precioConDescuento(899.0, 10.0) to 2, // 1618.20
            20.0 to 3 // 60.00
        )

        val totales = CalculoVentaPos.calcularTotales(items)

        assertEquals(1678.2, totales.subtotal, 0.001)
        assertEquals(268.51, totales.iva, 0.001)
        assertEquals(1946.71, totales.total, 0.001)
    }
}

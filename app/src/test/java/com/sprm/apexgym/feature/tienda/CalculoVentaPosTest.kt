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

    @Test
    fun `maneja valores limite negativos o no finitos sin propagar NaN`() {
        assertEquals(0.0, CalculoVentaPos.redondearDosDecimales(Double.NaN), 0.001)
        assertEquals(0.0, CalculoVentaPos.redondearDosDecimales(Double.POSITIVE_INFINITY), 0.001)
        assertEquals(0.13, CalculoVentaPos.redondearDosDecimales(0.125), 0.001)
        assertEquals(0.0, CalculoVentaPos.precioConDescuento(-50.0, 10.0), 0.001)
        assertEquals(250.0, CalculoVentaPos.precioConDescuento(250.0, -25.0), 0.001)
    }

    @Test
    fun `calcularTotales ignora cantidades negativas y acumula carrito mixto`() {
        val items = listOf(
            CalculoVentaPos.precioConDescuento(500.0, 20.0) to 1, // producto: 400.00
            250.0 to 1,                                           // servicio: 250.00
            799.0 to 1,                                           // membresia: 799.00
            100.0 to -3                                           // ignorado: 0.00
        )

        val totales = CalculoVentaPos.calcularTotales(items)

        assertEquals(1449.0, totales.subtotal, 0.001)
        assertEquals(231.84, totales.iva, 0.001)
        assertEquals(1680.84, totales.total, 0.001)
    }

    @Test
    fun `creatina de 450 genera 72 de IVA 16 por ciento y total exacto de 522`() {
        val totales = CalculoVentaPos.calcularTotales(listOf(450.0 to 1))

        assertEquals(450.0, totales.subtotal, 0.001)
        assertEquals(72.0, totales.iva, 0.001)
        assertEquals(522.0, totales.total, 0.001)
    }
}

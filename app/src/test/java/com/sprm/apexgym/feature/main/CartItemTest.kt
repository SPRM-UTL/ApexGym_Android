package com.sprm.apexgym.feature.main

import com.sprm.apexgym.feature.tienda.CalculoVentaPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartItemTest {

    @Test
    fun `subtotal de CartItem multiplica precioUnitario neto por cantidad`() {
        val precioConDescuento = CalculoVentaPos.precioConDescuento(400.0, 15.0) // 340.0
        val producto = CartItem(
            tipo = CartItemType.PRODUCTO,
            id = 10,
            nombre = "Creatina Monohidratada",
            subtitulo = "Suplementos",
            precioUnitario = precioConDescuento,
            cantidad = 3,
            stockMaximo = 12
        )

        assertEquals(340.0, producto.precioUnitario, 0.001)
        assertEquals(1020.0, producto.subtotal, 0.001)
    }

    @Test
    fun `CartItem soporta tipos PRODUCTO SERVICIO y MEMBRESIA`() {
        val servicio = CartItem(
            tipo = CartItemType.SERVICIO,
            id = 4,
            nombre = "Entrenamiento Personalizado",
            subtitulo = "Servicio",
            precioUnitario = 350.0,
            cantidad = 1,
            stockMaximo = 1
        )
        val membresia = CartItem(
            tipo = CartItemType.MEMBRESIA,
            id = 2,
            nombre = "Mensualidad Pro",
            subtitulo = "30 días de vigencia",
            precioUnitario = 699.0,
            cantidad = 1,
            stockMaximo = 1
        )

        assertEquals(CartItemType.SERVICIO, servicio.tipo)
        assertEquals(350.0, servicio.subtotal, 0.001)
        assertEquals(CartItemType.MEMBRESIA, membresia.tipo)
        assertEquals(699.0, membresia.subtotal, 0.001)
    }

    @Test
    fun `CartActionResult distingue exito y rechazo con mensaje`() {
        val ok = CartActionResult(exito = true, mensaje = "Producto agregado")
        val rechazo = CartActionResult(exito = false, mensaje = "Stock máximo alcanzado")

        assertTrue(ok.exito)
        assertFalse(rechazo.exito)
        assertEquals("Stock máximo alcanzado", rechazo.mensaje)
    }
}

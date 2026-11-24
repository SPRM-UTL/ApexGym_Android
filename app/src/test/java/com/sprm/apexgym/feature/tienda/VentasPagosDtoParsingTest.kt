package com.sprm.apexgym.feature.tienda

import com.google.gson.Gson
import com.sprm.apexgym.core.network.api.CrearVentaPosRequest
import com.sprm.apexgym.core.network.api.CrearVentaRequest
import com.sprm.apexgym.core.network.api.DetalleVentaPosRequest
import com.sprm.apexgym.core.network.api.DetalleVentaRequest
import com.sprm.apexgym.core.network.api.MetodoPagoDto
import com.sprm.apexgym.core.network.api.PagoVentaPosRequest
import com.sprm.apexgym.core.network.api.PagoVentaRequest
import com.sprm.apexgym.core.network.api.VentaDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VentasPagosDtoParsingTest {

    private val gson = Gson()

    @Test
    fun `serializa y deserializa CrearVentaRequest mixto con Mercado Pago`() {
        val request = CrearVentaRequest(
            metodoPagoId = 4,
            subtotal = 1249.0,
            total = 1249.0,
            montoRecibido = 1249.0,
            detalleVenta = listOf(
                DetalleVentaRequest(
                    tipoItem = "PRODUCTO",
                    cantidad = 2,
                    productoId = 15,
                    descripcion = "Proteína Whey"
                ),
                DetalleVentaRequest(
                    tipoItem = "SERVICIO",
                    cantidad = 1,
                    servicioId = 7,
                    descripcion = "Evaluación física"
                ),
                DetalleVentaRequest(
                    tipoItem = "MEMBRESIA",
                    cantidad = 1,
                    tipoMembresiaId = 3,
                    descripcion = "Plan Gold"
                )
            ),
            uuidIdempotencia = "uuid-mp-001",
            pago = PagoVentaRequest(correoCliente = "socio@apexgym.mx")
        )

        val json = gson.toJson(request)
        val parsed = gson.fromJson(json, CrearVentaRequest::class.java)

        assertEquals(4, parsed.metodoPagoId)
        assertEquals(3, parsed.detalleVenta.size)
        assertEquals(15, parsed.detalleVenta[0].productoId)
        assertNull(parsed.detalleVenta[0].servicioId)
        assertEquals(7, parsed.detalleVenta[1].servicioId)
        assertEquals(3, parsed.detalleVenta[2].tipoMembresiaId)
        assertEquals("socio@apexgym.mx", parsed.pago?.correoCliente)
        assertEquals("uuid-mp-001", parsed.uuidIdempotencia)
    }

    @Test
    fun `serializa CrearVentaPosRequest con voucher y metodo efectivo`() {
        val request = CrearVentaPosRequest(
            empleadoId = 2,
            aperturaCajaId = 9,
            clienteId = 14,
            metodoPagoId = 1,
            subtotal = 500.0,
            total = 580.0,
            montoRecibido = 600.0,
            uuidIdempotencia = "uuid-pos-009",
            detalleVenta = listOf(
                DetalleVentaPosRequest(
                    tipoItem = "PRODUCTO",
                    productoId = 11,
                    cantidad = 1,
                    almacenId = 1
                )
            ),
            pago = PagoVentaPosRequest(folioVoucher = "AUT-778899")
        )

        val json = gson.toJson(request)
        val parsed = gson.fromJson(json, CrearVentaPosRequest::class.java)

        assertEquals(9, parsed.aperturaCajaId)
        assertEquals(580.0, parsed.total, 0.001)
        assertEquals("AUT-778899", parsed.pago?.folioVoucher)
        assertEquals(1, parsed.detalleVenta.first().almacenId)
    }

    @Test
    fun `deserializa VentaDto completa con pagoDigital folioVenta y transaccionesPago`() {
        val rawJson = """
            {
              "id": 412,
              "total": 899.5,
              "subtotal": 775.43,
              "montoRecibido": 900.0,
              "cambio": 0.5,
              "estado": "PENDIENTE_PAGO",
              "clienteId": 22,
              "metodoPagoId": 4,
              "folioVenta": { "id": 10045 },
              "pagoDigital": {
                "estado": "PENDIENTE",
                "urlCheckout": "https://sandbox.mercadopago.com.mx/checkout/v1/redirect?pref_id=123",
                "yaPagada": false,
                "venceEn": "2026-11-24T13:00:00.000Z"
              },
              "metodoPago": { "id": 4, "nombre": "Mercado Pago", "codigo": "MERCADO_PAGO" },
              "detalleVenta": [
                { "id": 1, "cantidad": 1, "subtotal": 899.5, "tipoItem": "MEMBRESIA", "tipoMembresia": { "nombre": "Plan Gold" } }
              ],
              "transaccionesPago": [
                { "id": 88, "uuidIdempotencia": "uuid-tx-88", "proveedor": "MERCADO_PAGO", "monto": 899.5, "estado": "PENDIENTE" }
              ]
            }
        """.trimIndent()

        val venta = gson.fromJson(rawJson, VentaDto::class.java)

        assertEquals(412, venta.id)
        assertEquals(899.5, venta.total, 0.001)
        assertEquals(10045, venta.folioVenta?.id)
        assertNotNull(venta.pagoDigital)
        assertTrue(venta.pagoDigital?.urlCheckout?.contains("mercadopago") == true)
        assertEquals("MERCADO_PAGO", venta.metodoPago?.codigo)
        assertEquals("Plan Gold", venta.detalleVenta?.firstOrNull()?.tipoMembresia?.nombre)
        assertEquals("uuid-tx-88", venta.transaccionesPago?.firstOrNull()?.uuidIdempotencia)
    }

    @Test
    fun `MetodoPagoDto conserva valores por defecto seguros`() {
        val vacio = gson.fromJson("{}", MetodoPagoDto::class.java)
        assertEquals(0, vacio.id)
        assertNull(vacio.codigo)
    }
}

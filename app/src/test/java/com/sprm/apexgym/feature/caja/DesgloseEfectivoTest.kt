package com.sprm.apexgym.feature.caja

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesgloseEfectivoTest {

    @Test
    fun `contiene las 11 denominaciones MXN en orden descendente`() {
        assertEquals(11, DesgloseEfectivo.DENOMINACIONES.size)
        assertEquals("1000", DesgloseEfectivo.DENOMINACIONES.first().clave)
        assertEquals("0.5", DesgloseEfectivo.DENOMINACIONES.last().clave)
    }

    @Test
    fun `calcularResumen suma billetes monedas con cincuenta centavos y acota limites`() {
        val conteo = mapOf(
            "1000" to -2,
            "500" to 1,
            "200" to 3,
            "100" to 3,
            "50" to 2,
            "20" to 5,
            "10" to 2,
            "0.5" to 3,
            "1" to 1500
        )

        val resumen = DesgloseEfectivo.calcularResumen(conteo)

        assertEquals(1600.0, resumen.totalBilletes, 0.001)
        assertEquals(1021.5, resumen.totalMonedas, 0.001)
        assertEquals(2621.5, resumen.total, 0.001)
        assertEquals(1 + 3 + 3 + 2 + 5 + 2 + 3 + 1000, resumen.piezasTotales)
    }

    @Test
    fun `serializarJson devuelve null cuando no hay piezas y respeta orden canonico`() {
        assertNull(DesgloseEfectivo.serializarJson(emptyMap()))
        assertNull(DesgloseEfectivo.serializarJson(mapOf("500" to 0, "10" to -1)))

        val json = DesgloseEfectivo.serializarJson(
            mapOf(
                "0.5" to 3,
                "20" to 5,
                "500" to 1,
                "200" to 3,
                "100" to 3,
                "50" to 2,
                "10" to 0
            )
        )

        assertEquals("""{"500":1,"200":3,"100":3,"50":2,"20":5,"0.5":3}""", json)
    }

    @Test
    fun `calcularDiferencia distingue null cuadre sobrante y faltante`() {
        assertNull(DesgloseEfectivo.calcularDiferencia(1500.0, null))
        assertEquals(0.0, DesgloseEfectivo.calcularDiferencia(1500.0, 1500.0)!!, 0.001)
        assertEquals(120.5, DesgloseEfectivo.calcularDiferencia(1620.5, 1500.0)!!, 0.001)
        assertEquals(-50.25, DesgloseEfectivo.calcularDiferencia(1449.75, 1500.0)!!, 0.001)
    }
}

package com.sprm.apexgym.feature.caja

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CajaValidacionLogicTest {

    @Test
    fun `acepta ACTIVO y ACTIVA y rechaza otros estados`() {
        assertTrue(CajaValidacionLogic.esEstadoCajaActivo("ACTIVO"))
        assertTrue(CajaValidacionLogic.esEstadoCajaActivo(" activa "))
        assertFalse(CajaValidacionLogic.esEstadoCajaActivo("INACTIVO"))
        assertFalse(CajaValidacionLogic.esEstadoCajaActivo(null))
    }

    @Test
    fun `evalua tolerancia de corte sin depender de igualdad exacta float`() {
        assertTrue(CajaValidacionLogic.esCorteCuadrado(0.004))
        assertTrue(CajaValidacionLogic.esCorteCuadrado(-0.01))
        assertFalse(CajaValidacionLogic.requiereJustificacionCorte(0.005))
        assertTrue(CajaValidacionLogic.requiereJustificacionCorte(0.02))
        assertTrue(CajaValidacionLogic.requiereJustificacionCorte(-10.5))
    }

    @Test
    fun `valida monto de apertura dentro de limites`() {
        assertNull(CajaValidacionLogic.validarMontoApertura(500.0, "Turno matutino"))
        assertNotNull(CajaValidacionLogic.validarMontoApertura(-1.0, null))
        assertNotNull(CajaValidacionLogic.validarMontoApertura(250_000.0, null))
    }

    @Test
    fun `valida movimiento salida contra saldo disponible`() {
        assertNull(CajaValidacionLogic.validarMovimientoCaja("ENTRADA", 200.0, "Cambio", 100.0))
        assertNull(CajaValidacionLogic.validarMovimientoCaja("SALIDA", 100.0, "Pago garrafones", 100.0))
        assertNotNull(CajaValidacionLogic.validarMovimientoCaja("SALIDA", 150.0, "Retiro", 100.0))
        assertNotNull(CajaValidacionLogic.validarMovimientoCaja("SALIDA", 0.0, "Cero", 100.0))
        assertNotNull(CajaValidacionLogic.validarMovimientoCaja("ENTRADA", 100.0, "   ", 100.0))
    }

    @Test
    fun `formatea fecha de apertura en ISO UTC con sufijo Z`() {
        val iso = CajaValidacionLogic.formatearFechaAperturaUtc(1_700_000_000_000L)
        assertTrue(iso.endsWith("Z"))
        assertTrue(iso.contains("T"))
    }
}

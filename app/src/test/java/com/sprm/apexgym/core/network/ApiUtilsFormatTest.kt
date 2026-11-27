package com.sprm.apexgym.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiUtilsFormatTest {

    @Test
    fun `formatea montos positivos cero y negativos correctamente`() {
        assertEquals("$1,250.50 MXN", 1250.5.formatearMoneda())
        assertEquals("$0.00 MXN", 0.0.formatearMoneda())
        assertEquals("-$50.00 MXN", (-50.0).formatearMoneda())
        assertEquals("-$50.00", (-50.0).formatearMonedaSinSufijo())
        assertEquals("$450.00", 450.0.formatearMonedaSinSufijo())
    }
}

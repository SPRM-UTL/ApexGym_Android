package com.sprm.apexgym.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputFiltersTest {

    @Test
    fun `nombre de persona permite acentos espacios guiones y rechaza digitos o simbolos`() {
        assertTrue(InputValidationRules.esNombrePersonaValido("José María López"))
        assertTrue(InputValidationRules.esNombrePersonaValido("O'Connor"))
        assertTrue(InputValidationRules.esNombrePersonaValido("Ana-Luisa"))
        assertFalse(InputValidationRules.esNombrePersonaValido("Juan123"))
        assertFalse(InputValidationRules.esNombrePersonaValido("Cliente!@#"))
        assertFalse(InputValidationRules.esNombrePersonaValido("   "))
        assertFalse(InputValidationRules.esNombrePersonaValido("..."))
    }

    @Test
    fun `monto decimal permite enteros y hasta dos decimales y rechaza negativos o multiples puntos`() {
        assertTrue(InputValidationRules.esMontoDecimalParcialValido("", maxEnteros = 6, maxDecimales = 2))
        assertTrue(InputValidationRules.esMontoDecimalParcialValido("1250", maxEnteros = 6, maxDecimales = 2))
        assertTrue(InputValidationRules.esMontoDecimalParcialValido("1250.5", maxEnteros = 6, maxDecimales = 2))
        assertTrue(InputValidationRules.esMontoDecimalParcialValido("1250.55", maxEnteros = 6, maxDecimales = 2))
        assertFalse(InputValidationRules.esMontoDecimalParcialValido("1250.555", maxEnteros = 6, maxDecimales = 2))
        assertFalse(InputValidationRules.esMontoDecimalParcialValido("-50", maxEnteros = 6, maxDecimales = 2))
        assertFalse(InputValidationRules.esMontoDecimalParcialValido("12.3.4", maxEnteros = 6, maxDecimales = 2))
        assertFalse(InputValidationRules.esMontoDecimalParcialValido("1234567", maxEnteros = 6, maxDecimales = 2))
    }

    @Test
    fun `telefono y codigo postal validan exactamente 10 y 5 digitos`() {
        assertTrue(InputValidationRules.esTelefonoValido("4771234567"))
        assertFalse(InputValidationRules.esTelefonoValido("477123456"))
        assertFalse(InputValidationRules.esTelefonoValido("477-123-4567"))

        assertTrue(InputValidationRules.esCodigoPostalValido("37000"))
        assertFalse(InputValidationRules.esCodigoPostalValido("3700"))
        assertFalse(InputValidationRules.esCodigoPostalValido("3700A"))
    }

    @Test
    fun `usuario codigo y texto seguro rechazan espacios o etiquetas html`() {
        assertTrue(InputValidationRules.esCaracterUsuario('a'))
        assertTrue(InputValidationRules.esCaracterUsuario('_'))
        assertFalse(InputValidationRules.esCaracterUsuario(' '))
        assertFalse(InputValidationRules.esCaracterUsuario('<'))

        assertTrue(InputValidationRules.esCaracterCodigo('A'))
        assertTrue(InputValidationRules.esCaracterCodigo('-'))
        assertFalse(InputValidationRules.esCaracterCodigo(' '))

        assertTrue(InputValidationRules.esCaracterSinEspacios('@'))
        assertFalse(InputValidationRules.esCaracterSinEspacios(' '))

        assertTrue(InputValidationRules.esCaracterTextoSeguro('\n', allowNewLines = true))
        assertFalse(InputValidationRules.esCaracterTextoSeguro('\n', allowNewLines = false))
        assertFalse(InputValidationRules.esCaracterTextoSeguro('<', allowNewLines = true))
    }
}

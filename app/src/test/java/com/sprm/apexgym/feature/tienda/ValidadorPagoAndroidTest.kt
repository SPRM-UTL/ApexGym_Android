package com.sprm.apexgym.feature.tienda

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ValidadorPagoAndroidTest {

    private val fechaReferencia = YearMonth.of(2026, 11)

    @Test
    fun `efectivo valido calcula cambio exacto a dos decimales`() {
        val resultado = ValidadorPagoAndroid.validarEfectivo(
            montoRecibido = 500.0,
            total = 349.90
        )

        assertTrue(resultado.valido)
        assertEquals(500.0, resultado.montoRecibido, 0.001)
        assertEquals(150.10, resultado.cambio, 0.001)
    }

    @Test
    fun `efectivo exacto tiene cambio cero y evita imprecision binaria`() {
        val resultado = ValidadorPagoAndroid.validarEfectivo(
            montoRecibido = 100.0,
            total = 89.99
        )

        assertTrue(resultado.valido)
        assertEquals(10.01, resultado.cambio, 0.001)

        val exacto = ValidadorPagoAndroid.validarEfectivo(89.99, 89.99)
        assertTrue(exacto.valido)
        assertEquals(0.0, exacto.cambio, 0.001)
    }

    @Test
    fun `efectivo menor al total o nulo es rechazado`() {
        val menor = ValidadorPagoAndroid.validarEfectivo(99.98, 99.99)
        assertFalse(menor.valido)

        val nulo = ValidadorPagoAndroid.validarEfectivo(null, 100.0)
        assertFalse(nulo.valido)

        val totalCero = ValidadorPagoAndroid.validarEfectivo(100.0, 0.0)
        assertFalse(totalCero.valido)
    }

    @Test
    fun `detecta marcas de tarjeta VISA MASTERCARD AMEX y OTRA`() {
        assertEquals(ValidadorPagoAndroid.MARCA_VISA, ValidadorPagoAndroid.detectarMarcaTarjeta("4111 1111 1111 1111"))
        assertEquals(ValidadorPagoAndroid.MARCA_MASTERCARD, ValidadorPagoAndroid.detectarMarcaTarjeta("5412 3456 7890 1234"))
        assertEquals(ValidadorPagoAndroid.MARCA_MASTERCARD, ValidadorPagoAndroid.detectarMarcaTarjeta("2221 0012 3456 7890"))
        assertEquals(ValidadorPagoAndroid.MARCA_AMEX, ValidadorPagoAndroid.detectarMarcaTarjeta("3400 000000 00009"))
        assertEquals(ValidadorPagoAndroid.MARCA_AMEX, ValidadorPagoAndroid.detectarMarcaTarjeta("3782 822463 10005"))
        assertEquals(ValidadorPagoAndroid.MARCA_OTRA, ValidadorPagoAndroid.detectarMarcaTarjeta("6011 1111 1111 1117"))
    }

    @Test
    fun `tarjeta debito y credito validan longitud 16 y 15 para AMEX`() {
        val visaDebito = ValidadorPagoAndroid.validarTarjeta(
            tipoTarjeta = "DEBITO",
            numeroTarjeta = "4111 1111 1111 1111",
            vencimiento = "11/26",
            cvv = "123",
            titular = "Juan Pérez",
            folioVoucher = "VOUCHER-001",
            fechaReferencia = fechaReferencia
        )
        assertTrue(visaDebito.valido)
        assertEquals(ValidadorPagoAndroid.MARCA_VISA, visaDebito.marcaTarjeta)
        assertEquals("1111", visaDebito.ultimosDigitos)

        val amexCredito = ValidadorPagoAndroid.validarTarjeta(
            tipoTarjeta = "CREDITO",
            numeroTarjeta = "378282246310005",
            vencimiento = "12/28",
            cvv = "1234",
            titular = "Ana López",
            fechaReferencia = fechaReferencia
        )
        assertTrue(amexCredito.valido)
        assertEquals(ValidadorPagoAndroid.MARCA_AMEX, amexCredito.marcaTarjeta)
        assertEquals("0005", amexCredito.ultimosDigitos)

        val amexCon16 = ValidadorPagoAndroid.validarTarjeta(
            tipoTarjeta = "CREDITO",
            numeroTarjeta = "3782822463100051",
            vencimiento = "12/28",
            cvv = "1234",
            fechaReferencia = fechaReferencia
        )
        assertFalse(amexCon16.valido)
    }

    @Test
    fun `tarjeta rechaza fecha vencida CVV invalido y folio voucher duplicado o muy largo`() {
        val vencida = ValidadorPagoAndroid.validarTarjeta(
            tipoTarjeta = "DEBITO",
            numeroTarjeta = "5412345678901234",
            vencimiento = "10/26",
            cvv = "321",
            fechaReferencia = fechaReferencia
        )
        assertFalse(vencida.valido)

        val cvvCorto = ValidadorPagoAndroid.validarTarjeta(
            tipoTarjeta = "DEBITO",
            numeroTarjeta = "5412345678901234",
            vencimiento = "12/27",
            cvv = "12",
            fechaReferencia = fechaReferencia
        )
        assertFalse(cvvCorto.valido)

        val voucherDuplicado = ValidadorPagoAndroid.validarTarjeta(
            tipoTarjeta = "DEBITO",
            numeroTarjeta = "5412345678901234",
            vencimiento = "12/27",
            cvv = "123",
            folioVoucher = "aut-100",
            foliosExistentes = setOf("AUT-100"),
            fechaReferencia = fechaReferencia
        )
        assertFalse(voucherDuplicado.valido)

        val voucherLargo = ValidadorPagoAndroid.validarFolioVoucher("A".repeat(101), obligatorio = true)
        assertFalse(voucherLargo.valido)

        val voucherExacto = ValidadorPagoAndroid.validarFolioVoucher("A".repeat(100), obligatorio = true)
        assertTrue(voucherExacto.valido)
    }

    @Test
    fun `transferencia valida banco no vacio y referencia limpia sin HTML ni control`() {
        val ok = ValidadorPagoAndroid.validarTransferencia("BBVA México", "SPEI-2026-9981")
        assertTrue(ok.valido)

        val bancoNu = ValidadorPagoAndroid.validarTransferencia("Nu", "REF-12345")
        assertTrue(bancoNu.valido)

        val bancoVacio = ValidadorPagoAndroid.validarTransferencia("   ", "REF-12345")
        assertFalse(bancoVacio.valido)

        val refHtml = ValidadorPagoAndroid.validarTransferencia("BBVA", "<script>alert(1)</script>")
        assertFalse(refHtml.valido)

        val refControl = ValidadorPagoAndroid.validarTransferencia("BBVA", "REF123\n456")
        assertFalse(refControl.valido)
    }

    @Test
    fun `mercado pago valida correo con regex compatible JVM`() {
        assertTrue(ValidadorPagoAndroid.validarMercadoPago("socio.gym+1@apexgym.mx").valido)
        assertTrue(ValidadorPagoAndroid.validarMercadoPago("cliente@dominio.com").valido)

        assertFalse(ValidadorPagoAndroid.validarMercadoPago("").valido)
        assertFalse(ValidadorPagoAndroid.validarMercadoPago("correo_sin_arroba.com").valido)
        assertFalse(ValidadorPagoAndroid.validarMercadoPago("correo@dominio").valido)
        assertFalse(ValidadorPagoAndroid.validarMercadoPago("con espacio@dominio.com").valido)
        assertFalse(ValidadorPagoAndroid.validarMercadoPago("mal..punto@dominio.com").valido)
        assertFalse(ValidadorPagoAndroid.validarMercadoPago("<xss>@dominio.com").valido)
    }
}

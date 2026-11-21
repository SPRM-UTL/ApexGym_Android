package com.sprm.apexgym.feature.main

import com.sprm.apexgym.core.network.client.ClienteMembresiaDto
import com.sprm.apexgym.core.network.client.ClientePerfilDto
import com.sprm.apexgym.core.network.client.EstadoMembresiaDto
import com.sprm.apexgym.core.network.client.MiMembresiaResponseDto
import com.sprm.apexgym.core.network.client.TipoMembresiaDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MembresiaVigenciaHelperTest {

    private val hoyFijo = LocalDate.of(2026, 11, 20)

    private val planMensual = TipoMembresiaDto(
        id = 1,
        nombre = "Plan Black VIP",
        descripcion = "Acceso total y zona VIP",
        duracionDias = 30,
        precio = 599.0
    )

    @Test
    fun `estado cargando sin datos devuelve badge consultando`() {
        val resumen = MembresiaVigenciaHelper.construirResumenHero(
            data = null,
            cargando = true,
            errorCarga = false,
            hoy = hoyFijo
        )
        assertEquals("CONSULTANDO...", resumen.badgeTexto)
        assertEquals("Cargando vigencia de tu membresía...", resumen.subtituloTexto)
        assertFalse(resumen.esVigente)
        assertFalse(resumen.porVencer)
    }

    @Test
    fun `estado error sin datos devuelve badge sin conexion`() {
        val resumen = MembresiaVigenciaHelper.construirResumenHero(
            data = null,
            cargando = false,
            errorCarga = true,
            hoy = hoyFijo
        )
        assertEquals("SIN CONEXIÓN", resumen.badgeTexto)
        assertTrue(resumen.subtituloTexto.contains("No se pudo consultar"))
        assertFalse(resumen.esVigente)
    }

    @Test
    fun `membresia activa con varios dias restantes formatea badge y subtitulo`() {
        val activa = ClienteMembresiaDto(
            id = 10,
            fechaInicio = "2026-11-01T00:00:00.000Z",
            fechaFin = "2026-11-30T00:00:00.000Z",
            renovacionAutomatica = true,
            tipoMembresia = planMensual,
            estadoMembresia = EstadoMembresiaDto(id = 1, nombre = "Vigente")
        )
        val data = MiMembresiaResponseDto(membresiaActiva = activa)

        val resumen = MembresiaVigenciaHelper.construirResumenHero(
            data = data,
            cargando = false,
            errorCarga = false,
            hoy = hoyFijo
        )

        assertEquals("PLAN BLACK VIP", resumen.badgeTexto)
        assertEquals("Activa hasta 30 Nov 2026 • 10 días restantes", resumen.subtituloTexto)
        assertTrue(resumen.esVigente)
        assertFalse(resumen.porVencer)
    }

    @Test
    fun `membresia activa con 1 dia restante usa singular y marca por vencer`() {
        val activa = ClienteMembresiaDto(
            id = 11,
            fechaInicio = "2026-10-23",
            fechaFin = "2026-11-21",
            tipoMembresia = planMensual
        )
        val resumen = MembresiaVigenciaHelper.construirResumenHero(
            data = MiMembresiaResponseDto(membresiaActiva = activa),
            cargando = false,
            errorCarga = false,
            hoy = hoyFijo
        )

        assertEquals("Activa hasta 21 Nov 2026 • 1 día restante", resumen.subtituloTexto)
        assertTrue(resumen.esVigente)
        assertTrue(resumen.porVencer)
    }

    @Test
    fun `membresia activa que vence hoy muestra vence hoy y marca por vencer`() {
        val activa = ClienteMembresiaDto(
            id = 12,
            fechaInicio = "2026-10-22",
            fechaFin = "2026-11-20",
            tipoMembresia = planMensual
        )
        val resumen = MembresiaVigenciaHelper.construirResumenHero(
            data = MiMembresiaResponseDto(membresiaActiva = activa),
            cargando = false,
            errorCarga = false,
            hoy = hoyFijo
        )

        assertEquals("Activa hasta 20 Nov 2026 • Vence hoy", resumen.subtituloTexto)
        assertTrue(resumen.esVigente)
        assertTrue(resumen.porVencer)
    }

    @Test
    fun `sin membresia activa con historial muestra ultima vigencia`() {
        val anterior = ClienteMembresiaDto(
            id = 5,
            fechaInicio = "2026-10-01",
            fechaFin = "2026-10-30",
            tipoMembresia = planMensual
        )
        val resumen = MembresiaVigenciaHelper.construirResumenHero(
            data = MiMembresiaResponseDto(membresiaActiva = null, historial = listOf(anterior)),
            cargando = false,
            errorCarga = false,
            hoy = hoyFijo
        )

        assertEquals("SIN MEMBRESÍA ACTIVA", resumen.badgeTexto)
        assertEquals("Última vigencia: 30 Oct 2026 • Ver detalle y planes", resumen.subtituloTexto)
        assertFalse(resumen.esVigente)
    }

    @Test
    fun `sin membresia activa y sin historial invita a ver detalle y planes`() {
        val resumen = MembresiaVigenciaHelper.construirResumenHero(
            data = MiMembresiaResponseDto(membresiaActiva = null, historial = emptyList()),
            cargando = false,
            errorCarga = false,
            hoy = hoyFijo
        )

        assertEquals("SIN MEMBRESÍA ACTIVA", resumen.badgeTexto)
        assertEquals("Sin plan vigente • Toca para ver detalle y planes", resumen.subtituloTexto)
        assertFalse(resumen.esVigente)
    }

    @Test
    fun `calcularProgreso calcula dias totales transcurridos restantes y porcentaje`() {
        val progresoMitad = MembresiaVigenciaHelper.calcularProgreso(
            fechaInicioIso = "2026-11-01",
            fechaFinIso = "2026-11-30",
            duracionDiasPlan = 30,
            hoy = LocalDate.of(2026, 11, 15)
        )
        assertNotNull(progresoMitad)
        assertEquals(30L, progresoMitad!!.diasTotales)
        assertEquals(15L, progresoMitad.diasTranscurridos)
        assertEquals(15L, progresoMitad.diasRestantes)
        assertEquals(50, progresoMitad.porcentajeCompletado)

        val progresoUltimoDia = MembresiaVigenciaHelper.calcularProgreso(
            fechaInicioIso = "2026-11-01",
            fechaFinIso = "2026-11-30",
            duracionDiasPlan = 30,
            hoy = LocalDate.of(2026, 11, 30)
        )
        assertNotNull(progresoUltimoDia)
        assertEquals(30L, progresoUltimoDia!!.diasTranscurridos)
        assertEquals(0L, progresoUltimoDia.diasRestantes)
        assertEquals(100, progresoUltimoDia.porcentajeCompletado)
    }

    @Test
    fun `construirNombreCompleto y formatearPrecioMxn funcionan con enteros y decimales`() {
        val cliente = ClientePerfilDto(
            id = 1,
            nombre = "  Juan ",
            apellidoPaterno = "Pérez",
            apellidoMaterno = " López "
        )
        assertEquals("Juan Pérez López", MembresiaVigenciaHelper.construirNombreCompleto(cliente))
        assertNull(MembresiaVigenciaHelper.construirNombreCompleto(null))
        assertEquals("$599 MXN", MembresiaVigenciaHelper.formatearPrecioMxn(599.0))
        assertEquals("$1,250.50 MXN", MembresiaVigenciaHelper.formatearPrecioMxn(1250.5))
    }
}

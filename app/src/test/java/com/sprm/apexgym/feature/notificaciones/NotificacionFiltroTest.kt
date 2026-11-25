package com.sprm.apexgym.feature.notificaciones

import com.sprm.apexgym.core.network.api.NotificacionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificacionFiltroTest {

    private val pendiente = NotificacionDto(
        id = 1,
        titulo = "Pago pendiente",
        mensaje = "Tu membresía vence pronto.",
        tipo = "MEMBRESIA",
        estado = NotificacionDto.ESTADO_PENDIENTE
    )
    private val leida = NotificacionDto(
        id = 2,
        titulo = "Bienvenido",
        mensaje = "Gracias por registrarte.",
        tipo = "GENERAL",
        estado = NotificacionDto.ESTADO_LEIDA
    )
    private val ignorada = NotificacionDto(
        id = 3,
        titulo = "Promoción",
        mensaje = "Descuento en suplementos.",
        tipo = "PROMOCION",
        estado = NotificacionDto.ESTADO_IGNORADA
    )
    private val notificaciones = listOf(pendiente, leida, ignorada)

    @Test
    fun `sin filtros devuelve todas las notificaciones`() {
        assertEquals(notificaciones, filtrarNotificaciones(notificaciones, null, ""))
    }

    @Test
    fun `filtra por estado pendiente`() {
        val resultado = filtrarNotificaciones(
            notificaciones,
            NotificacionDto.ESTADO_PENDIENTE,
            ""
        )
        assertEquals(listOf(pendiente), resultado)
    }

    @Test
    fun `busca en titulo mensaje y tipo sin distinguir mayusculas`() {
        assertEquals(
            listOf(pendiente),
            filtrarNotificaciones(notificaciones, null, "membresía")
        )
        assertEquals(
            listOf(ignorada),
            filtrarNotificaciones(notificaciones, null, "promocion")
        )
        assertEquals(
            listOf(leida),
            filtrarNotificaciones(notificaciones, null, "gracias")
        )
    }

    @Test
    fun `combina estado y busqueda`() {
        assertTrue(filtrarNotificaciones(notificaciones, "LEIDA", "promoción").isEmpty())
        assertEquals(
            listOf(leida),
            filtrarNotificaciones(notificaciones, "LEIDA", "bienvenido")
        )
    }

    @Test
    fun `filtra por tipo ACTIVIDAD_EMPLEADO sin romper filtros existentes`() {
        val actividad = NotificacionDto(
            id = 4,
            titulo = "Actividad de empleado",
            mensaje = "Registró una venta en tienda.",
            tipo = NotificacionDto.TIPO_ACTIVIDAD_EMPLEADO,
            estado = NotificacionDto.ESTADO_PENDIENTE
        )
        val listaConActividad = notificaciones + actividad

        assertEquals(
            listOf(actividad),
            filtrarNotificaciones(
                notificaciones = listaConActividad,
                estado = null,
                texto = "",
                tipo = NotificacionDto.TIPO_ACTIVIDAD_EMPLEADO
            )
        )
        assertTrue(
            filtrarNotificaciones(
                notificaciones = listaConActividad,
                estado = NotificacionDto.ESTADO_LEIDA,
                texto = "",
                tipo = NotificacionDto.TIPO_ACTIVIDAD_EMPLEADO
            ).isEmpty()
        )
    }
}

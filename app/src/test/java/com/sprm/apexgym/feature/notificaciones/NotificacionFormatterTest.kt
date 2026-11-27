package com.sprm.apexgym.feature.notificaciones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificacionFormatterTest {

    @Test
    fun `formatea titulo y cuerpo de asistencia sin rutas ni verbos HTTP`() {
        val tituloOriginal = "Android: Asistencia (POST)"
        val mensajeOriginal =
            "Ana López registró un registro en Asistencia desde la app Android (POST /asistencias/validar-y-registrar) (Sucursal #1)."

        val titulo = NotificacionFormatter.formatearTitulo(tituloOriginal, mensajeOriginal, "ACTIVIDAD_EMPLEADO")
        val mensaje = NotificacionFormatter.formatearMensaje(mensajeOriginal, tituloOriginal, "ACTIVIDAD_EMPLEADO")

        assertEquals("Acceso registrado", titulo)
        assertFalse(mensaje.contains("POST"))
        assertFalse(mensaje.contains("/asistencias/validar-y-registrar"))
        assertFalse(mensaje.contains("desde la app Android"))
        assertEquals("Ana López registró un acceso en Sucursal #1.", mensaje)
    }

    @Test
    fun `formatea apertura corte y movimiento con titulos claros`() {
        assertEquals(
            "Apertura de caja registrada",
            NotificacionFormatter.formatearTitulo("Android: AperturaCaja (POST)", null, "ACTIVIDAD_EMPLEADO")
        )
        assertEquals(
            "Corte de caja validado",
            NotificacionFormatter.formatearTitulo(
                "Android: CorteCaja (PUT)",
                "Admin validó (PUT /cortes-caja/5/validar)",
                "ACTIVIDAD_EMPLEADO"
            )
        )
        assertEquals(
            "Nueva venta registrada",
            NotificacionFormatter.formatearTitulo("Android: Venta (POST)", null, "ACTIVIDAD_EMPLEADO")
        )
    }

    @Test
    fun `conserva intactas las notificaciones que no son de actividad android`() {
        val titulo = NotificacionFormatter.formatearTitulo("Recordatorio de pago", "Tu membresía vence en 3 días", "MEMBRESIA")
        val mensaje = NotificacionFormatter.formatearMensaje("Tu membresía vence en 3 días", "Recordatorio de pago", "MEMBRESIA")

        assertEquals("Recordatorio de pago", titulo)
        assertEquals("Tu membresía vence en 3 días", mensaje)
    }
}

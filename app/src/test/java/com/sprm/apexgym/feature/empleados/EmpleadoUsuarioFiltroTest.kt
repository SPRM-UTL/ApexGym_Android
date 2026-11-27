package com.sprm.apexgym.feature.empleados

import com.sprm.apexgym.core.network.api.CatalogoDto
import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.api.UsuarioEmpleadoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmpleadoUsuarioFiltroTest {

    @Test
    fun `excluye cuentas con rol cliente exclusivamente o sin roles`() {
        val soloCliente = UsuarioEmpleadoDto(
            id = 1,
            nombre = "Socio Cliente",
            roles = listOf(CatalogoDto(id = 4, nombre = "Cliente"))
        )
        val clienteMinusculas = UsuarioEmpleadoDto(
            id = 2,
            nombre = "Socio 2",
            roles = listOf(CatalogoDto(id = 4, nombre = " cliente "))
        )
        val sinRoles = UsuarioEmpleadoDto(id = 3, nombre = "Sin rol", roles = emptyList())
        val rolesNulos = UsuarioEmpleadoDto(id = 4, nombre = "Null rol", roles = null)
        val empleado = UsuarioEmpleadoDto(
            id = 5,
            nombre = "Recepcionista",
            roles = listOf(CatalogoDto(id = 2, nombre = "Recepcionista"))
        )
        val mixto = UsuarioEmpleadoDto(
            id = 6,
            nombre = "Admin Socio",
            roles = listOf(
                CatalogoDto(id = 4, nombre = "Cliente"),
                CatalogoDto(id = 1, nombre = "Administrador")
            )
        )

        assertFalse(EmpleadoUsuarioFiltro.esUsuarioDeEmpleado(soloCliente))
        assertFalse(EmpleadoUsuarioFiltro.esUsuarioDeEmpleado(clienteMinusculas))
        assertFalse(EmpleadoUsuarioFiltro.esUsuarioDeEmpleado(sinRoles))
        assertFalse(EmpleadoUsuarioFiltro.esUsuarioDeEmpleado(rolesNulos))
        assertTrue(EmpleadoUsuarioFiltro.esUsuarioDeEmpleado(empleado))
        assertTrue(EmpleadoUsuarioFiltro.esUsuarioDeEmpleado(mixto))
    }

    @Test
    fun `filtra clientes y usuarios ocupados por otros empleados manteniendo el actual`() {
        val usuarios = listOf(
            UsuarioEmpleadoDto(id = 10, nombre = "Cliente Uno", roles = listOf(CatalogoDto(nombre = "Cliente"))),
            UsuarioEmpleadoDto(id = 11, nombre = "Otro Emp", roles = listOf(CatalogoDto(nombre = "Empleado"))),
            UsuarioEmpleadoDto(id = 12, nombre = "Actual Emp", roles = listOf(CatalogoDto(nombre = "Recepcion"))),
            UsuarioEmpleadoDto(id = 13, nombre = "Libre Emp", roles = listOf(CatalogoDto(nombre = "Admin")))
        )
        val empleados = listOf(
            EmpleadoDto(id = 1, usuarioId = 11, usuario = usuarios[1]),
            EmpleadoDto(id = 2, usuarioId = 12, usuario = usuarios[2])
        )

        val alCrear = EmpleadoUsuarioFiltro.filtrarUsuariosDisponibles(
            todosUsuarios = usuarios,
            todosEmpleados = empleados,
            empleadoId = 0,
            usuarioActualId = null
        )
        assertEquals(listOf(13), alCrear.map { it.id })

        val alEditar = EmpleadoUsuarioFiltro.filtrarUsuariosDisponibles(
            todosUsuarios = usuarios,
            todosEmpleados = empleados,
            empleadoId = 2,
            usuarioActualId = 12
        )
        assertEquals(listOf(12, 13), alEditar.map { it.id })
    }
}

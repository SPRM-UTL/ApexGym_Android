package com.sprm.apexgym.feature.empleados

import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.api.UsuarioEmpleadoDto

/**
 * Filtra las cuentas de usuario elegibles para vincularse a un empleado,
 * excluyendo cuentas exclusivas de clientes y cuentas ya asignadas a otro empleado.
 */
object EmpleadoUsuarioFiltro {

    fun esUsuarioDeEmpleado(usuario: UsuarioEmpleadoDto): Boolean {
        val roles = usuario.roles.orEmpty()
        if (roles.isEmpty()) return false
        return roles.any { rol ->
            val nombreRol = rol.nombre?.trim()?.lowercase().orEmpty()
            nombreRol.isNotBlank() && nombreRol != "cliente"
        }
    }

    fun filtrarUsuariosDisponibles(
        todosUsuarios: List<UsuarioEmpleadoDto>,
        todosEmpleados: List<EmpleadoDto>,
        empleadoId: Int,
        usuarioActualId: Int?
    ): List<UsuarioEmpleadoDto> {
        val ocupadosPorOtros = todosEmpleados
            .filter { it.id != empleadoId }
            .mapNotNull { it.usuario?.id ?: it.usuarioId }
            .toSet()

        return todosUsuarios.filter { usuario ->
            val disponible = usuario.id == usuarioActualId || !ocupadosPorOtros.contains(usuario.id)
            disponible && esUsuarioDeEmpleado(usuario)
        }
    }
}

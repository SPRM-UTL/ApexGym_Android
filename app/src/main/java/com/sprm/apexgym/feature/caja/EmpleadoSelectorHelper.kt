package com.sprm.apexgym.feature.caja

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.session.SessionManager

object EmpleadoSelectorHelper {

    data class Resultado(
        val empleados: List<EmpleadoDto>
    )

    suspend fun prepararSelector(
        context: Context,
        contenedor: View,
        spinner: Spinner,
        preferirEmpleadoId: Int? = null
    ): Resultado {
        val empleadoSesion = SessionManager.obtenerEmpleadoId(context)
        if (empleadoSesion != null && empleadoSesion > 0) {
            contenedor.visibility = View.GONE
            return Resultado(emptyList())
        }

        val todos = try {
            RetrofitClient.empleadoService.obtenerEmpleados().dataExitosa().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        val activos = todos.filter { emp ->
            val estado = emp.estadoEmpleado?.nombre.orEmpty().uppercase()
            estado.isBlank() || estado.contains("ACTIV") || estado.contains("TURNO")
        }.ifEmpty { todos }

        if (activos.isEmpty()) {
            contenedor.visibility = View.VISIBLE
            spinner.adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Sin empleados disponibles")
            )
            return Resultado(emptyList())
        }

        val nombres = activos.map { emp ->
            val nombre = emp.nombreCompleto.ifBlank { "Empleado #${emp.id}" }
            val puesto = emp.puesto?.nombre?.takeIf { it.isNotBlank() }
            if (puesto != null) "$nombre ($puesto)" else nombre
        }

        contenedor.visibility = View.VISIBLE
        spinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            nombres
        )

        if (preferirEmpleadoId != null && preferirEmpleadoId > 0) {
            val idx = activos.indexOfFirst { it.id == preferirEmpleadoId }
            if (idx >= 0) spinner.setSelection(idx)
        }

        return Resultado(activos)
    }

    fun resolverEmpleadoId(
        context: Context,
        empleadosCargados: List<EmpleadoDto>,
        spinner: Spinner
    ): Int? {
        val empleadoSesion = SessionManager.obtenerEmpleadoId(context)
        if (empleadoSesion != null && empleadoSesion > 0) return empleadoSesion
        return empleadosCargados.getOrNull(spinner.selectedItemPosition)?.id
    }
}

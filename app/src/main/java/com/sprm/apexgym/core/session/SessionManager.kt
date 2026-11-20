package com.sprm.apexgym.core.session

import android.content.Context

/**
 * Guarda el rol, la identidad y la sucursal activa del usuario autenticado.
 * No sustituye al token httpOnly: permite decidir qué UI mostrar y enviar el
 * encabezado `x-sucursal-id` al backend en cada petición.
 */
object SessionManager {

    private const val PREFS = "sesion_apexgym"
    private const val KEY_USUARIO_ID = "usuario_id"
    private const val KEY_EMPLEADO_ID = "empleado_id"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROL = "rol"
    private const val KEY_SUCURSAL_ID = "sucursal_id"
    private const val KEY_SUCURSAL_NOMBRE = "sucursal_nombre"
    private const val KEY_SUCURSAL_CODIGO = "sucursal_codigo"
    private const val KEY_SUCURSAL_CIUDAD = "sucursal_ciudad"
    private const val KEY_PUEDE_CAMBIAR_SUCURSAL = "puede_cambiar_sucursal"

    const val ROL_CLIENTE = "Cliente"
    const val DEFAULT_SUCURSAL_ID = 1
    const val DEFAULT_SUCURSAL_NOMBRE = "Sucursal Matriz"

    fun guardar(
        context: Context,
        usuarioId: Int,
        nombre: String?,
        email: String?,
        rol: String,
        empleadoId: Int?,
        sucursalId: Int = DEFAULT_SUCURSAL_ID
    ) {
        val sucursalValida = if (sucursalId > 0) sucursalId else DEFAULT_SUCURSAL_ID
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_USUARIO_ID, usuarioId)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROL, rol)
            .putInt(KEY_SUCURSAL_ID, sucursalValida)
            .apply {
                if (empleadoId != null) putInt(KEY_EMPLEADO_ID, empleadoId)
                else remove(KEY_EMPLEADO_ID)
            }
            .apply()
    }

    fun guardarSucursal(
        context: Context,
        sucursalId: Int,
        nombre: String?,
        codigo: String? = null,
        ciudad: String? = null,
        puedeCambiar: Boolean? = null
    ) {
        val idValido = if (sucursalId > 0) sucursalId else DEFAULT_SUCURSAL_ID
        prefs(context)
            .edit()
            .putInt(KEY_SUCURSAL_ID, idValido)
            .putString(KEY_SUCURSAL_NOMBRE, nombre?.takeIf { it.isNotBlank() } ?: DEFAULT_SUCURSAL_NOMBRE)
            .putString(KEY_SUCURSAL_CODIGO, codigo?.takeIf { it.isNotBlank() })
            .putString(KEY_SUCURSAL_CIUDAD, ciudad?.takeIf { it.isNotBlank() })
            .apply {
                if (puedeCambiar != null) {
                    putBoolean(KEY_PUEDE_CAMBIAR_SUCURSAL, puedeCambiar)
                }
            }
            .apply()
    }

    fun puedeCambiarSucursal(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PUEDE_CAMBIAR_SUCURSAL, false)

    fun haySesion(context: Context): Boolean = obtenerRol(context) != null

    fun obtenerRol(context: Context): String? =
        prefs(context).getString(KEY_ROL, null)

    fun esEmpleado(context: Context): Boolean {
        val rol = obtenerRol(context) ?: return false
        return !rol.equals(ROL_CLIENTE, ignoreCase = true)
    }

    fun obtenerNombre(context: Context): String? =
        prefs(context).getString(KEY_NOMBRE, null)

    fun obtenerEmail(context: Context): String? =
        prefs(context).getString(KEY_EMAIL, null)

    fun obtenerUsuarioId(context: Context): Int =
        prefs(context).getInt(KEY_USUARIO_ID, 0)

    fun obtenerEmpleadoId(context: Context): Int? {
        val id = prefs(context).getInt(KEY_EMPLEADO_ID, 0)
        return id.takeIf { it > 0 }
    }

    fun obtenerSucursalId(context: Context): Int {
        val id = prefs(context).getInt(KEY_SUCURSAL_ID, DEFAULT_SUCURSAL_ID)
        return if (id > 0) id else DEFAULT_SUCURSAL_ID
    }

    fun obtenerSucursalNombre(context: Context): String =
        prefs(context).getString(KEY_SUCURSAL_NOMBRE, DEFAULT_SUCURSAL_NOMBRE)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_SUCURSAL_NOMBRE

    fun obtenerSucursalCodigo(context: Context): String? =
        prefs(context).getString(KEY_SUCURSAL_CODIGO, null)

    fun obtenerSucursalCiudad(context: Context): String? =
        prefs(context).getString(KEY_SUCURSAL_CIUDAD, null)

    fun obtenerSucursalSubtitulo(context: Context): String {
        val partes = listOfNotNull(
            obtenerSucursalCodigo(context)?.takeIf { it.isNotBlank() },
            obtenerSucursalCiudad(context)?.takeIf { it.isNotBlank() }
        )
        return if (partes.isNotEmpty()) {
            partes.joinToString(" • ")
        } else {
            "ID #${obtenerSucursalId(context)}"
        }
    }

    fun limpiar(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

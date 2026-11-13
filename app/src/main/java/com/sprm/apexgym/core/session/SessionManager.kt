package com.sprm.apexgym.core.session

import android.content.Context

/**
 * Guarda el rol y la identidad del usuario autenticado.
 * No sustituye al token httpOnly: solo permite decidir qué UI mostrar.
 */
object SessionManager {

    private const val PREFS = "sesion_apexgym"
    private const val KEY_USUARIO_ID = "usuario_id"
    private const val KEY_EMPLEADO_ID = "empleado_id"
    private const val KEY_NOMBRE = "nombre"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROL = "rol"

    const val ROL_CLIENTE = "Cliente"

    fun guardar(
        context: Context,
        usuarioId: Int,
        nombre: String?,
        email: String?,
        rol: String,
        empleadoId: Int?
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_USUARIO_ID, usuarioId)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROL, rol)
            .apply {
                if (empleadoId != null) putInt(KEY_EMPLEADO_ID, empleadoId)
                else remove(KEY_EMPLEADO_ID)
            }
            .apply()
    }

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

    fun limpiar(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

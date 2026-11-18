package com.sprm.apexgym.feature.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.LoginRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.client.UsuarioRemoto
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.feature.main.CartManager
import com.sprm.apexgym.feature.main.HomeFragment
import com.sprm.apexgym.feature.main.RegisterFragment
import com.sprm.apexgym.feature.main.EmpleadoDashboardFragment
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

/**
 * Pantalla de Inicio de Sesión conectada a POST /api/clientes/verificarCredenciales.
 */
class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tilCorreo = view.findViewById<TextInputLayout>(R.id.tilCorreo)
        val etCorreo = view.findViewById<TextInputEditText>(R.id.etCorreo)
        val tilContrasena = view.findViewById<TextInputLayout>(R.id.tilContrasena)
        val etContrasena = view.findViewById<TextInputEditText>(R.id.etContrasena)

        val btnIniciarSesion = view.findViewById<MaterialButton>(R.id.btnIniciarSesion)
        val tvRegistrarse = view.findViewById<View>(R.id.tvRegistrarse)
        val llIrARegistro = view.findViewById<View>(R.id.llIrARegistro)

        etCorreo.setText("")
        etContrasena.setText("")

        etCorreo.doAfterTextChanged { tilCorreo.error = null }
        etContrasena.doAfterTextChanged { tilContrasena.error = null }

        btnIniciarSesion.setOnClickListener {
            val correo = etCorreo.text?.toString()?.trim().orEmpty()
            val contrasena = etContrasena.text?.toString().orEmpty()

            var valido = true

            if (correo.isEmpty()) {
                tilCorreo.error = "Ingresa tu correo electrónico"
                valido = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                tilCorreo.error = "Ingresa un correo electrónico válido"
                valido = false
            }

            if (contrasena.isEmpty()) {
                tilContrasena.error = "Ingresa tu contraseña"
                valido = false
            }

            if (!valido) return@setOnClickListener

            btnIniciarSesion.isEnabled = false
            btnIniciarSesion.text = "Iniciando sesión..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val resultado = autenticar(correo, contrasena)
                    val usuario = resultado.usuario
                    val usuarioId = usuario.id
                    val esCliente = resultado.rol.equals(SessionManager.ROL_CLIENTE, ignoreCase = true)

                    val empleadoId = if (esCliente) {
                        null
                    } else {
                        resolverEmpleadoId(usuarioId, correo)
                    }

                    SessionManager.guardar(
                        context = requireContext(),
                        usuarioId = usuarioId,
                        nombre = usuario.nombre,
                        email = usuario.email ?: correo,
                        rol = resultado.rol,
                        empleadoId = empleadoId
                    )
                    CartManager.vaciarCarrito(requireContext())

                    val destino: Fragment = if (esCliente) {
                        HomeFragment()
                    } else {
                        EmpleadoDashboardFragment()
                    }

                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                        .replace(R.id.fragment_container, destino)
                        .commit()
                } catch (e: CredencialesInvalidasException) {
                    Toast.makeText(
                        requireContext(),
                        e.message ?: "Credenciales incorrectas",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: IOException) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo conectar con el servidor. Verifica tu conexión.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error al iniciar sesión: ${e.localizedMessage ?: "desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    if (isAdded) {
                        btnIniciarSesion.isEnabled = true
                        btnIniciarSesion.setText(R.string.login_btn_submit)
                    }
                }
            }
        }

        val irARegistroListener = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack("login_to_register")
                .commit()
        }

        tvRegistrarse.setOnClickListener(irARegistroListener)
        llIrARegistro.setOnClickListener(irARegistroListener)
    }

    private fun extraerMensajeError(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return try {
            JSONObject(rawJson).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private class CredencialesInvalidasException(mensaje: String) : Exception(mensaje)

    private data class ResultadoAutenticacion(
        val usuario: UsuarioRemoto,
        val rol: String
    )

    /**
     * Primero intenta como cliente y, si no existe la cuenta de cliente,
     * reintenta con el endpoint de usuarios (empleados). El rol se toma del
     * catálogo `usuario.roles` que devuelve el backend.
     */
    private suspend fun autenticar(correo: String, contrasena: String): ResultadoAutenticacion {
        val solicitud = LoginRequest(email = correo, contrasenia = contrasena)

        val respuestaCliente = try {
            RetrofitClient.authService.login(solicitud)
        } catch (io: IOException) {
            throw io
        } catch (_: Exception) {
            null
        }

        val usuarioCliente = respuestaCliente?.takeIf { it.isSuccessful && it.body()?.responseFlag == 0 }
            ?.body()?.data?.usuario
        if (usuarioCliente != null) {
            val rol = usuarioCliente.roles
                ?.mapNotNull { it.nombre?.trim() }
                ?.filter { it.isNotEmpty() }
                ?.firstOrNull { !it.equals(SessionManager.ROL_CLIENTE, ignoreCase = true) }
                ?: SessionManager.ROL_CLIENTE
            return ResultadoAutenticacion(usuarioCliente, rol)
        }

        val errorCliente = respuestaCliente?.let {
            extraerMensajeError(it.errorBody()?.string()) ?: it.body()?.message
        }

        val respuestaEmpleado = try {
            RetrofitClient.empleadoService.loginEmpleado(solicitud)
        } catch (io: IOException) {
            throw io
        } catch (_: Exception) {
            null
        }

        val usuarioEmpleado = respuestaEmpleado?.takeIf { it.isSuccessful && it.body()?.responseFlag == 0 }
            ?.body()?.data?.usuario
        if (usuarioEmpleado != null) {
            val rol = usuarioEmpleado.roles
                ?.mapNotNull { it.nombre?.trim() }
                ?.filter { it.isNotEmpty() }
                ?.firstOrNull { !it.equals(SessionManager.ROL_CLIENTE, ignoreCase = true) }
                ?: "Empleado"
            return ResultadoAutenticacion(usuarioEmpleado, rol)
        }

        val errorEmpleado = respuestaEmpleado?.let {
            extraerMensajeError(it.errorBody()?.string()) ?: it.body()?.message
        }

        throw CredencialesInvalidasException(
            errorEmpleado ?: errorCliente ?: "Correo o contraseña incorrectos"
        )
    }

    /** Busca el empleado asociado al usuario autenticado (por id o correo). */
    private suspend fun resolverEmpleadoId(usuarioId: Int, correo: String): Int? {
        return try {
            val empleados = RetrofitClient.empleadoService.obtenerEmpleados().dataExitosa().orEmpty()
            empleados.firstOrNull { it.usuario?.id == usuarioId && usuarioId > 0 }?.id
                ?: empleados.firstOrNull {
                    it.usuario?.email.equals(correo, ignoreCase = true) ||
                        it.correo.equals(correo, ignoreCase = true)
                }?.id
        } catch (_: Exception) {
            null
        }
    }
}

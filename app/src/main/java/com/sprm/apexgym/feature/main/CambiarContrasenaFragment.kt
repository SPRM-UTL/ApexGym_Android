package com.sprm.apexgym.feature.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.client.ActualizarMiPerfilRequest
import com.sprm.apexgym.core.network.client.MiPerfilResponseDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentCambiarContrasenaBinding
import com.sprm.apexgym.feature.auth.LoginFragment
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

class CambiarContrasenaFragment : BaseFragment<FragmentCambiarContrasenaBinding>() {

    private var perfilActual: MiPerfilResponseDto? = null
    private var guardando: Boolean = false

    override fun getLayoutRes(): Int = R.layout.fragment_cambiar_contrasena

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener {
            if (!guardando) {
                parentFragmentManager.popBackStack()
            }
        }

        binding.etContrasenaActual.filters = arrayOf(android.text.InputFilter.LengthFilter(64))
        binding.etNuevaContrasena.filters = arrayOf(android.text.InputFilter.LengthFilter(64))
        binding.etConfirmarContrasena.filters = arrayOf(android.text.InputFilter.LengthFilter(64))

        binding.etContrasenaActual.doAfterTextChanged { binding.tilContrasenaActual.error = null }
        binding.etNuevaContrasena.doAfterTextChanged { binding.tilNuevaContrasena.error = null }
        binding.etConfirmarContrasena.doAfterTextChanged { binding.tilConfirmarContrasena.error = null }

        precargarPerfil()

        binding.btnActualizarContrasena.setOnClickListener {
            validarYActualizar()
        }
    }

    private fun precargarPerfil() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.authService.obtenerMiPerfil()
                if (!isAdded) return@launch
                if (res.code() == 401) {
                    manejarSesionExpirada()
                    return@launch
                }
                if (res.isSuccessful && res.body()?.responseFlag == 0) {
                    perfilActual = res.body()?.data
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun validarYActualizar() {
        if (guardando) return

        val actual = binding.etContrasenaActual.text?.toString().orEmpty()
        val nueva = binding.etNuevaContrasena.text?.toString().orEmpty()
        val confirmar = binding.etConfirmarContrasena.text?.toString().orEmpty()

        var valido = true
        if (actual.isBlank()) {
            binding.tilContrasenaActual.error = "Ingresa tu contraseña actual"
            valido = false
        }
        if (nueva.length < 8) {
            binding.tilNuevaContrasena.error = "La nueva contraseña debe tener al menos 8 caracteres"
            valido = false
        }
        if (confirmar != nueva) {
            binding.tilConfirmarContrasena.error = "Las contraseñas nuevas no coinciden"
            valido = false
        }
        if (!valido) return

        guardando = true
        binding.btnActualizarContrasena.isEnabled = false
        binding.btnActualizarContrasena.text = "Actualizando..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val datos = perfilActual ?: run {
                    val resPerfil = RetrofitClient.authService.obtenerMiPerfil()
                    if (!isAdded) return@launch
                    if (resPerfil.code() == 401) {
                        manejarSesionExpirada()
                        return@launch
                    }
                    resPerfil.body()?.data
                }

                val cliente = datos?.cliente
                val usuario = datos?.usuario
                if (cliente == null || usuario == null) {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "No se pudo obtener tu información de cuenta. Intenta nuevamente.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                val fechaLimpia = cliente.fechaNacimiento
                    ?.takeIf { it.isNotBlank() }
                    ?.split("T")
                    ?.firstOrNull()
                    .orEmpty()

                val request = ActualizarMiPerfilRequest(
                    usuario = usuario.nombre.orEmpty(),
                    email = usuario.email ?: cliente.correo.orEmpty(),
                    contraseniaActual = actual,
                    nuevaContrasenia = nueva,
                    nombre = cliente.nombre.orEmpty(),
                    apellidoPaterno = cliente.apellidoPaterno.orEmpty(),
                    apellidoMaterno = cliente.apellidoMaterno.orEmpty(),
                    fechaNacimiento = fechaLimpia,
                    genero = cliente.genero?.takeIf { it.isNotBlank() } ?: "Masculino",
                    telefono = cliente.telefono.orEmpty(),
                    direccion = cliente.direccion.orEmpty(),
                    contactoEmergenciaNombre = cliente.contactoEmergenciaNombre.orEmpty(),
                    contactoEmergenciaTelefono = cliente.contactoEmergenciaTelefono.orEmpty()
                )

                val response = RetrofitClient.authService.actualizarMiPerfil(request)
                if (!isAdded || view == null) return@launch

                if (response.code() == 401) {
                    manejarSesionExpirada()
                    return@launch
                }

                val body = response.body()
                if (response.isSuccessful && body?.responseFlag == 0) {
                    Toast.makeText(
                        requireContext(),
                        body.message ?: "Contraseña actualizada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                } else {
                    val msg = extraerMensajeError(response.errorBody()?.string())
                        ?: body?.message
                        ?: "No se pudo actualizar la contraseña (${response.code()})"
                    if (msg.contains("actual", ignoreCase = true) ||
                        msg.contains("incorrect", ignoreCase = true)
                    ) {
                        binding.tilContrasenaActual.error = msg
                    } else {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (_: IOException) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo conectar con el servidor. Verifica tu conexión.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Error al actualizar contraseña: ${e.localizedMessage ?: "desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                guardando = false
                if (isAdded && view != null) {
                    binding.btnActualizarContrasena.isEnabled = true
                    binding.btnActualizarContrasena.text = "Actualizar contraseña"
                }
            }
        }
    }

    private fun manejarSesionExpirada() {
        RetrofitClient.cookieJar.clear()
        if (!isAdded) return
        val ctx = requireContext()
        SessionManager.limpiar(ctx)
        CartManager.vaciarCarrito(ctx)
        Toast.makeText(
            ctx,
            "Tu sesión ha expirado. Inicia sesión nuevamente.",
            Toast.LENGTH_LONG
        ).show()
        val fm = requireActivity().supportFragmentManager
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fm.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    private fun extraerMensajeError(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return try {
            JSONObject(rawJson).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}

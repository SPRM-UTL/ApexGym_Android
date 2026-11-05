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
import com.sprm.apexgym.feature.main.HomeFragment
import com.sprm.apexgym.feature.main.RegisterFragment
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
                    val response = RetrofitClient.authService.login(
                        LoginRequest(
                            email = correo,
                            contrasenia = contrasena
                        )
                    )

                    if (response.isSuccessful && response.body()?.responseFlag == 0) {
                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            .replace(R.id.fragment_container, HomeFragment())
                            .commit()
                    } else {
                        val mensajeServidor = extraerMensajeError(response.errorBody()?.string())
                            ?: response.body()?.message
                            ?: "Credenciales incorrectas"

                        Toast.makeText(
                            requireContext(),
                            mensajeServidor,
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
}

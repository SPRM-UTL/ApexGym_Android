package com.sprm.apexgym.feature.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.LoginRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.main.HomeFragment
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etCorreo = view.findViewById<TextInputEditText>(R.id.etCorreo)
        val etContrasena = view.findViewById<TextInputEditText>(R.id.etContrasena)
        val btnIniciarSesion = view.findViewById<MaterialButton>(R.id.btnIniciarSesion)
        val tvRegistrarse = view.findViewById<View>(R.id.tvRegistrarse)
        val llIrARegistro = view.findViewById<View>(R.id.llIrARegistro)

        // Acción Iniciar Sesión con API
        btnIniciarSesion.setOnClickListener {
            val correo = etCorreo.text?.toString()?.trim().orEmpty()
            val contrasena = etContrasena.text?.toString().orEmpty()

            if (correo.isBlank() || contrasena.isBlank()) {
                mostrarMensaje("Ingresa tu correo y contraseña")
                return@setOnClickListener
            }

            btnIniciarSesion.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.authService.login(
                        LoginRequest(
                            email = correo,
                            contrasenia = contrasena
                        )
                    )

                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(requireContext(), "Inicio de sesión correcto", Toast.LENGTH_SHORT).show()

                        // Transición animada hacia Home
                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            .replace(R.id.fragment_container, HomeFragment())
                            .addToBackStack("login_to_home")
                            .commit()
                    } else {
                        val errorMsg = when (response.code()) {
                            401 -> "Correo o contraseña incorrectos"
                            403 -> "No tienes permiso para acceder"
                            else -> "Error del servidor: ${response.code()}"
                        }
                        mostrarMensaje(errorMsg)
                    }
                } catch (e: IOException) {
                    mostrarMensaje("No se pudo conectar con el servidor")
                } catch (e: HttpException) {
                    mostrarMensaje("Error HTTP: ${e.code()}")
                } catch (e: Exception) {
                    mostrarMensaje("Ocurrió un error al iniciar sesión")
                } finally {
                    if (isAdded) {
                        btnIniciarSesion.isEnabled = true
                    }
                }
            }
        }

        // Navegación a Registro con animación
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

    private fun mostrarMensaje(mensaje: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        }
    }
}
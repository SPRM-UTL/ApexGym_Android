package com.sprm.apexgym.feature.auth

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.LoginRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val txtCorreo = view.findViewById<EditText>(R.id.etCorreo)
        val txtContrasena = view.findViewById<EditText>(R.id.etContrasena)
        val txtRegistrarse = view.findViewById<TextView>(R.id.tvRegistrarse)
        val btnIniciarSesion = view.findViewById<MaterialButton>(R.id.btnIniciarSesion)

        txtRegistrarse.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_login, RegistroFragment())
                .addToBackStack(null)
                .commit()
        }

        btnIniciarSesion.setOnClickListener {

            val correo = txtCorreo.text
                ?.toString()
                ?.trim()
                .orEmpty()

            val contrasena = txtContrasena.text
                ?.toString()
                .orEmpty()

            if (correo.isBlank() || contrasena.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Ingresa tu correo y contraseña",
                    Toast.LENGTH_SHORT
                ).show()

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

                    if (response.isSuccessful) {
                        val resultado = response.body()

                        if (resultado != null) {
                            Toast.makeText(
                                requireContext(),
                                "Inicio de sesión correcto",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            mostrarMensaje("El servidor no devolvió datos")
                        }
                    } else {
                        mostrarMensaje(
                            when (response.code()) {
                                401 -> "Correo o contraseña incorrectos"
                                403 -> "No tienes permiso para acceder"
                                else -> "Error del servidor: ${response.code()}"
                            }
                        )
                    }

                } catch (e: IOException) {
                    mostrarMensaje(
                        "No se pudo conectar con el servidor"
                    )
                } catch (e: HttpException) {
                    mostrarMensaje(
                        "Error HTTP: ${e.code()}"
                    )
                } catch (e: Exception) {
                    mostrarMensaje(
                        "Ocurrió un error al iniciar sesión"
                    )
                } finally {
                    if (isAdded) {
                        btnIniciarSesion.isEnabled = true
                    }
                }
            }
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            requireContext(),
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}
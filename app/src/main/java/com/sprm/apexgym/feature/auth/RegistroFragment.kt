package com.sprm.apexgym.feature.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.CorreoRequest
import com.sprm.apexgym.core.network.client.LoginRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class RegistroFragment : Fragment(R.layout.fragment_registro) {

    private val viewModel: RegistroViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val txtNombre = view.findViewById<EditText>(R.id.etUsuario)
        val txtEmail = view.findViewById<EditText>(R.id.etCorreo)
        val txtContrasenia = view.findViewById<EditText>(R.id.etContrasena)
        val txtLogin = view.findViewById<TextView>(R.id.tvLogin)

        txtLogin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_login, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        txtNombre.setText(viewModel.usuario)
        txtEmail.setText(viewModel.email)
        txtContrasenia.setText(viewModel.contrasenia)

        view.findViewById<Button>(R.id.btnContinuar)
            .setOnClickListener {

                val usuario = txtNombre.text.toString().trim()
                val correo = txtEmail.text.toString().trim()
                val contrasenia = txtContrasenia.text.toString()

                var valido = true

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.authService.verificarCorreo(
                            CorreoRequest(correo = correo)
                        )

                        if (response.isSuccessful) {
                            val resultado = response.body()

                            if (resultado != null) {
                                txtEmail.error = "Este correo ya esta siendo utilizado"
                                valido = false
                            } else {
                                mostrarMensaje("El Hubo un problema al verificar el correo")
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
                            e.toString()
                        )
                    }
                }

                if (usuario.length < 3) {
                    txtNombre.error = "Mínimo 3 caracteres"
                    valido = false
                }

                if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(correo).matches()
                ) {
                    txtEmail.error = "Ingresa un correo válido"
                    valido = false
                }

                if (contrasenia.length < 8) {
                    txtContrasenia.error =
                        "La contraseña debe tener al menos 8 caracteres"
                    valido = false
                }

                if (!valido) return@setOnClickListener

                viewModel.usuario = txtNombre.text.toString()
                viewModel.email = txtEmail.text.toString()
                viewModel.contrasenia = txtContrasenia.text.toString()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_login, ClienteFragment())
                    .addToBackStack(null)
                    .commit()
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
package com.sprm.apexgym.feature.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import com.sprm.apexgym.R
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

        // Recuperar datos si regresa a este paso
        txtNombre.setText(viewModel.usuario)
        txtEmail.setText(viewModel.email)
        txtContrasenia.setText(viewModel.contrasenia)

        view.findViewById<Button>(R.id.btnContinuar)
            .setOnClickListener {

                viewModel.usuario = txtNombre.text.toString()
                viewModel.email = txtEmail.text.toString()
                viewModel.contrasenia = txtContrasenia.text.toString()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_login, ClienteFragment())
                    .addToBackStack(null)
                    .commit()
            }
    }
}
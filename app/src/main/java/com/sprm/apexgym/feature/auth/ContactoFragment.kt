package com.sprm.apexgym.feature.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.sprm.apexgym.R

class ContactoFragment : Fragment(R.layout.fragment_contacto) {
    private val viewModel: RegistroViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val txtTelefono = view.findViewById<EditText>(R.id.etTelefono)
        val txtDireccion = view.findViewById<EditText>(R.id.etDireccion)
        val txtContacto = view.findViewById<EditText>(R.id.etContactoENombre)
        val txtContactoTelefono = view.findViewById<EditText>(R.id.etContactoETelefono)
        val progress = view.findViewById<ProgressBar>(R.id.progressRegistro)
        val btnRegistro = view.findViewById<Button>(R.id.btnRegistro)
        txtTelefono.setText(viewModel.telefono)
        txtDireccion.setText(viewModel.direccion)
        txtContacto.setText(viewModel.contactoEmergenciaNombre)
        txtContactoTelefono.setText(viewModel.contactoEmergenciaTelefono)

        btnRegistro.setOnClickListener {

            val telefono = txtTelefono.text.toString().trim()
            val direccion = txtDireccion.text.toString().trim()
            val contacto = txtContacto.text.toString().trim()
            val telefonoContacto = txtContactoTelefono.text.toString().trim()
            var valido = true

            if (!telefono.matches(Regex("^\\d{10}$"))) {
                txtTelefono.error = "Ingresa un teléfono de 10 dígitos"
                valido = false
            }

            if (direccion.length < 5) {
                txtDireccion.error = "Ingresa una dirección válida"
                valido = false
            }

            if (contacto.length < 3) {
                txtContacto.error = "Ingresa el nombre del contacto"
                valido = false
            }

            if (!telefonoContacto.matches(Regex("^\\d{10}$"))) {
                txtContactoTelefono.error = "Ingresa un teléfono de 10 dígitos"
                valido = false
            }

            if (!valido) return@setOnClickListener
                viewModel.telefono = txtTelefono.text.toString()
                viewModel.direccion = txtDireccion.text.toString()
                viewModel.contactoEmergenciaNombre = txtContacto.text.toString()
                viewModel.contactoEmergenciaTelefono = txtContactoTelefono.text.toString()

                val datos = viewModel.obtenerDatos()
                viewModel.registrarCliente(datos)
                viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
                    progress.visibility =
                        if (cargando) View.VISIBLE else View.GONE

                    btnRegistro.isEnabled = !cargando

                    btnRegistro.text = if (cargando) {
                        "Registrando..."
                    } else {
                        "Registrarse"
                    }
                }
                viewModel.mensaje.observe(viewLifecycleOwner) { mensaje ->
                    Toast.makeText(
                        requireContext(),
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                    if (mensaje.contains("exitosamente", ignoreCase = true)) {
                        parentFragmentManager.popBackStack(
                            null,
                            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                    }
                }
            }
    }
}
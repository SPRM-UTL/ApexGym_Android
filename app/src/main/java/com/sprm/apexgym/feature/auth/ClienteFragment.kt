package com.sprm.apexgym.feature.auth

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.sprm.apexgym.R
import java.util.Calendar
import java.util.Locale

class ClienteFragment : Fragment(R.layout.fragment_cliente) {
    private val viewModel: RegistroViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val txtNombre = view.findViewById<EditText>(R.id.etNombre)
        val txtApePa = view.findViewById<EditText>(R.id.etApePaterno)
        val txtApeMa = view.findViewById<EditText>(R.id.etApeMaterno)
        val txtFecha = view.findViewById<EditText>(R.id.etFechaNacimiento)
        val txtGenero = view.findViewById<EditText>(R.id.etGenero)
        val txtTelefono = view.findViewById<EditText>(R.id.etTelefono)
        val txtDireccion = view.findViewById<EditText>(R.id.etDireccion)
        val txtContacto = view.findViewById<EditText>(R.id.etContactoENombre)
        val txtContactoTelefono = view.findViewById<EditText>(R.id.etContactoETelefono)

        txtNombre.setText(viewModel.nombre)
        txtApePa.setText(viewModel.apellidoPaterno)
        txtApeMa.setText(viewModel.apellidoMaterno)
        txtFecha.setText(viewModel.fechaNacimiento)
        txtGenero.setText(viewModel.genero)
        txtTelefono.setText(viewModel.telefono)
        txtDireccion.setText(viewModel.direccion)
        txtContacto.setText(viewModel.contactoEmergenciaNombre)
        txtContactoTelefono.setText(viewModel.contactoEmergenciaTelefono)

        txtFecha.setOnClickListener {

            val calendario = Calendar.getInstance()

            val anio = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, anioSeleccionado, mesSeleccionado, diaSeleccionado ->

                    val fecha = String.format(
                        Locale.getDefault(),
                        "%04d-%02d-%02d",
                        anioSeleccionado,
                        mesSeleccionado + 1,
                        diaSeleccionado
                    )

                    txtFecha.setText(fecha)
                },
                anio,
                mes,
                dia
            )

            // No permitir seleccionar fechas futuras
            datePicker.datePicker.maxDate =
                System.currentTimeMillis()

            datePicker.show()
        }

        view.findViewById<Button>(R.id.btnRegistro)
            .setOnClickListener {
                viewModel.nombre = txtNombre.text.toString()
                viewModel.apellidoPaterno = txtApePa.text.toString()
                viewModel.apellidoMaterno = txtApeMa.text.toString()
                viewModel.fechaNacimiento = txtFecha.text.toString()
                viewModel.genero = txtGenero.text.toString()
                viewModel.telefono = txtTelefono.text.toString()
                viewModel.direccion = txtDireccion.text.toString()
                viewModel.contactoEmergenciaNombre = txtContacto.text.toString()
                viewModel.contactoEmergenciaTelefono = txtContactoTelefono.text.toString()

                val datos = viewModel.obtenerDatos()
                viewModel.registrarCliente(datos)
                viewModel.mensaje.observe(viewLifecycleOwner) { mensaje ->
                    Toast.makeText(
                        requireContext(),
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
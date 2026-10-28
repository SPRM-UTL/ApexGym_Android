package com.sprm.apexgym.feature.auth

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.sprm.apexgym.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
        val spGenero = view.findViewById<Spinner>(R.id.spGenero)
        val txtLogin = view.findViewById<TextView>(R.id.tvLogin)

        txtLogin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_login, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        val generos = listOf(
            "Selecciona un género",
            "Masculino",
            "Femenino",
            "Otro"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            generos
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spGenero.adapter = adapter

        txtNombre.setText(viewModel.nombre)
        txtApePa.setText(viewModel.apellidoPaterno)
        txtApeMa.setText(viewModel.apellidoMaterno)
        txtFecha.setText(viewModel.fechaNacimiento)
        val posicion = generos.indexOf(viewModel.genero)

        if (posicion >= 0) {
            spGenero.setSelection(posicion)
        }

        txtFecha.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                mostrarDatePicker(txtFecha)
            }
            true
        }

        view.findViewById<Button>(R.id.btnRegistro)
            .setOnClickListener {

                val nombre = txtNombre.text.toString().trim()
                val apePa = txtApePa.text.toString().trim()
                val apeMa = txtApeMa.text.toString().trim()
                val fecha = txtFecha.text.toString().trim()
                val genero = spGenero.selectedItem.toString()

                val patronNombre = Regex(
                    "^[\\p{L}][\\p{L} '-]*$"
                )

                var valido = true

                if (!patronNombre.matches(nombre)) {
                    txtNombre.error = "Ingresa un nombre válido"
                    valido = false
                }

                if (!patronNombre.matches(apePa)) {
                    txtApePa.error = "Ingresa un apellido válido"
                    valido = false
                }

                if (!patronNombre.matches(apeMa)) {
                    txtApeMa.error = "Ingresa un apellido válido"
                    valido = false
                }

                val formato = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).apply {
                    isLenient = false
                }

                val fechaValida = try {
                    val date = formato.parse(fecha)
                    date != null &&
                            formato.format(date) == fecha &&
                            !date.after(Date())
                } catch (e: Exception) {
                    false
                }

                if (!fechaValida) {
                    txtFecha.error = "Selecciona una fecha válida"
                    valido = false
                }

                if (genero == "Selecciona un género") {
                    Toast.makeText(
                        requireContext(),
                        "Selecciona un género",
                        Toast.LENGTH_SHORT
                    ).show()
                    valido = false
                }

                if (!valido) return@setOnClickListener

                viewModel.nombre = txtNombre.text.toString()
                viewModel.apellidoPaterno = txtApePa.text.toString()
                viewModel.apellidoMaterno = txtApeMa.text.toString()
                viewModel.fechaNacimiento = txtFecha.text.toString()
                viewModel.genero = spGenero.selectedItem.toString()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_login, ContactoFragment())
                    .addToBackStack(null)
                    .commit()
            }
    }

    private fun mostrarDatePicker(
        campoFecha: EditText
    ) {
        val calendario = Calendar.getInstance()

        val dialogo = DatePickerDialog(
            requireContext(),
            { _, anio, mes, dia ->
                val fecha = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    anio,
                    mes + 1,
                    dia
                )

                campoFecha.setText(fecha)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        dialogo.datePicker.maxDate = System.currentTimeMillis()
        dialogo.show()
    }
}
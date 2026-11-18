package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.CatalogoDto
import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.api.EmpleadoRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.network.normalizarFechaAEnviar
import com.sprm.apexgym.databinding.FragmentCrearEditarEmpleadoBinding
import kotlinx.coroutines.launch

class CrearEditarEmpleadoFragment : BaseFragment<FragmentCrearEditarEmpleadoBinding>() {

    private var empleadoId: Int = 0
    private var empleadoActual: EmpleadoDto? = null

    private var puestos: List<CatalogoDto> = emptyList()
    private var areas: List<CatalogoDto> = emptyList()
    private var estados: List<CatalogoDto> = emptyList()

    override fun getLayoutRes(): Int = R.layout.fragment_crear_editar_empleado

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        empleadoId = arguments?.getInt(ARG_EMPLEADO_ID, 0) ?: 0

        binding.tvTitle.text = if (empleadoId > 0) "Editar Empleado" else "Nuevo Empleado"

        // Campos que la API de empleados no expone.
        ocultarCampo(binding.etDni)
        ocultarCampo(binding.etSueldoBase)
        ocultarCampo(binding.etComisionPorcentaje)
        ocultarCampo(binding.etCuentaBancaria)
        binding.cbActivo.visibility = View.GONE
        binding.rvHorarioConfig.visibility = View.GONE
        binding.btnAgregarDiaHorario.visibility = View.GONE
        binding.tvHorarioConfigHint.text =
            "No disponible: la API no expone horarios ni turnos de empleado."
        binding.tvChangeAvatarHint.text =
            "La API no permite subir foto desde esta pantalla."

        binding.flChangeAvatar.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "La carga de foto de empleado no está disponible en la API actual.",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnCancelar.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnAgregarDiaHorario.visibility = View.GONE

        cargarDatos()
    }

    private fun cargarDatos() {
        binding.btnGuardar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            puestos = RetrofitClient.empleadoService.obtenerPuestos().dataExitosa().orEmpty()
            areas = RetrofitClient.empleadoService.obtenerAreasTrabajo().dataExitosa().orEmpty()
            estados = RetrofitClient.empleadoService.obtenerEstadosEmpleado().dataExitosa().orEmpty()

            configurarDropdown(binding.actvRol, puestos)
            configurarDropdown(binding.actvTipoContrato, areas)
            configurarDropdown(binding.actvEstadoEmpleado, estados)

            if (empleadoId > 0) {
                cargarEmpleado()
            } else {
                binding.btnGuardar.isEnabled = true
                binding.etFechaIngreso.setText(com.sprm.apexgym.core.network.fechaHoyIso())
            }
        }
    }

    private fun configurarDropdown(
        campo: android.widget.AutoCompleteTextView,
        opciones: List<CatalogoDto>
    ) {
        val nombres = opciones.mapNotNull { it.nombre }
        campo.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                nombres
            )
        )
    }

    private suspend fun cargarEmpleado() {
        try {
            val empleado = RetrofitClient.empleadoService.obtenerEmpleado(empleadoId).dataExitosa()
            if (empleado == null) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo cargar el empleado",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            empleadoActual = empleado
            binding.etNombres.setText(empleado.nombre.orEmpty())
            binding.etApellidos.setText(
                listOfNotNull(empleado.apellidoPaterno, empleado.apellidoMaterno)
                    .joinToString(" ")
            )
            binding.etTelefono.setText(empleado.telefono.orEmpty())
            binding.etEmail.setText(empleado.correo ?: empleado.usuario?.email.orEmpty())
            binding.etDireccion.setText(empleado.direccion.orEmpty())
            binding.etFechaNacimiento.setText(
                formatearFecha(empleado.fechaNacimiento, "dd/MM/yyyy")
                    .takeUnless { it == "—" }
                    .orEmpty()
            )
            binding.etFechaIngreso.setText(
                formatearFecha(empleado.fechaIngreso, "dd/MM/yyyy")
                    .takeUnless { it == "—" }
                    .orEmpty()
            )

            binding.actvRol.setText(empleado.puesto?.nombre.orEmpty(), false)
            binding.actvTipoContrato.setText(empleado.areaTrabajo?.nombre.orEmpty(), false)
            binding.actvEstadoEmpleado.setText(empleado.estadoEmpleado?.nombre.orEmpty(), false)
            binding.btnGuardar.isEnabled = true
        } catch (_: Exception) {
            Toast.makeText(
                requireContext(),
                "No se pudo conectar con el servidor",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun guardar() {
        val nombre = binding.etNombres.text?.toString()?.trim().orEmpty()
        val apellidos = binding.etApellidos.text?.toString()?.trim().orEmpty()
        val telefono = binding.etTelefono.text?.toString()?.trim().orEmpty()
        val fechaNacimiento = normalizarFechaAEnviar(
            binding.etFechaNacimiento.text?.toString().orEmpty()
        )
        val fechaIngreso = normalizarFechaAEnviar(
            binding.etFechaIngreso.text?.toString().orEmpty()
        )

        val puesto = puestos.firstOrNull { it.nombre == binding.actvRol.text.toString() }
        val area = areas.firstOrNull { it.nombre == binding.actvTipoContrato.text.toString() }
        val estado = estados.firstOrNull {
            it.nombre == binding.actvEstadoEmpleado.text.toString()
        }

        when {
            nombre.isBlank() -> return aviso("Ingresa el nombre del empleado")
            apellidos.isBlank() -> return aviso("Ingresa los apellidos del empleado")
            telefono.length != 10 || telefono.any { !it.isDigit() } ->
                return aviso("El teléfono debe tener 10 dígitos")
            fechaNacimiento == null -> return aviso("Ingresa una fecha de nacimiento válida")
            fechaIngreso == null -> return aviso("Ingresa una fecha de ingreso válida")
            puesto == null -> return aviso("Selecciona un puesto")
            area == null -> return aviso("Selecciona un área de trabajo")
            estado == null -> return aviso("Selecciona un estado de empleado")
        }

        val partesApellido = apellidos.split(" ", limit = 2)
        val solicitud = EmpleadoRequest(
            puestoId = puesto.id,
            areaTrabajoId = area.id,
            estadoEmpleadoId = estado.id,
            usuarioId = empleadoActual?.usuarioId,
            nombre = nombre,
            apellidoPaterno = partesApellido.first(),
            apellidoMaterno = partesApellido.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() },
            telefono = telefono,
            correo = binding.etEmail.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            direccion = binding.etDireccion.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            fechaNacimiento = fechaNacimiento!!,
            fechaIngreso = fechaIngreso!!
        )

        binding.btnGuardar.isEnabled = false
        binding.btnGuardar.text = "Guardando..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = if (empleadoId > 0) {
                    RetrofitClient.empleadoService.actualizarEmpleado(empleadoId, solicitud)
                } else {
                    RetrofitClient.empleadoService.crearEmpleado(solicitud)
                }

                val empleado = response.dataExitosa()
                if (empleado == null) {
                    aviso(response.mensajeDeError() ?: "No se pudo guardar el empleado")
                } else {
                    Toast.makeText(
                        requireContext(),
                        if (empleadoId > 0) "Empleado actualizado" else "Empleado creado",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (_: Exception) {
                aviso("No se pudo conectar con el servidor")
            } finally {
                if (isAdded) {
                    binding.btnGuardar.isEnabled = true
                    binding.btnGuardar.text = "Guardar"
                }
            }
        }
    }

    private fun aviso(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
    }

    private fun ocultarCampo(campo: View) {
        var actual: View? = campo
        while (actual != null && actual !is com.google.android.material.textfield.TextInputLayout) {
            actual = actual.parent as? View
        }
        (actual ?: campo).visibility = View.GONE
    }

    companion object {
        private const val ARG_EMPLEADO_ID = "arg_empleado_id"

        fun newInstance(empleadoId: Int = 0): CrearEditarEmpleadoFragment {
            return CrearEditarEmpleadoFragment().apply {
                arguments = Bundle().apply { putInt(ARG_EMPLEADO_ID, empleadoId) }
            }
        }
    }
}

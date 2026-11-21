package com.sprm.apexgym.feature.empleados

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.CatalogoDto
import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.api.EmpleadoRequest
import com.sprm.apexgym.core.network.api.UsuarioEmpleadoDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.network.normalizarFechaAEnviar
import com.sprm.apexgym.databinding.FragmentCrearEditarEmpleadoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.Calendar
import java.util.Locale

class CrearEditarEmpleadoFragment : BaseFragment<FragmentCrearEditarEmpleadoBinding>() {

    private var empleadoId: Int = 0
    private var empleadoActual: EmpleadoDto? = null
    private var imagenBase64Seleccionada: String? = null

    private var puestos: List<CatalogoDto> = emptyList()
    private var areas: List<CatalogoDto> = emptyList()
    private var estados: List<CatalogoDto> = emptyList()
    private var usuariosDisponibles: List<UsuarioEmpleadoDto> = emptyList()
    private var opcionesUsuarioTexto: List<Pair<Int?, String>> = emptyList()

    private val selectorImagen = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            procesarImagenSeleccionada(uri)
        }
    }

    override fun getLayoutRes(): Int = R.layout.fragment_crear_editar_empleado

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        empleadoId = arguments?.getInt(ARG_EMPLEADO_ID, 0) ?: 0

        binding.tvTitle.text = if (empleadoId > 0) "Editar Empleado" else "Nuevo Empleado"
        binding.tvChangeAvatarHint.text = "Toca para seleccionar una foto"

        val abrirGaleria = View.OnClickListener { selectorImagen.launch("image/*") }
        binding.flChangeAvatar.setOnClickListener(abrirGaleria)
        binding.ivEmpleadoAvatar.setOnClickListener(abrirGaleria)

        binding.etFechaNacimiento.setOnClickListener {
            mostrarSelectorFecha(binding.etFechaNacimiento, 1998)
        }
        binding.etFechaIngreso.setOnClickListener {
            mostrarSelectorFecha(binding.etFechaIngreso, Calendar.getInstance().get(Calendar.YEAR))
        }

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnCancelar.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGuardar.setOnClickListener { guardar() }

        cargarDatos()
    }

    private fun mostrarSelectorFecha(campo: EditText, anioDefault: Int) {
        val cal = Calendar.getInstance()
        val textoActual = campo.text?.toString()?.trim().orEmpty()
        val partes = textoActual.split("/", "-")
        var anio = anioDefault
        var mes = cal.get(Calendar.MONTH)
        var dia = cal.get(Calendar.DAY_OF_MONTH)

        if (partes.size == 3) {
            if (partes[0].length == 4) {
                anio = partes[0].toIntOrNull() ?: anio
                mes = (partes[1].toIntOrNull() ?: (mes + 1)) - 1
                dia = partes[2].toIntOrNull() ?: dia
            } else {
                dia = partes[0].toIntOrNull() ?: dia
                mes = (partes[1].toIntOrNull() ?: (mes + 1)) - 1
                anio = partes[2].toIntOrNull() ?: anio
            }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                campo.setText(
                    String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                )
            },
            anio,
            mes,
            dia
        ).show()
    }

    private fun procesarImagenSeleccionada(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    val stream = requireContext().contentResolver.openInputStream(uri)
                        ?: return@withContext null
                    val original = stream.use { BitmapFactory.decodeStream(it) }
                        ?: return@withContext null
                    val maxLado = 720
                    val escala = minOf(
                        maxLado.toFloat() / original.width,
                        maxLado.toFloat() / original.height,
                        1f
                    )
                    val bitmapFinal = if (escala < 1f) {
                        Bitmap.createScaledBitmap(
                            original,
                            (original.width * escala).toInt().coerceAtLeast(1),
                            (original.height * escala).toInt().coerceAtLeast(1),
                            true
                        )
                    } else {
                        original
                    }
                    val baos = ByteArrayOutputStream()
                    bitmapFinal.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    Pair(bitmapFinal, "data:image/jpeg;base64,$base64")
                } catch (_: Exception) {
                    null
                }
            }

            if (!isAdded) return@launch
            if (resultado != null) {
                binding.ivEmpleadoAvatar.setImageBitmap(resultado.first)
                imagenBase64Seleccionada = resultado.second
                binding.tvChangeAvatarHint.text = "Foto lista para guardar"
            } else {
                aviso("No se pudo procesar la imagen seleccionada")
            }
        }
    }

    private fun cargarImagenRemota(ruta: String?) {
        val urlCompleta = RetrofitClient.resolverUrlImagen(ruta) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    URL(urlCompleta).openStream().use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) {
                    null
                }
            }
            if (isAdded && bitmap != null && imagenBase64Seleccionada == null) {
                binding.ivEmpleadoAvatar.setImageBitmap(bitmap)
            }
        }
    }

    private fun cargarDatos() {
        binding.btnGuardar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                puestos = RetrofitClient.empleadoService.obtenerPuestos().dataExitosa().orEmpty()
                areas = RetrofitClient.empleadoService.obtenerAreasTrabajo().dataExitosa().orEmpty()
                estados = RetrofitClient.empleadoService.obtenerEstadosEmpleado().dataExitosa().orEmpty()

                val todosUsuarios = try {
                    RetrofitClient.empleadoService.obtenerUsuarios().dataExitosa().orEmpty()
                } catch (_: Exception) {
                    emptyList()
                }
                val todosEmpleados = try {
                    RetrofitClient.empleadoService.obtenerEmpleados().dataExitosa().orEmpty()
                } catch (_: Exception) {
                    emptyList()
                }

                if (!isAdded) return@launch

                configurarDropdown(binding.actvRol, puestos)
                configurarDropdown(binding.actvTipoContrato, areas)
                configurarDropdown(binding.actvEstadoEmpleado, estados)

                if (empleadoId > 0) {
                    cargarEmpleado(todosUsuarios, todosEmpleados)
                } else {
                    configurarDropdownUsuarios(todosUsuarios, todosEmpleados, null)
                    binding.actvUsuarioSistema.setText(SIN_USUARIO, false)
                    binding.btnGuardar.isEnabled = true
                    val cal = Calendar.getInstance()
                    binding.etFechaIngreso.setText(
                        String.format(
                            Locale.US,
                            "%02d/%02d/%04d",
                            cal.get(Calendar.DAY_OF_MONTH),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.YEAR)
                        )
                    )
                }
            } catch (_: Exception) {
                if (isAdded) {
                    aviso("No se pudieron cargar los catálogos de empleados")
                    binding.btnGuardar.isEnabled = true
                }
            }
        }
    }

    private fun configurarDropdownUsuarios(
        todosUsuarios: List<UsuarioEmpleadoDto>,
        todosEmpleados: List<EmpleadoDto>,
        usuarioActualId: Int?
    ) {
        val ocupadosPorOtros = todosEmpleados
            .filter { it.id != empleadoId }
            .mapNotNull { it.usuario?.id ?: it.usuarioId }
            .toSet()

        usuariosDisponibles = todosUsuarios.filter { u ->
            u.id == usuarioActualId || !ocupadosPorOtros.contains(u.id)
        }

        val lista = mutableListOf<Pair<Int?, String>>(null to SIN_USUARIO)
        usuariosDisponibles.forEach { u ->
            val etiqueta = if (!u.email.isNullOrBlank()) {
                "${u.nombre ?: "Usuario #${u.id}"} (${u.email})"
            } else {
                u.nombre ?: "Usuario #${u.id}"
            }
            lista.add(u.id to etiqueta)
        }
        if (usuarioActualId != null && lista.none { it.first == usuarioActualId }) {
            val uActual = empleadoActual?.usuario
            val etiqueta = if (uActual != null) {
                "${uActual.nombre ?: "Usuario #$usuarioActualId"} (${uActual.email.orEmpty()})"
            } else {
                "Usuario #$usuarioActualId"
            }
            lista.add(usuarioActualId to etiqueta)
        }

        opcionesUsuarioTexto = lista
        binding.actvUsuarioSistema.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                lista.map { it.second }
            )
        )
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

    private suspend fun cargarEmpleado(
        todosUsuarios: List<UsuarioEmpleadoDto>,
        todosEmpleados: List<EmpleadoDto>
    ) {
        try {
            val empleado = RetrofitClient.empleadoService.obtenerEmpleado(empleadoId).dataExitosa()
            if (!isAdded) return
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
            binding.etDireccion.setText(empleado.calle.orEmpty())
            binding.etNumeroExterior.setText(empleado.numeroExterior.orEmpty())
            binding.etNumeroInterior.setText(empleado.numeroInterior.orEmpty())
            binding.etColonia.setText(empleado.colonia.orEmpty())
            binding.etCodigoPostal.setText(empleado.codigoPostal.orEmpty())
            binding.etMunicipio.setText(empleado.municipio.orEmpty())
            binding.etEstado.setText(empleado.estado.orEmpty())
            binding.etEntreCalles.setText(empleado.entreCalles.orEmpty())
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

            val usuarioActualId = empleado.usuario?.id ?: empleado.usuarioId
            configurarDropdownUsuarios(todosUsuarios, todosEmpleados, usuarioActualId)
            val textoUsuario = opcionesUsuarioTexto
                .firstOrNull { it.first == usuarioActualId }
                ?.second
                ?: SIN_USUARIO
            binding.actvUsuarioSistema.setText(textoUsuario, false)

            cargarImagenRemota(empleado.imagenUrl)
            binding.btnGuardar.isEnabled = true
        } catch (_: Exception) {
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            }
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
            fechaNacimiento == null -> return aviso("Selecciona una fecha de nacimiento válida")
            fechaIngreso == null -> return aviso("Selecciona una fecha de ingreso válida")
            puesto == null -> return aviso("Selecciona un puesto")
            area == null -> return aviso("Selecciona un área de trabajo")
            estado == null -> return aviso("Selecciona un estado de empleado")
        }

        val textoUsuarioSel = binding.actvUsuarioSistema.text?.toString()?.trim().orEmpty()
        val usuarioIdSeleccionado = opcionesUsuarioTexto
            .firstOrNull { it.second == textoUsuarioSel }
            ?.first

        val partesApellido = apellidos.split(" ", limit = 2)
        val solicitud = EmpleadoRequest(
            puestoId = puesto.id,
            areaTrabajoId = area.id,
            estadoEmpleadoId = estado.id,
            usuarioId = usuarioIdSeleccionado,
            nombre = nombre,
            apellidoPaterno = partesApellido.first(),
            apellidoMaterno = partesApellido.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() },
            telefono = telefono,
            correo = binding.etEmail.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            calle = binding.etDireccion.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            numeroExterior = binding.etNumeroExterior.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            numeroInterior = binding.etNumeroInterior.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            colonia = binding.etColonia.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            codigoPostal = binding.etCodigoPostal.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            municipio = binding.etMunicipio.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            estado = binding.etEstado.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            entreCalles = binding.etEntreCalles.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            fechaNacimiento = fechaNacimiento!!,
            fechaIngreso = fechaIngreso!!,
            imagenUrl = imagenBase64Seleccionada ?: empleadoActual?.imagenUrl
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
                if (!isAdded) return@launch
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
                if (isAdded) {
                    aviso("No se pudo conectar con el servidor")
                }
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

    companion object {
        private const val ARG_EMPLEADO_ID = "arg_empleado_id"
        private const val SIN_USUARIO = "Sin usuario vinculado"

        fun newInstance(empleadoId: Int = 0): CrearEditarEmpleadoFragment {
            return CrearEditarEmpleadoFragment().apply {
                arguments = Bundle().apply { putInt(ARG_EMPLEADO_ID, empleadoId) }
            }
        }
    }
}

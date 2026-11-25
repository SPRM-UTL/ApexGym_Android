package com.sprm.apexgym.feature.empleados

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentEmpleadoDetalleBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class EmpleadoDetalleFragment : BaseFragment<FragmentEmpleadoDetalleBinding>() {

    private var empleadoId: Int = 0
    private var telefonoActual: String? = null
    private var nombreActual: String = "este empleado"

    override fun getLayoutRes(): Int = R.layout.fragment_empleado_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        empleadoId = arguments?.getInt(ARG_EMPLEADO_ID, 0) ?: 0

        setupClickListeners()
        cargarEmpleado()
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        val irAEditar = View.OnClickListener {
            if (empleadoId > 0) {
                navegar(CrearEditarEmpleadoFragment.newInstance(empleadoId))
            }
        }
        binding.flEdit.setOnClickListener(irAEditar)
        binding.btnVerTurnos.setOnClickListener(irAEditar)

        binding.btnVerAsistencias.setOnClickListener {
            val tel = telefonoActual?.trim()
            if (tel.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Este empleado no tiene teléfono registrado", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
            }
        }

        binding.btnGestionarPermisos.setOnClickListener {
            confirmarEliminacion()
        }
    }

    private fun cargarEmpleado() {
        if (empleadoId <= 0) {
            Toast.makeText(requireContext(), "Empleado no válido", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerEmpleado(empleadoId)
                val empleado = response.dataExitosa()

                if (!isAdded) return@launch

                if (empleado == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo cargar el empleado",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                nombreActual = empleado.nombreCompleto.ifBlank { "Empleado #${empleado.id}" }
                telefonoActual = empleado.telefono

                binding.tvEmpleadoNombre.text = nombreActual
                binding.tvEmpleadoRol.text = listOfNotNull(
                    empleado.puesto?.nombre,
                    empleado.areaTrabajo?.nombre
                ).joinToString(" • ").ifBlank { "Puesto no asignado" }
                binding.tvEmpleadoEstado.text =
                    empleado.estadoEmpleado?.nombre ?: "Activo"

                binding.tvEmpleadoTelefono.text = empleado.telefono ?: "—"
                binding.tvEmpleadoEmail.text =
                    empleado.correo ?: empleado.usuario?.email ?: "—"
                binding.tvEmpleadoDireccion.text = formatearDireccion(empleado)
                binding.tvEmpleadoDni.text = "EMP-${empleado.id.toString().padStart(4, '0')}"
                binding.tvEmpleadoFechaIngreso.text = formatearFecha(
                    empleado.fechaIngreso,
                    "dd/MM/yyyy"
                )
                binding.tvEmpleadoTipoContrato.text =
                    empleado.areaTrabajo?.nombre ?: "Sin área asignada"

                val salario = empleado.puesto?.salarioBase
                binding.tvEmpleadoSueldo.text = if (salario != null && salario > 0) {
                    "${salario.formatearMonedaSinSufijo()} MXN"
                } else {
                    "—"
                }

                val urlFoto = RetrofitClient.resolverUrlImagen(empleado.imagenUrl)
                if (urlFoto != null) {
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            URL(urlFoto).openStream().use { BitmapFactory.decodeStream(it) }
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (isAdded && bitmap != null) {
                        binding.ivEmpleadoAvatar.imageTintList = null
                        binding.ivEmpleadoAvatar.setImageBitmap(bitmap)
                    }
                }
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
    }

    private fun confirmarEliminacion() {
        if (empleadoId <= 0) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar empleado")
            .setMessage("¿Deseas eliminar a $nombreActual? Esta acción lo dará de baja del listado activo.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                ejecutarEliminacion()
            }
            .show()
    }

    private fun ejecutarEliminacion() {
        binding.btnGestionarPermisos.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.empleadoService.eliminarEmpleado(empleadoId)
                if (!isAdded) return@launch
                if (res.isSuccessful && res.body()?.responseFlag == 0) {
                    Toast.makeText(requireContext(), "Empleado eliminado correctamente", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(
                        requireContext(),
                        res.mensajeDeError() ?: "No se pudo eliminar el empleado",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnGestionarPermisos.isEnabled = true
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error de conexión al eliminar", Toast.LENGTH_LONG).show()
                    binding.btnGestionarPermisos.isEnabled = true
                }
            }
        }
    }

    private fun formatearDireccion(empleado: com.sprm.apexgym.core.network.api.EmpleadoDto): String {
        val calle = empleado.calle?.trim().orEmpty()
        val numExt = empleado.numeroExterior?.trim().orEmpty()
        val numInt = empleado.numeroInterior?.trim().orEmpty()
        val colonia = empleado.colonia?.trim().orEmpty()
        val cp = empleado.codigoPostal?.trim().orEmpty()
        val municipio = empleado.municipio?.trim().orEmpty()
        val estado = empleado.estado?.trim().orEmpty()
        val entreCalles = empleado.entreCalles?.trim().orEmpty()

        val lineaCalle = buildString {
            if (calle.isNotEmpty()) append(calle)
            if (numExt.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append("#$numExt")
            }
            if (numInt.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append("Int. $numInt")
            }
        }

        val partes = buildList {
            if (lineaCalle.isNotEmpty()) add(lineaCalle)
            if (colonia.isNotEmpty()) add("Col. $colonia")
            if (cp.isNotEmpty()) add("C.P. $cp")
            if (municipio.isNotEmpty()) add(municipio)
            if (estado.isNotEmpty()) add(estado)
        }.joinToString(", ")

        val completo = if (entreCalles.isNotEmpty()) {
            if (partes.isNotEmpty()) "$partes ($entreCalles)" else entreCalles
        } else {
            partes
        }
        return completo.ifBlank { "—" }
    }

    private fun navegar(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val ARG_EMPLEADO_ID = "arg_empleado_id"

        fun newInstance(empleadoId: Int): EmpleadoDetalleFragment {
            return EmpleadoDetalleFragment().apply {
                arguments = Bundle().apply { putInt(ARG_EMPLEADO_ID, empleadoId) }
            }
        }
    }
}

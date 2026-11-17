package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentEmpleadoDetalleBinding
import kotlinx.coroutines.launch

class EmpleadoDetalleFragment : BaseFragment<FragmentEmpleadoDetalleBinding>() {

    private var empleadoId: Int = 0

    override fun getLayoutRes(): Int = R.layout.fragment_empleado_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        empleadoId = arguments?.getInt(ARG_EMPLEADO_ID, 0) ?: 0

        binding.rvHorarioTrabajo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCertificaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHorarioTrabajo.visibility = View.GONE
        binding.rvCertificaciones.visibility = View.GONE
        binding.btnAgregarCertificacion.visibility = View.GONE

        setupClickListeners()
        cargarEmpleado()
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.flEdit.setOnClickListener {
            if (empleadoId > 0) {
                navegar(CrearEditarEmpleadoFragment.newInstance(empleadoId))
            }
        }

        val noDisponible = View.OnClickListener {
            Toast.makeText(
                requireContext(),
                "Este módulo no tiene endpoint disponible en la API actual.",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.btnVerAsistencias.setOnClickListener(noDisponible)
        binding.btnVerTurnos.setOnClickListener(noDisponible)
        binding.btnGestionarPermisos.setOnClickListener(noDisponible)
        binding.tvEmpleadoSueldo.setOnClickListener(noDisponible)
        binding.rvHorarioTrabajo.setOnClickListener(noDisponible)
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

                if (empleado == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo cargar el empleado",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                binding.tvEmpleadoNombre.text = empleado.nombreCompleto.ifBlank {
                    "Empleado #${empleado.id}"
                }
                binding.tvEmpleadoRol.text = empleado.puesto?.nombre ?: "Puesto no asignado"
                binding.tvEmpleadoEstado.text =
                    empleado.estadoEmpleado?.nombre ?: "Estado no disponible"

                binding.tvTurnosMes.text = "—"
                binding.tvVentasTotales.text = "—"
                binding.tvComisiones.text = "—"

                binding.tvEmpleadoTelefono.text = empleado.telefono ?: "—"
                binding.tvEmpleadoEmail.text =
                    empleado.correo ?: empleado.usuario?.email ?: "—"
                binding.tvEmpleadoDireccion.text = empleado.direccion ?: "—"
                binding.tvEmpleadoDni.text = "No capturado en la API"
                binding.tvEmpleadoFechaIngreso.text = formatearFecha(
                    empleado.fechaIngreso,
                    "dd/MM/yyyy"
                )
                binding.tvEmpleadoTipoContrato.text = "No disponible en la API"
                binding.tvEmpleadoSueldo.text = "No disponible en la API"
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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

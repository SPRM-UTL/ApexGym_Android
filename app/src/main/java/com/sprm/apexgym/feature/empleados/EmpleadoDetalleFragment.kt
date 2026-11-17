package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentEmpleadoDetalleBinding

class EmpleadoDetalleFragment : BaseFragment<FragmentEmpleadoDetalleBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_empleado_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerViews()
    }

    private fun setupViews() {
        binding.tvEmpleadoNombre.text = "Carlos Rodríguez"
        binding.tvEmpleadoRol.text = "Entrenador Personal"
        binding.tvEmpleadoEstado.text = "ACTIVO"
        binding.tvTurnosMes.text = "18"
        binding.tvEmpleadoSueldo.text = "$18,500.00 MXN"
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

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.flEdit.setOnClickListener {
            navegar(CrearEditarEmpleadoFragment())
        }

        binding.btnVerAsistencias.setOnClickListener {
            navegar(AsistenciasEmpleadoFragment())
        }

        binding.btnVerTurnos.setOnClickListener {
            navegar(TurnosEmpleadoFragment())
        }

        binding.btnGestionarPermisos.setOnClickListener {
            navegar(PermisosEmpleadoFragment())
        }

        binding.rvHorarioTrabajo.setOnClickListener {
            navegar(HorarioEmpleadoFragment())
        }

        binding.tvEmpleadoSueldo.setOnClickListener {
            navegar(NominaEmpleadoFragment())
        }

        binding.btnAgregarCertificacion.setOnClickListener {
            Toast.makeText(requireContext(), "Nueva certificación agregada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        binding.rvHorarioTrabajo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCertificaciones.layoutManager = LinearLayoutManager(requireContext())
    }
}

package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
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
        // TODO: Load empleado data and populate views
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flEdit.setOnClickListener {
            // TODO: Navigate to edit with empleado ID
        }
        binding.btnVerAsistencias.setOnClickListener {
            // TODO: Navigate to asistencias
        }
        binding.btnVerTurnos.setOnClickListener {
            // TODO: Navigate to turnos
        }
        binding.btnGestionarPermisos.setOnClickListener {
            // TODO: Navigate to permisos
        }
    }

    private fun setupRecyclerViews() {
        binding.rvHorarioTrabajo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCertificaciones.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapters
    }
}

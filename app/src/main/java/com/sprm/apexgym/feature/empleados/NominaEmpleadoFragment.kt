package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentNominaEmpleadoBinding

class NominaEmpleadoFragment : BaseFragment<FragmentNominaEmpleadoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_nomina_empleado

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerViews()
    }

    private fun setupViews() {
        // TODO: Load empleado data and nomina summary
        // Setup period chips
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flFilter.setOnClickListener {
            // TODO: Show filter dialog
        }
        binding.tvVerTodoHistorial.setOnClickListener {
            // TODO: Navigate to full historial
        }
        binding.btnGenerarBoleta.setOnClickListener {
            // TODO: Generate boleta PDF
        }
        binding.btnExportarNomina.setOnClickListener {
            // TODO: Export full nomina PDF
        }
    }

    private fun setupRecyclerViews() {
        binding.rvDetalleComisiones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDeducciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorialPagos.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapters
    }
}

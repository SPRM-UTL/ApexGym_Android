package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
        // Datos de nómina
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flFilter.setOnClickListener {
            Toast.makeText(requireContext(), "Filtrar período de nómina", Toast.LENGTH_SHORT).show()
        }
        binding.tvVerTodoHistorial.setOnClickListener {
            Toast.makeText(requireContext(), "Historial completo de pagos", Toast.LENGTH_SHORT).show()
        }
        binding.btnGenerarBoleta.setOnClickListener {
            Toast.makeText(requireContext(), "Boleta generada en PDF", Toast.LENGTH_SHORT).show()
        }
        binding.btnExportarNomina.setOnClickListener {
            Toast.makeText(requireContext(), "Reporte de nómina exportado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        binding.rvDetalleComisiones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDeducciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorialPagos.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapters
    }
}

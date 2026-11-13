package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentAsistenciasEmpleadoBinding

class AsistenciasEmpleadoFragment : BaseFragment<FragmentAsistenciasEmpleadoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_asistencias_empleado

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerView()
    }

    private fun setupViews() {
        // Datos de asistencias
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flFilter.setOnClickListener {
            Toast.makeText(requireContext(), "Filtros de asistencia", Toast.LENGTH_SHORT).show()
        }
        binding.etSearchAsistencias.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                Toast.makeText(requireContext(), "Búsqueda de asistencias", Toast.LENGTH_SHORT).show()
                true
            } else false
        }
    }

    private fun setupRecyclerView() {
        binding.rvAsistenciasEmpleado.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapter
    }
}

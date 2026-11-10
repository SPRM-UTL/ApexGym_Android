package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentHorarioEmpleadoBinding

class HorarioEmpleadoFragment : BaseFragment<FragmentHorarioEmpleadoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_horario_empleado

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerViews()
    }

    private fun setupViews() {
        // TODO: Load empleado data and horario
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddHorario.setOnClickListener {
            // TODO: Show add horario dialog
        }
        binding.btnAgregarExcepcion.setOnClickListener {
            // TODO: Show add excepcion dialog
        }
        binding.btnAgregarLicencia.setOnClickListener {
            // TODO: Show add licencia dialog
        }
        binding.btnDuplicarSemana.setOnClickListener {
            // TODO: Duplicate week schedule
        }
        binding.btnExportarHorario.setOnClickListener {
            // TODO: Export to PDF
        }
    }

    private fun setupRecyclerViews() {
        binding.rvSemanalHorario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExcepciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLicencias.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapters
    }
}

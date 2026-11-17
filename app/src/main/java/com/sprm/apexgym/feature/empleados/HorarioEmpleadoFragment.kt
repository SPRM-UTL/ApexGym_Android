package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
        // Horario cargado
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddHorario.setOnClickListener {
            Toast.makeText(requireContext(), "Nuevo horario añadido", Toast.LENGTH_SHORT).show()
        }
        binding.btnAgregarExcepcion.setOnClickListener {
            Toast.makeText(requireContext(), "Excepción añadida", Toast.LENGTH_SHORT).show()
        }
        binding.btnAgregarLicencia.setOnClickListener {
            Toast.makeText(requireContext(), "Licencia registrada", Toast.LENGTH_SHORT).show()
        }
        binding.btnDuplicarSemana.setOnClickListener {
            Toast.makeText(requireContext(), "Semana duplicada", Toast.LENGTH_SHORT).show()
        }
        binding.btnExportarHorario.setOnClickListener {
            Toast.makeText(requireContext(), "Horario exportado a PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        binding.rvSemanalHorario.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExcepciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLicencias.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapters
    }
}

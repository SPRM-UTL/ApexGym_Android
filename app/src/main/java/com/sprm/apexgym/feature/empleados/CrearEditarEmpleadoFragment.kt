package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentCrearEditarEmpleadoBinding

class CrearEditarEmpleadoFragment : BaseFragment<FragmentCrearEditarEmpleadoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_crear_editar_empleado

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupAutoCompleteViews()
        setupRecyclerView()
    }

    private fun setupViews() {
        // Título y datos iniciales
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flChangeAvatar.setOnClickListener {
            Toast.makeText(requireContext(), "Seleccionar foto de empleado", Toast.LENGTH_SHORT).show()
        }
        binding.btnCancelar.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGuardar.setOnClickListener {
            Toast.makeText(requireContext(), "Empleado guardado exitosamente", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        binding.btnAgregarDiaHorario.setOnClickListener {
            Toast.makeText(requireContext(), "Día de horario añadido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAutoCompleteViews() {
        // TODO: Setup dropdown adapters for roles, contract types, etc.
        // binding.actvRol.setAdapter(...)
        // binding.actvTipoContrato.setAdapter(...)
    }

    private fun setupRecyclerView() {
        binding.rvHorarioConfig.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapter for schedule config
    }
}

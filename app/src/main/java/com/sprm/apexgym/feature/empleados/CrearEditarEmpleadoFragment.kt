package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
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
        // TODO: Check if editing existing empleado, load data
        // binding.tvTitle.text = if (isEditing) "Editar Empleado" else "Nuevo Empleado"
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flChangeAvatar.setOnClickListener {
            // TODO: Open image picker
        }
        binding.btnCancelar.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGuardar.setOnClickListener {
            // TODO: Validate and save
        }
        binding.btnAgregarDiaHorario.setOnClickListener {
            // TODO: Add day to schedule recycler
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

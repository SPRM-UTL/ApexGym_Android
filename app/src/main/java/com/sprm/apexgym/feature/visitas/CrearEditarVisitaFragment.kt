package com.sprm.apexgym.feature.visitas

import android.os.Bundle
import android.view.View
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentCrearEditarVisitaBinding

class CrearEditarVisitaFragment : BaseFragment<FragmentCrearEditarVisitaBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_crear_editar_visita

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupAutoCompleteViews()
    }

    private fun setupViews() {
        // TODO: Check if editing existing visita, load data
        // binding.tvTitle.text = if (isEditing) "Editar Visita" else "Nueva Visita"
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnCancelar.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGuardar.setOnClickListener {
            // TODO: Validate and save
        }
        binding.btnUsarUbicacionActual.setOnClickListener {
            // TODO: Get current location
        }
    }

    private fun setupAutoCompleteViews() {
        // TODO: Setup dropdown adapters for origen, estado, nivelInteres
        // binding.actvOrigen.setAdapter(...)
        // binding.actvEstado.setAdapter(...)
        // binding.actvNivelInteres.setAdapter(...)
    }
}

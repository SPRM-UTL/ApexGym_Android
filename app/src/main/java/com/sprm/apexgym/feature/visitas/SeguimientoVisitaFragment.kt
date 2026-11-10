package com.sprm.apexgym.feature.visitas

import android.os.Bundle
import android.view.View
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentSeguimientoVisitaBinding

class SeguimientoVisitaFragment : BaseFragment<FragmentSeguimientoVisitaBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_seguimiento_visita

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupListeners()
    }

    private fun setupViews() {
        // TODO: Load prospect data
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnCancelar.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGuardarSeguimiento.setOnClickListener {
            // TODO: Validate and save seguimiento
        }
    }

    private fun setupListeners() {
        binding.cbRequiereAccion.setOnCheckedChangeListener { _, isChecked ->
            binding.tilProximaAccion.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.cbProgramarProximo.setOnCheckedChangeListener { _, isChecked ->
            binding.llProximoSeguimiento.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        binding.cbAhora.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // TODO: Set current date/time
            }
        }
    }
}

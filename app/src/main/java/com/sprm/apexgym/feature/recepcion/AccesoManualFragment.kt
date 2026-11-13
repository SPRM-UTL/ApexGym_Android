package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentAccesoManualBinding

class AccesoManualFragment : BaseFragment<FragmentAccesoManualBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_acceso_manual

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Initial hide
        binding.cvClientFound.visibility = View.GONE
        binding.cvClientNotFound.visibility = View.GONE

        binding.btnBuscar.setOnClickListener {
            val query = binding.etManualSearch.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa nombre, DNI o código", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Mock result always found
            binding.cvClientFound.visibility = View.VISIBLE
            binding.cvClientNotFound.visibility = View.GONE
            binding.tvClientName.text = "Carlos Ramírez"
            binding.tvClientCode.text = "Código: APX-2024-0891"
            binding.tvClientPlan.text = "Plan Premium Anual"
            binding.tvClientExpiry.text = "30/12/2026"
            binding.tvClientDaysLeft.text = "23"
        }

        binding.btnGrantAccess.setOnClickListener {
            val dialog = AccesoResultadoDialogFragment.newInstance(true)
            dialog.show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
        }

        binding.btnDenyAccess.setOnClickListener {
            val dialog = AccesoResultadoDialogFragment.newInstance(false)
            dialog.show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
        }

        binding.btnNuevoCliente.setOnClickListener {
            Toast.makeText(requireContext(), "Registrar nuevo cliente — próximamente", Toast.LENGTH_SHORT).show()
        }
    }
}

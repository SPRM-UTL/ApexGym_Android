package com.sprm.apexgym.feature.visitas

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentVisitaDetalleBinding

class VisitaDetalleFragment : BaseFragment<FragmentVisitaDetalleBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_visita_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerView()
    }

    private fun setupViews() {
        // TODO: Load visita data and populate views
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flEdit.setOnClickListener {
            // TODO: Navigate to edit
        }
        binding.btnNuevoSeguimiento.setOnClickListener {
            // TODO: Navigate to seguimiento
        }
        binding.btnLlamar.setOnClickListener {
            // TODO: Make phone call
        }
        binding.btnWhatsApp.setOnClickListener {
            // TODO: Open WhatsApp
        }
        binding.btnConvertirCliente.setOnClickListener {
            // TODO: Convert to client flow
        }
    }

    private fun setupRecyclerView() {
        binding.rvSeguimientos.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapter
    }
}

package com.sprm.apexgym.feature.visitas

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
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
        // Datos de prospecto
    }

    private fun navegar(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flEdit.setOnClickListener {
            navegar(CrearEditarVisitaFragment())
        }
        binding.btnNuevoSeguimiento.setOnClickListener {
            navegar(SeguimientoVisitaFragment())
        }
        binding.btnLlamar.setOnClickListener {
            Toast.makeText(requireContext(), "Iniciando llamada al prospecto...", Toast.LENGTH_SHORT).show()
        }
        binding.btnWhatsApp.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo chat de WhatsApp...", Toast.LENGTH_SHORT).show()
        }
        binding.btnConvertirCliente.setOnClickListener {
            Toast.makeText(requireContext(), "Prospecto convertido a nuevo cliente con éxito", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.rvSeguimientos.layoutManager = LinearLayoutManager(requireContext())
    }
}

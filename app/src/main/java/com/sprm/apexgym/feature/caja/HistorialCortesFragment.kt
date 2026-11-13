package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentHistorialCortesBinding

class HistorialCortesFragment : BaseFragment<FragmentHistorialCortesBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_historial_cortes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.flFilter.setOnClickListener {
            Toast.makeText(requireContext(), "Filtro de cortes", Toast.LENGTH_SHORT).show()
        }

        binding.etSearchCortes.setOnEditorActionListener { _, _, _ ->
            Toast.makeText(requireContext(), "Buscando: ${binding.etSearchCortes.text}", Toast.LENGTH_SHORT).show()
            true
        }
    }
}

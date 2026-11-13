package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentConfiguracionCajaBinding

class ConfiguracionCajaFragment : BaseFragment<FragmentConfiguracionCajaBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_configuracion_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnAgregarCaja.setOnClickListener {
            Toast.makeText(requireContext(), "Agregar nueva caja — próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnAgregarDenominacion.setOnClickListener {
            Toast.makeText(requireContext(), "Agregar denominación — próximamente", Toast.LENGTH_SHORT).show()
        }

        binding.btnGuardarConfigCaja.setOnClickListener {
            Toast.makeText(requireContext(), "Configuración guardada correctamente", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}

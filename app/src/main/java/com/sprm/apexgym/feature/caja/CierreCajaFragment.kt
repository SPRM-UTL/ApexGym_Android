package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentCierreCajaBinding

class CierreCajaFragment : BaseFragment<FragmentCierreCajaBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_cierre_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvCajaNombre.text = "Caja Principal - Turno Mañana"
        binding.tvFondoInicial.text = "S/ 500.00"
        binding.tvHoraApertura.text = "08:00 AM"
        binding.tvVentasTotales.text = "S/ 2,450.00"
        binding.tvEfectivoEsperado.text = "S/ 2,950.00"

        binding.btnCerrarCaja.setOnClickListener {
            Toast.makeText(requireContext(), "Corte generado y caja cerrada exitosamente", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            parentFragmentManager.popBackStack()
        }
    }
}

package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.View
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentMovimientosCajaBinding

class MovimientosCajaFragment : BaseFragment<FragmentMovimientosCajaBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_movimientos_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvCajaNombre.text = "Caja Principal - Turno Mañana"
        binding.tvAperturaInfo.text = "Abierta: 08:00 AM • Fondo: S/ 500.00"
        binding.tvSaldoActual.text = "S/ 2,950.00"

        binding.flAddMovement.setOnClickListener {
            val sheet = NuevoMovimientoSheetFragment()
            sheet.show(parentFragmentManager, NuevoMovimientoSheetFragment.TAG)
        }
    }
}

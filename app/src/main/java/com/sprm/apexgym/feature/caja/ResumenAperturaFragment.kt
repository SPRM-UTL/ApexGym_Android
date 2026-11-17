package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentResumenAperturaBinding

class ResumenAperturaFragment : BaseFragment<FragmentResumenAperturaBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_resumen_apertura

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.flRefresh.setOnClickListener {
            Toast.makeText(requireContext(), "Actualizando datos de caja...", Toast.LENGTH_SHORT).show()
        }

        binding.tvResumenCajaNombre.text = "Caja Principal - Turno Mañana"
        binding.tvResumenAperturaInfo.text = "Abierta: 08:00 AM • Fondo: S/ 500.00"
        binding.tvResumenSaldoActual.text = "S/ 2,950.00"
        binding.tvResumenVentasHoy.text = "S/ 2,450.00"
        binding.tvResumenTransacciones.text = "47"
        binding.tvResumenEfectivoEsperado.text = "S/ 2,950.00"
        binding.tvAlertCount.text = "1"

        binding.tvVerTodosMovimientos.setOnClickListener {
            navegar(MovimientosCajaFragment())
        }

        binding.btnAccionAbrirCaja.setOnClickListener {
            navegar(AperturaCajaFragment())
        }

        binding.btnAccionCorteCaja.setOnClickListener {
            navegar(CorteCajaFragment())
        }

        binding.btnAccionHistorialCortes.setOnClickListener {
            navegar(HistorialCortesFragment())
        }

        binding.btnAccionConfigCaja.setOnClickListener {
            navegar(ConfiguracionCajaFragment())
        }
    }

    private fun navegar(fragment: androidx.fragment.app.Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

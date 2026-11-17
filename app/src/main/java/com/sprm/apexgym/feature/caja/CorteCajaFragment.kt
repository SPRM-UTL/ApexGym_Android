package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentCorteCajaBinding

class CorteCajaFragment : BaseFragment<FragmentCorteCajaBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_corte_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvCorteCajaNombre.text = "Caja Principal - Turno Mañana"
        binding.tvCorteEstado.text = "PENDIENTE"
        binding.tvCorteFecha.text = "05/11/2026"
        binding.tvCorteHora.text = "14:30"
        binding.tvCorteResponsable.text = "María González"
        binding.tvCorteVentasTotales.text = "S/ 2,450.00"
        binding.tvCorteNumTransacciones.text = "47 transacciones"
        binding.tvCorteEfectivoCaja.text = "S/ 2,950.00"
        binding.tvCorteDiferencia.text = "Diferencia: S/ 0.00"

        binding.btnGuardarBorrador.setOnClickListener {
            Toast.makeText(requireContext(), "Borrador de corte guardado", Toast.LENGTH_SHORT).show()
        }

        binding.btnFinalizarCorte.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, CierreCajaFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}

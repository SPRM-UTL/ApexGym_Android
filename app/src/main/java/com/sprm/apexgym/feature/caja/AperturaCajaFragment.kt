package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentAperturaCajaBinding

class AperturaCajaFragment : BaseFragment<FragmentAperturaCajaBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_apertura_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvTotalCalculado.text = "S/ 500.00"
        binding.tvDiferenciaFondo.text = "Diferencia con fondo inicial: S/ 0.00"

        binding.btnAbrirCaja.setOnClickListener {
            val fondo = binding.etFondoInicial.text.toString().trim()
            if (fondo.isEmpty()) {
                binding.etFondoInicial.setText("500.00")
            }
            Toast.makeText(requireContext(), "Caja abierta exitosamente", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, ResumenAperturaFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}

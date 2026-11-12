package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.View
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentHistorialAsistenciasBinding

class HistorialAsistenciasFragment : BaseFragment<FragmentHistorialAsistenciasBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_historial_asistencias

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvTotalAccesos.text = "24"
        binding.tvEsteMes.text = "18"
        binding.tvRachaActual.text = "5"
    }
}

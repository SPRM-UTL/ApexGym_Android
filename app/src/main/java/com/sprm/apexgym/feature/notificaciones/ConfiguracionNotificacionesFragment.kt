package com.sprm.apexgym.feature.notificaciones

import android.os.Bundle
import android.view.View
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentConfiguracionNotificacionesBinding

class ConfiguracionNotificacionesFragment : BaseFragment<FragmentConfiguracionNotificacionesBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_configuracion_notificaciones

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        // TODO: Load current settings and populate switches
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        // Master toggle
        binding.swMasterPush.setOnCheckedChangeListener { _, isChecked ->
            setCategoriesEnabled(isChecked)
        }

        // Category switches
        binding.swNotifAsistencias.setOnCheckedChangeListener { _, _ ->
            // TODO: Save setting
        }
        binding.swNotifMembresias.setOnCheckedChangeListener { _, _ ->
            // TODO: Save setting
        }
        binding.swNotifPOS.setOnCheckedChangeListener { _, _ ->
            // TODO: Save setting
        }
        binding.swNotifCaja.setOnCheckedChangeListener { _, _ ->
            // TODO: Save setting
        }
        binding.swNotifSistema.setOnCheckedChangeListener { _, _ ->
            // TODO: Save setting
        }

        // Sound & Vibration
        binding.swNotifSonido.setOnCheckedChangeListener { _, _ ->
            // TODO: Save setting
        }
        binding.swNotifVibracion.setOnCheckedChangeListener { _, _ ->
            // TODO: Save setting
        }

        // Tono selector
        binding.tvTonoSeleccionado.setOnClickListener {
            // TODO: Open ringtone picker
        }

        // Quiet hours
        binding.swNoMolestar.setOnCheckedChangeListener { _, _ ->
            // TODO: Save setting
        }
    }

    private fun setCategoriesEnabled(enabled: Boolean) {
        binding.swNotifAsistencias.isEnabled = enabled
        binding.swNotifMembresias.isEnabled = enabled
        binding.swNotifPOS.isEnabled = enabled
        binding.swNotifCaja.isEnabled = enabled
        binding.swNotifSistema.isEnabled = enabled
        binding.swNotifSonido.isEnabled = enabled
        binding.swNotifVibracion.isEnabled = enabled
    }
}

package com.sprm.apexgym.feature.notificaciones

import android.os.Bundle
import android.view.View
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentNotificacionDetalleBinding

class NotificacionDetalleFragment : BaseFragment<FragmentNotificacionDetalleBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_notificacion_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        // TODO: Load notificacion data and populate views
        // Show/hide action buttons based on notificacion type
        // Show/hide related entity card based on data
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flShare.setOnClickListener {
            // TODO: Share notification
        }
        binding.flDelete.setOnClickListener {
            // TODO: Delete notification
        }
        binding.btnNotifActionPrimary.setOnClickListener {
            // TODO: Handle primary action (navigate to related entity)
        }
        binding.btnNotifActionSecondary.setOnClickListener {
            // TODO: Dismiss
        }
        binding.btnMarcarLeida.setOnClickListener {
            // TODO: Mark as read
        }
        binding.btnMarcarNoLeida.setOnClickListener {
            // TODO: Mark as unread
        }
        binding.btnResponder.setOnClickListener {
            // TODO: Reply to notification
        }
        binding.btnArchivar.setOnClickListener {
            // TODO: Archive notification
        }
    }
}

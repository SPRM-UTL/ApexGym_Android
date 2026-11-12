package com.sprm.apexgym.feature.notificaciones

import android.os.Bundle
import android.view.View
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentNotificacionesListBinding

class NotificacionesListFragment : BaseFragment<FragmentNotificacionesListBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_notificaciones_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupViewPager()
    }

    private fun setupViews() {
        // TODO: Load unread count and show/hide badge
        // Setup tabs: Todas, No Leídas, Archivadas, etc.
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flConfig.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ConfiguracionNotificacionesFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.btnMarcarTodasLeidas.setOnClickListener {
            // TODO: Mark all as read
        }
        binding.etSearchNotificaciones.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // TODO: Perform search
                true
            } else false
        }
    }

    private fun setupViewPager() {
        // TODO: Setup ViewPager2 with FragmentStateAdapter for tabs
        // binding.tabLayoutNotificaciones.addTab(...)
    }
}

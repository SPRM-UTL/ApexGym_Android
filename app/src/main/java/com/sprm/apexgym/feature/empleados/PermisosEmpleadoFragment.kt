package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentPermisosEmpleadoBinding

class PermisosEmpleadoFragment : BaseFragment<FragmentPermisosEmpleadoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_permisos_empleado

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupViewPager()
    }

    private fun setupViews() {
        // TODO: Load empleado data and balances
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddPermiso.setOnClickListener {
            // TODO: Show solicitar permiso dialog
        }
        binding.btnSolicitarVacaciones.setOnClickListener {
            // TODO: Navigate to solicitar vacaciones
        }
        binding.btnSolicitarPermiso.setOnClickListener {
            // TODO: Navigate to solicitar permiso
        }
    }

    private fun setupViewPager() {
        // TODO: Setup ViewPager2 with tabs: Permisos, Licencias, Historial
        // binding.tabLayoutPermisos.addTab(...)
    }
}

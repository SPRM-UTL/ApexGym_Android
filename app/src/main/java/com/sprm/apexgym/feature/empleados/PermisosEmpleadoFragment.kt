package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
        // Datos de permisos
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddPermiso.setOnClickListener {
            Toast.makeText(requireContext(), "Solicitud de permiso", Toast.LENGTH_SHORT).show()
        }
        binding.btnSolicitarVacaciones.setOnClickListener {
            Toast.makeText(requireContext(), "Solicitud de vacaciones", Toast.LENGTH_SHORT).show()
        }
        binding.btnSolicitarPermiso.setOnClickListener {
            Toast.makeText(requireContext(), "Solicitar permiso laboral", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViewPager() {
        // TODO: Setup ViewPager2 with tabs: Permisos, Licencias, Historial
        // binding.tabLayoutPermisos.addTab(...)
    }
}

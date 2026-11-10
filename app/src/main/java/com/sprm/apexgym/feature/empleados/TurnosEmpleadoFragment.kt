package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentTurnosEmpleadoBinding

class TurnosEmpleadoFragment : BaseFragment<FragmentTurnosEmpleadoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_turnos_empleado

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupViewPager()
        setupRecyclerView()
    }

    private fun setupViews() {
        // TODO: Load empleado data
        // Setup tab layout with ViewPager2
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddTurno.setOnClickListener {
            // TODO: Navigate to create turno
        }
        binding.tvVerTodosTurnos.setOnClickListener {
            // TODO: Navigate to full turnos list
        }
    }

    private fun setupViewPager() {
        // TODO: Setup ViewPager2 with FragmentStateAdapter for weeks
        // binding.tabLayoutTurnos.addTab(...)
    }

    private fun setupRecyclerView() {
        binding.rvProximosTurnos.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapter
    }
}

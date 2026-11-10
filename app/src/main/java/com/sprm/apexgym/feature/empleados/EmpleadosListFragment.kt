package com.sprm.apexgym.feature.empleados

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentEmpleadosListBinding

class EmpleadosListFragment : BaseFragment<FragmentEmpleadosListBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_empleados_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerView()
    }

    private fun setupViews() {
        // TODO: Setup stats cards with data
        // TODO: Setup filter chips
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddEmpleado.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CrearEditarEmpleadoFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.ivFilter.setOnClickListener {
            // TODO: Show filter bottom sheet
        }
        binding.etSearchEmpleados.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // TODO: Perform search
                true
            } else false
        }
    }

    private fun setupRecyclerView() {
        binding.rvEmpleados.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapter with EmpleadoAdapter
    }
}

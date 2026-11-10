package com.sprm.apexgym.feature.visitas

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentVisitasListBinding

class VisitasListFragment : BaseFragment<FragmentVisitasListBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_visitas_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerView()
    }

    private fun setupViews() {
        // TODO: Load stats and setup filter chips
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddVisita.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CrearEditarVisitaFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.ivFilterVisitas.setOnClickListener {
            // TODO: Show filter bottom sheet
        }
        binding.etSearchVisitas.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // TODO: Perform search
                true
            } else false
        }
    }

    private fun setupRecyclerView() {
        binding.rvVisitas.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Set adapter with VisitaAdapter
    }
}

package com.sprm.apexgym.feature.visitas

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.VisitaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.databinding.FragmentVisitaDetalleBinding
import kotlinx.coroutines.launch

class VisitaDetalleFragment : BaseFragment<FragmentVisitaDetalleBinding>() {

    private var visitaId: Int = 0

    override fun getLayoutRes(): Int = R.layout.fragment_visita_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visitaId = arguments?.getInt(ARG_VISITA_ID, 0) ?: 0
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flEdit.setOnClickListener {
            if (visitaId > 0) {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.fragment_container, CrearEditarVisitaFragment.newInstance(visitaId))
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(requireContext(), "No se pudo identificar la visita", Toast.LENGTH_SHORT).show()
            }
        }

        cargarVisita()
    }

    private fun cargarVisita() {
        if (visitaId <= 0) {
            Toast.makeText(requireContext(), "No se pudo identificar la visita", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val visita = RetrofitClient.empleadoService.obtenerVisita(visitaId).dataExitosa()
                if (visita == null) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo cargar la visita",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    mostrarVisita(visita)
                }
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarVisita(visita: VisitaDto) {
        val tipoNombre = visita.tipoVisita?.nombre ?: "Tipo #${visita.tipoVisitaId}"
        binding.tvVisitaNombre.text = visita.nombreVisitante ?: "Visitante #${visita.id}"
        binding.tvVisitaEstado.text = tipoNombre.uppercase()
        binding.tvVisitaOrigen.text = if (visita.costo > 0) {
            visita.costo.formatearMonedaSinSufijo()
        } else {
            "Gratis"
        }
        binding.tvVisitaOrigen.setBackgroundResource(
            if (visita.costo > 0) R.drawable.bg_badge_orange else R.drawable.bg_badge_green
        )
        binding.tvVisitaTelDetalle.text = formatearFecha(visita.fechaHora, "dd/MM/yyyy HH:mm")
        binding.tvVisitaEmailDetalle.text = tipoNombre
        binding.tvVisitaDireccionDetalle.text = if (visita.costo > 0) {
            visita.costo.formatearMonedaSinSufijo()
        } else {
            "Sin costo"
        }
        binding.tvVisitaNotas.text = visita.observaciones
            ?.takeIf { it.isNotBlank() }
            ?: "Sin observaciones registradas"
    }

    override fun onResume() {
        super.onResume()
        if (view != null) cargarVisita()
    }

    companion object {
        private const val ARG_VISITA_ID = "visita_id"

        fun newInstance(visitaId: Int): VisitaDetalleFragment {
            return VisitaDetalleFragment().apply {
                arguments = Bundle().apply { putInt(ARG_VISITA_ID, visitaId) }
            }
        }
    }
}

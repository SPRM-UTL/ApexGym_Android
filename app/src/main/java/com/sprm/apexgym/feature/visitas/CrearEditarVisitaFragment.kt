package com.sprm.apexgym.feature.visitas

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.CrearVisitaRequest
import com.sprm.apexgym.core.network.api.TipoVisitaDto
import com.sprm.apexgym.core.network.api.VisitaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentCrearEditarVisitaBinding
import kotlinx.coroutines.launch

class CrearEditarVisitaFragment : BaseFragment<FragmentCrearEditarVisitaBinding>() {

    private var visitaId: Int = 0
    private var tiposVisita: List<TipoVisitaDto> = emptyList()
    private var tipoSeleccionado: TipoVisitaDto? = null

    override fun getLayoutRes(): Int = R.layout.fragment_crear_editar_visita

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visitaId = arguments?.getInt(ARG_VISITA_ID, 0) ?: 0

        binding.tvTitle.text = if (visitaId > 0) "Editar Visita" else "Nueva Visita"
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnCancelar.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnGuardar.setOnClickListener { guardarVisita() }

        cargarTiposVisita()
    }

    private fun cargarTiposVisita() {
        binding.btnGuardar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerTiposVisita()
                tiposVisita = response.dataExitosa().orEmpty()

                binding.actvTipoVisita.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        tiposVisita.map { it.nombre ?: "Tipo #${it.id}" }
                    )
                )
                binding.actvTipoVisita.setOnItemClickListener { _, _, position, _ ->
                    seleccionarTipo(tiposVisita.getOrNull(position))
                }

                binding.btnGuardar.isEnabled = tiposVisita.isNotEmpty()

                if (tiposVisita.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No hay tipos de visita registrados",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (visitaId > 0) {
                    cargarVisita()
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

    private fun cargarVisita() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val visita = RetrofitClient.empleadoService.obtenerVisita(visitaId).dataExitosa()
                if (visita == null) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo cargar la visita",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                mostrarVisita(visita)
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
        binding.etNombreCompleto.setText(visita.nombreVisitante.orEmpty())
        binding.etNotasIniciales.setText(visita.observaciones.orEmpty())

        val tipo = tiposVisita.firstOrNull { it.id == visita.tipoVisitaId }
            ?: visita.tipoVisita
        if (tipo != null) {
            seleccionarTipo(tipo)
            binding.actvTipoVisita.setText(tipo.nombre.orEmpty(), false)
        }
    }

    private fun seleccionarTipo(tipo: TipoVisitaDto?) {
        tipoSeleccionado = tipo
        binding.tvCostoTipo.text = if (tipo == null) {
            "Selecciona un tipo de visita para ver su costo"
        } else if (tipo.costo > 0) {
            "Costo del tipo de visita: ${tipo.costo.formatearMonedaSinSufijo()}"
        } else {
            "Tipo de visita sin costo"
        }
    }

    private fun guardarVisita() {
        val nombre = binding.etNombreCompleto.text?.toString()?.trim().orEmpty()
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el nombre del visitante", Toast.LENGTH_SHORT).show()
            return
        }

        val tipo = tipoSeleccionado
        if (tipo == null) {
            Toast.makeText(requireContext(), "Selecciona un tipo de visita", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnGuardar.isEnabled = false
        binding.btnGuardar.text = "Guardando..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = CrearVisitaRequest(
                    nombreVisitante = nombre,
                    tipoVisitaId = tipo.id,
                    observaciones = binding.etNotasIniciales.text
                        ?.toString()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                )

                val response = if (visitaId > 0) {
                    RetrofitClient.empleadoService.actualizarVisita(visitaId, request)
                } else {
                    RetrofitClient.empleadoService.crearVisita(request)
                }

                if (response.dataExitosa() == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo guardar la visita",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        if (visitaId > 0) "Visita actualizada" else "Visita registrada",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                if (isAdded) {
                    binding.btnGuardar.isEnabled = true
                    binding.btnGuardar.text = "Guardar Visita"
                }
            }
        }
    }

    companion object {
        private const val ARG_VISITA_ID = "visita_id"

        fun newInstance(visitaId: Int = 0): CrearEditarVisitaFragment {
            return CrearEditarVisitaFragment().apply {
                arguments = Bundle().apply { putInt(ARG_VISITA_ID, visitaId) }
            }
        }
    }
}

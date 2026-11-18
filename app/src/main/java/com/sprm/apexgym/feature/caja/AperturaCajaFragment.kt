package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.CajaDto
import com.sprm.apexgym.core.network.api.CrearAperturaRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentAperturaCajaBinding
import kotlinx.coroutines.launch

class AperturaCajaFragment : BaseFragment<FragmentAperturaCajaBinding>() {

    private var cajas: List<CajaDto> = emptyList()

    override fun getLayoutRes(): Int = R.layout.fragment_apertura_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.rvDenominaciones.visibility = View.GONE
        binding.tvTotalCalculado.text = "—"
        binding.tvDiferenciaFondo.text = "El desglose de denominaciones no está disponible en la API."

        binding.etFondoInicial.setOnFocusChangeListener { _, _ -> actualizarTotal() }
        binding.etFondoInicial.setOnEditorActionListener { _, _, _ ->
            actualizarTotal()
            false
        }

        binding.btnAbrirCaja.setOnClickListener { abrirCaja() }
        cargarCajas()
    }

    private fun cargarCajas() {
        binding.btnAbrirCaja.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerCajas()
                cajas = response.dataExitosa().orEmpty()

                if (cajas.isEmpty()) {
                    binding.spCaja.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("Sin cajas registradas")
                    )
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError()
                            ?: "No hay cajas registradas. Créalas en Configuración de caja.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                binding.spCaja.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    cajas.map { it.nombre ?: "Caja #${it.id}" }
                )
                binding.btnAbrirCaja.isEnabled = true
                actualizarTotal()
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun actualizarTotal() {
        val monto = binding.etFondoInicial.text?.toString()?.toDoubleOrNull()
        binding.tvTotalCalculado.text = if (monto != null && monto > 0) {
            monto.formatearMonedaSinSufijo()
        } else {
            "—"
        }
        binding.tvDiferenciaFondo.text = "El desglose de denominaciones no está disponible en la API."
    }

    private fun abrirCaja() {
        val empleadoId = SessionManager.obtenerEmpleadoId(requireContext())
        if (empleadoId == null) {
            Toast.makeText(
                requireContext(),
                "Tu usuario no tiene un empleado asociado. No es posible abrir caja.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val caja = cajas.getOrNull(binding.spCaja.selectedItemPosition)
        if (caja == null) {
            Toast.makeText(requireContext(), "Selecciona una caja", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = binding.etFondoInicial.text?.toString()?.toDoubleOrNull()
        if (monto == null || monto < 0) {
            Toast.makeText(requireContext(), "Ingresa un fondo inicial válido", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAbrirCaja.isEnabled = false
        binding.btnAbrirCaja.text = "Abriendo..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearApertura(
                    CrearAperturaRequest(
                        cajaId = caja.id,
                        empleadoId = empleadoId,
                        montoInicial = monto,
                        observaciones = binding.etObservacionesApertura.text
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                    )
                )

                if (response.dataExitosa() == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo abrir la caja",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Caja abierta correctamente", Toast.LENGTH_SHORT).show()
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
                    binding.btnAbrirCaja.isEnabled = true
                    binding.btnAbrirCaja.text = "Abrir Caja"
                }
            }
        }
    }
}

package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.api.CrearMovimientoRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentNuevoMovimientoSheetBinding
import kotlinx.coroutines.launch

class NuevoMovimientoSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNuevoMovimientoSheetBinding? = null
    private val binding get() = _binding!!
    private var esEntrada = true
    private var aperturaId: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNuevoMovimientoSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aperturaId = arguments?.getInt(ARG_APERTURA_ID, 0) ?: 0
        actualizarTipoVisual()

        binding.btnCloseMovimiento.setOnClickListener { dismiss() }
        binding.btnCancelarMovimiento.setOnClickListener { dismiss() }

        binding.btnTipoEntrada.setOnClickListener {
            esEntrada = true
            actualizarTipoVisual()
        }

        binding.btnTipoSalida.setOnClickListener {
            esEntrada = false
            actualizarTipoVisual()
        }

        binding.btnGuardarMovimiento.setOnClickListener { guardarMovimiento() }
    }

    private fun actualizarTipoVisual() {
        val activo = ContextCompat.getColor(requireContext(), R.color.apex_orange)
        val inactivo = ContextCompat.getColor(requireContext(), R.color.apex_card_dark)
        binding.btnTipoEntrada.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (esEntrada) activo else inactivo)
        binding.btnTipoSalida.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (esEntrada) inactivo else activo)
    }

    private fun guardarMovimiento() {
        if (aperturaId <= 0) {
            Toast.makeText(requireContext(), "No hay una caja abierta", Toast.LENGTH_SHORT).show()
            return
        }

        val empleadoId = SessionManager.obtenerEmpleadoId(requireContext())
        if (empleadoId == null) {
            Toast.makeText(
                requireContext(),
                "Tu usuario no tiene un empleado asociado",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val concepto = binding.etConcepto.text?.toString()?.trim().orEmpty()
        if (concepto.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa el concepto del movimiento", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = binding.etMonto.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
        if (monto == null || monto <= 0.0) {
            Toast.makeText(requireContext(), "Ingresa un monto mayor a cero", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnGuardarMovimiento.isEnabled = false
        binding.btnGuardarMovimiento.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearMovimiento(
                    CrearMovimientoRequest(
                        aperturaCajaId = aperturaId,
                        empleadoId = empleadoId,
                        tipo = if (esEntrada) "ENTRADA" else "SALIDA",
                        monto = monto,
                        concepto = concepto,
                        observaciones = binding.etObservaciones.text
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                    )
                )

                if (response.dataExitosa() == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo registrar el movimiento",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Movimiento registrado. Queda pendiente de validación.",
                        Toast.LENGTH_LONG
                    ).show()
                    parentFragmentManager.setFragmentResult(
                        RESULT_KEY,
                        Bundle().apply { putInt(ARG_APERTURA_ID, aperturaId) }
                    )
                    dismiss()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                if (_binding != null) {
                    binding.btnGuardarMovimiento.isEnabled = true
                    binding.btnGuardarMovimiento.text = "Guardar Movimiento"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NuevoMovimientoSheet"
        const val RESULT_KEY = "nuevo_movimiento_result"
        private const val ARG_APERTURA_ID = "apertura_id"

        fun newInstance(aperturaId: Int): NuevoMovimientoSheetFragment {
            return NuevoMovimientoSheetFragment().apply {
                arguments = Bundle().apply { putInt(ARG_APERTURA_ID, aperturaId) }
            }
        }
    }
}

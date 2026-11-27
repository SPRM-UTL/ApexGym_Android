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
import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearMoneda
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.ui.InputFilters
import com.sprm.apexgym.databinding.FragmentNuevoMovimientoSheetBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class NuevoMovimientoSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNuevoMovimientoSheetBinding? = null
    private val binding get() = _binding!!
    private var esEntrada = true
    private var aperturaId: Int = 0
    private var saldoDisponible: Double? = null
    private var aperturaCerrada: Boolean = false
    private var empleadosResponsables: List<EmpleadoDto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevoMovimientoSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aperturaId = arguments?.getInt(ARG_APERTURA_ID, 0) ?: 0
        if (arguments?.containsKey(ARG_SALDO_DISPONIBLE) == true) {
            val valSaldo = arguments?.getDouble(ARG_SALDO_DISPONIBLE, Double.NaN) ?: Double.NaN
            if (valSaldo.isFinite() && valSaldo >= 0.0) {
                saldoDisponible = valSaldo
                mostrarSaldoDisponible(valSaldo)
            }
        }
        actualizarTipoVisual()

        binding.btnCloseMovimiento.setOnClickListener { dismiss() }
        binding.btnCancelarMovimiento.setOnClickListener { dismiss() }

        binding.etConcepto.filters = InputFilters.plainText(255)
        binding.etMonto.filters = InputFilters.decimalAmount(maxIntegers = 5, maxDecimals = 2)
        binding.etObservaciones.filters = InputFilters.plainText(500, allowNewLines = true)

        binding.btnTipoEntrada.setOnClickListener {
            esEntrada = true
            actualizarTipoVisual()
        }

        binding.btnTipoSalida.setOnClickListener {
            esEntrada = false
            actualizarTipoVisual()
        }

        binding.btnGuardarMovimiento.setOnClickListener { guardarMovimiento() }

        cargarContextoInicial()
    }

    private fun cargarContextoInicial() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = requireContext()
            val empDef = async {
                EmpleadoSelectorHelper.prepararSelector(
                    ctx,
                    binding.llResponsableMovimiento,
                    binding.spResponsableMovimiento
                )
            }
            val resumenDef = async {
                if (aperturaId > 0) {
                    try {
                        RetrofitClient.empleadoService.obtenerResumenApertura(aperturaId).dataExitosa()
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
            }

            val empRes = empDef.await()
            val resumen = resumenDef.await()
            if (!isAdded || _binding == null) return@launch

            empleadosResponsables = empRes.empleados
            if (resumen != null) {
                saldoDisponible = resumen.saldoDisponible
                mostrarSaldoDisponible(resumen.saldoDisponible)
                if (!CajaValidacionLogic.esAperturaAbierta(resumen.apertura?.estado)) {
                    aperturaCerrada = true
                    binding.btnGuardarMovimiento.isEnabled = false
                    binding.tvSaldoDisponibleSheet.visibility = View.VISIBLE
                    binding.tvSaldoDisponibleSheet.text = "Esta apertura de caja ya está cerrada"
                }
            }
        }
    }

    private fun mostrarSaldoDisponible(saldo: Double) {
        binding.tvSaldoDisponibleSheet.visibility = View.VISIBLE
        binding.tvSaldoDisponibleSheet.text = "Saldo disponible en caja: ${saldo.formatearMoneda()}"
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
        if (aperturaCerrada) {
            Toast.makeText(
                requireContext(),
                "La apertura de caja ya está cerrada. No se pueden agregar movimientos.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val empleadoId = EmpleadoSelectorHelper.resolverEmpleadoId(
            requireContext(),
            empleadosResponsables,
            binding.spResponsableMovimiento
        )
        if (empleadoId == null) {
            Toast.makeText(
                requireContext(),
                "Selecciona el empleado responsable del movimiento",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val tipo = if (esEntrada) "ENTRADA" else "SALIDA"
        val concepto = binding.etConcepto.text?.toString()?.trim().orEmpty()
        val monto = binding.etMonto.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
        val observaciones = binding.etObservaciones.text?.toString()?.trim()?.takeIf { it.isNotBlank() }

        val errorValidacion = CajaValidacionLogic.validarMovimientoCaja(
            tipo = tipo,
            monto = monto,
            concepto = concepto,
            saldoDisponible = saldoDisponible
        )
        if (errorValidacion != null || monto == null) {
            Toast.makeText(
                requireContext(),
                errorValidacion ?: "Revisa los datos del movimiento",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if ((observaciones?.length ?: 0) > CajaValidacionLogic.MAX_CARACTERES_OBSERVACIONES) {
            Toast.makeText(
                requireContext(),
                "Las observaciones no deben exceder ${CajaValidacionLogic.MAX_CARACTERES_OBSERVACIONES} caracteres",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.btnGuardarMovimiento.isEnabled = false
        binding.btnGuardarMovimiento.text = "Guardando..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearMovimiento(
                    CrearMovimientoRequest(
                        aperturaCajaId = aperturaId,
                        empleadoId = empleadoId,
                        tipo = tipo,
                        monto = monto,
                        concepto = concepto,
                        observaciones = observaciones
                    )
                )
                if (!isAdded || _binding == null) return@launch

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
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                if (isAdded && _binding != null) {
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
        private const val ARG_SALDO_DISPONIBLE = "saldo_disponible"

        fun newInstance(aperturaId: Int, saldoDisponible: Double? = null): NuevoMovimientoSheetFragment {
            return NuevoMovimientoSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_APERTURA_ID, aperturaId)
                    if (saldoDisponible != null && saldoDisponible.isFinite()) {
                        putDouble(ARG_SALDO_DISPONIBLE, saldoDisponible)
                    }
                }
            }
        }
    }
}

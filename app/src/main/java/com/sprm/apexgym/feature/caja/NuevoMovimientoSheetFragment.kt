package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.databinding.FragmentNuevoMovimientoSheetBinding

class NuevoMovimientoSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNuevoMovimientoSheetBinding? = null
    private val binding get() = _binding!!
    private var esEntrada = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNuevoMovimientoSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseMovimiento.setOnClickListener { dismiss() }
        binding.btnCancelarMovimiento.setOnClickListener { dismiss() }

        binding.btnTipoEntrada.setOnClickListener {
            esEntrada = true
            Toast.makeText(requireContext(), "Tipo: Entrada", Toast.LENGTH_SHORT).show()
        }

        binding.btnTipoSalida.setOnClickListener {
            esEntrada = false
            Toast.makeText(requireContext(), "Tipo: Salida", Toast.LENGTH_SHORT).show()
        }

        binding.btnGuardarMovimiento.setOnClickListener {
            val concepto = binding.etConcepto.text.toString().trim()
            val monto = binding.etMonto.text.toString().trim()
            if (concepto.isEmpty() || monto.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa concepto y monto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val tipoStr = if (esEntrada) "Entrada" else "Salida"
            Toast.makeText(requireContext(), "$tipoStr registrado correctamente: S/ $monto", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NuevoMovimientoSheet"
    }
}

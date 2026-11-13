package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.databinding.FragmentAsistenciaDetalleSheetBinding
import java.text.SimpleDateFormat
import java.util.*

class AsistenciaDetalleSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAsistenciaDetalleSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAsistenciaDetalleSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX"))
        binding.tvFechaHora.text = sdf.format(Date())
        binding.tvTipoAcceso.text = "Entrada QR"
        binding.tvPlan.text = "Plan Premium"
        binding.tvDiasRestantes.text = "23 días"
        binding.btnCloseDetalle.setOnClickListener { dismiss() }
        binding.btnRepetirAcceso.setOnClickListener {
            Toast.makeText(requireContext(), "Acceso registrado nuevamente", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        binding.btnVerPerfilCliente.setOnClickListener {
            Toast.makeText(requireContext(), "Ver perfil del cliente — próximamente", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AsistenciaDetalleSheet"
    }
}

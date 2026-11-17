package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.sprm.apexgym.databinding.DialogAccesoResultadoBinding

class AccesoResultadoDialogFragment : DialogFragment() {

    private var _binding: DialogAccesoResultadoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAccesoResultadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val success = arguments?.getBoolean(ARG_SUCCESS, true) ?: true

        val nombre = arguments?.getString(ARG_NOMBRE).orEmpty()
        val plan = arguments?.getString(ARG_PLAN).orEmpty()
        val mensaje = arguments?.getString(ARG_MENSAJE).orEmpty()

        if (success) {
            binding.ivSuccess.visibility = View.VISIBLE
            binding.ivError.visibility = View.GONE
            binding.ivWarning.visibility = View.GONE
            binding.tvResultTitle.text = "Acceso Permitido"
            binding.tvResultSubtitle.text = "Bienvenido a ApexGym"
            binding.cvMemberInfo.visibility = View.VISIBLE
            binding.llDeniedReason.visibility = View.GONE

            if (nombre.isNotBlank()) binding.tvMemberName.text = nombre
            binding.tvMemberMembership.text = plan.ifBlank { "Sin membresía activa" }
            binding.tvMemberStatus.text = "Acceso autorizado"
        } else {
            binding.ivSuccess.visibility = View.GONE
            binding.ivError.visibility = View.VISIBLE
            binding.ivWarning.visibility = View.GONE
            binding.tvResultTitle.text = "Acceso Denegado"
            binding.tvResultSubtitle.text = "Verifica la membresía del cliente"
            binding.cvMemberInfo.visibility = View.GONE
            binding.llDeniedReason.visibility = View.VISIBLE
            binding.tvDeniedReason.text = mensaje.ifBlank {
                "Membresía inactiva o no registrada"
            }
        }

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnScanAgain.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AccesoResultadoDialog"
        private const val ARG_SUCCESS = "arg_success"
        private const val ARG_MENSAJE = "arg_mensaje"
        private const val ARG_NOMBRE = "arg_nombre"
        private const val ARG_PLAN = "arg_plan"

        fun newInstance(
            success: Boolean,
            mensaje: String? = null,
            nombre: String? = null,
            plan: String? = null
        ): AccesoResultadoDialogFragment {
            return AccesoResultadoDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SUCCESS, success)
                    putString(ARG_MENSAJE, mensaje)
                    putString(ARG_NOMBRE, nombre)
                    putString(ARG_PLAN, plan)
                }
            }
        }
    }
}

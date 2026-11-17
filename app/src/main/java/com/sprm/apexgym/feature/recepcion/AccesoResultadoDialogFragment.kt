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

        if (success) {
            binding.ivSuccess.visibility = View.VISIBLE
            binding.ivError.visibility = View.GONE
            binding.ivWarning.visibility = View.GONE
            binding.tvResultTitle.text = "Acceso Permitido"
            binding.tvResultSubtitle.text = "Bienvenido a ApexGym"
            binding.cvMemberInfo.visibility = View.VISIBLE
            binding.llDeniedReason.visibility = View.GONE
        } else {
            binding.ivSuccess.visibility = View.GONE
            binding.ivError.visibility = View.VISIBLE
            binding.ivWarning.visibility = View.GONE
            binding.tvResultTitle.text = "Acceso Denegado"
            binding.tvResultSubtitle.text = "Verifica tu membresía"
            binding.cvMemberInfo.visibility = View.GONE
            binding.llDeniedReason.visibility = View.VISIBLE
            binding.tvDeniedReason.text = "Membresía inactiva o no registrada"
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
        fun newInstance(success: Boolean): AccesoResultadoDialogFragment {
            return AccesoResultadoDialogFragment().apply {
                arguments = Bundle().apply { putBoolean(ARG_SUCCESS, success) }
            }
        }
    }
}

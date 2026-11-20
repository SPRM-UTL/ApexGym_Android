package com.sprm.apexgym.feature.recepcion

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.sprm.apexgym.databinding.DialogAccesoResultadoBinding

class AccesoResultadoDialogFragment : DialogFragment() {

    private var _binding: DialogAccesoResultadoBinding? = null
    private val binding get() = _binding!!

    var onDismissAction: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAccesoResultadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val success = arguments?.getBoolean(ARG_SUCCESS, true) ?: true
        val nombre = arguments?.getString(ARG_NOMBRE).orEmpty()
        val plan = arguments?.getString(ARG_PLAN).orEmpty()
        val mensaje = arguments?.getString(ARG_MENSAJE).orEmpty()
        val botonPrimario = arguments?.getString(ARG_BOTON_PRIMARIO).orEmpty()

        if (success) {
            binding.ivSuccess.visibility = View.VISIBLE
            binding.ivError.visibility = View.GONE
            binding.ivWarning.visibility = View.GONE
            binding.tvResultTitle.text = "Acceso Permitido"
            binding.tvResultSubtitle.text = mensaje.ifBlank { "Entrada registrada correctamente" }
            binding.cvMemberInfo.visibility = View.VISIBLE
            binding.llDeniedReason.visibility = View.GONE

            binding.tvMemberName.text = nombre.ifBlank { "Socio" }
            binding.tvMemberMembership.text = plan.ifBlank { "Membresía vigente" }
            binding.tvMemberStatus.text = "REGISTRADO"
        } else {
            binding.ivSuccess.visibility = View.GONE
            binding.ivError.visibility = View.VISIBLE
            binding.ivWarning.visibility = View.GONE
            binding.tvResultTitle.text = "Acceso Denegado"
            binding.tvResultSubtitle.text = if (nombre.isNotBlank()) nombre else "Verifica la membresía"
            binding.cvMemberInfo.visibility = View.GONE
            binding.llDeniedReason.visibility = View.VISIBLE
            binding.tvDeniedReason.text = mensaje.ifBlank {
                "Membresía inactiva o vencida"
            }
        }

        if (botonPrimario.isNotBlank()) {
            binding.btnScanAgain.text = botonPrimario
        }

        val cerrar = View.OnClickListener {
            dismiss()
            onDismissAction?.invoke()
        }
        binding.btnClose.setOnClickListener(cerrar)
        binding.btnScanAgain.setOnClickListener(cerrar)
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
        private const val ARG_BOTON_PRIMARIO = "arg_boton_primario"

        fun newInstance(
            success: Boolean,
            mensaje: String? = null,
            nombre: String? = null,
            plan: String? = null,
            botonPrimario: String? = null
        ): AccesoResultadoDialogFragment {
            return AccesoResultadoDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SUCCESS, success)
                    putString(ARG_MENSAJE, mensaje)
                    putString(ARG_NOMBRE, nombre)
                    putString(ARG_PLAN, plan)
                    putString(ARG_BOTON_PRIMARIO, botonPrimario)
                }
            }
        }
    }
}

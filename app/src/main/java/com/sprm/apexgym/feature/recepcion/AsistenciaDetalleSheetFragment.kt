package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.core.network.api.AsistenciaDto
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.databinding.FragmentAsistenciaDetalleSheetBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsistenciaDetalleSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAsistenciaDetalleSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAsistenciaDetalleSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val asistencia = arguments?.getString(ARG_ASISTENCIA)?.let { serializada ->
            try {
                org.json.JSONObject(serializada).let { json ->
                    AsistenciaDto(
                        id = json.optInt("id"),
                        clienteId = json.optInt("clienteId"),
                        fechaHora = json.optString("fechaHora").takeIf { it.isNotBlank() },
                        observaciones = json.optString("observaciones").takeIf { it.isNotBlank() },
                        cliente = null,
                        clienteMembresia = null
                    )
                }
            } catch (_: Exception) {
                null
            }
        }

        val nombres = arguments?.getString(ARG_CLIENTE).orEmpty()
        val plan = arguments?.getString(ARG_PLAN).orEmpty()
        val estado = arguments?.getString(ARG_ESTADO).orEmpty()
        val fechaFin = arguments?.getString(ARG_FECHA_FIN).orEmpty()

        binding.tvStatusTitle.text = "Acceso registrado"
        binding.tvStatusSubtitle.text = nombres.ifBlank { "Cliente" }
        binding.tvTipoAcceso.text = "Entrada registrada en recepción"
        binding.tvFechaHora.text = asistencia?.fechaHora?.let { formatearFecha(it) }
            ?: SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX")).format(Date())
        binding.tvMetodo.text = "No disponible en la API"
        binding.tvRecepcionista.text = "No disponible en la API"
        binding.tvPlan.text = plan.ifBlank { "Sin membresía activa" }
        binding.tvDiasRestantes.text = calcularDiasRestantes(fechaFin)
        binding.tvEstadoMembresia.text = estado.ifBlank { "Sin estado" }
        binding.tvObservaciones.text = asistencia?.observaciones?.takeIf { it.isNotBlank() }
            ?: "Sin observaciones"

        binding.btnCloseDetalle.setOnClickListener { dismiss() }

        // El backend no expone repetición de acceso ni perfil de cliente para recepción.
        binding.btnRepetirAcceso.visibility = View.GONE
        binding.btnVerPerfilCliente.visibility = View.GONE
    }

    private fun calcularDiasRestantes(fechaFin: String): String {
        if (fechaFin.isBlank()) return "—"
        return try {
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val fin = formato.parse(fechaFin.take(10)) ?: return fechaFin
            val dias = ((fin.time - System.currentTimeMillis()) / 86_400_000L).toInt()
            if (dias < 0) "Vencida" else "$dias días"
        } catch (_: Exception) {
            fechaFin
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AsistenciaDetalleSheet"
        private const val ARG_ASISTENCIA = "arg_asistencia"
        private const val ARG_CLIENTE = "arg_cliente"
        private const val ARG_PLAN = "arg_plan"
        private const val ARG_ESTADO = "arg_estado"
        private const val ARG_FECHA_FIN = "arg_fecha_fin"

        fun newInstance(asistencia: AsistenciaDto): AsistenciaDetalleSheetFragment {
            val json = org.json.JSONObject().apply {
                put("id", asistencia.id)
                put("clienteId", asistencia.clienteId)
                put("fechaHora", asistencia.fechaHora ?: "")
                put("observaciones", asistencia.observaciones ?: "")
            }
            return AsistenciaDetalleSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ASISTENCIA, json.toString())
                    putString(ARG_CLIENTE, asistencia.cliente?.nombreCompleto.orEmpty())
                    putString(ARG_PLAN, asistencia.clienteMembresia?.tipoMembresia?.nombre.orEmpty())
                    putString(
                        ARG_ESTADO,
                        asistencia.clienteMembresia?.estadoMembresia?.nombre.orEmpty()
                    )
                    putString(ARG_FECHA_FIN, asistencia.clienteMembresia?.fechaFin.orEmpty())
                }
            }
        }
    }
}

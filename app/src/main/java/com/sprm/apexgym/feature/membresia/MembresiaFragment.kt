package com.sprm.apexgym.feature.membresia

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sprm.apexgym.core.util.QrGenerator
import com.sprm.apexgym.databinding.FragmentMembresiaBinding
import com.sprm.apexgym.feature.membresia.adapter.BeneficiosAdapter
import java.util.Locale

class MembresiaFragment : Fragment() {

    private var _binding: FragmentMembresiaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MembresiaViewModel by viewModels()
    private val beneficiosAdapter = BeneficiosAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembresiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarVistas()
        observarViewModel()

        viewModel.cargarInformacion()
    }

    private fun configurarVistas() {
        binding.rvBeneficios.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = beneficiosAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.cargarInformacion()
        }

        binding.btnReintentar.setOnClickListener {
            viewModel.cargarInformacion()
        }

        binding.btnRegresar.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun observarViewModel() {
        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.swipeRefresh.isRefreshing = cargando
            binding.progressBar.visibility = if (cargando && !binding.swipeRefresh.isRefreshing) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.cardError.visibility = View.VISIBLE
                binding.tvMensajeError.text = error
            } else {
                binding.cardError.visibility = View.GONE
            }
        }

        viewModel.membresiaData.observe(viewLifecycleOwner) { data ->
            if (data == null) return@observe

            val cliente = data.cliente
            if (cliente != null) {
                binding.tvSaludoCliente.text = "¡Hola, ${cliente.nombreCompleto}!"
                val codigo = if (cliente.codigoAcceso.isNotBlank()) cliente.codigoAcceso else "APEX-CLI-${cliente.id}"
                binding.tvCodigoAcceso.text = codigo

                val qrBitmap = QrGenerator.generarQr(codigo, 512, 512)
                if (qrBitmap != null) {
                    binding.ivQrAcceso.setImageBitmap(qrBitmap)
                }
            }

            val membresia = data.membresia
            if (membresia != null && data.tieneMembresia) {
                binding.tvNombrePlan.text = membresia.plan
                binding.tvDescripcionPlan.text = membresia.descripcionPlan ?: "Membresía activa en sucursal"

                val fechaInicio = membresia.fechaInicio ?: "N/D"
                val fechaFin = membresia.fechaFin ?: "N/D"
                binding.tvFechasVigencia.text = "$fechaInicio  al  $fechaFin"

                binding.tvDiasRestantes.text = membresia.diasRestantes.toString()
                binding.tvEstadoMembresia.text = membresia.estado.uppercase(Locale.getDefault())

                val colorHex = membresia.colorEstado ?: if (membresia.diasRestantes > 5) "#2b8a3e" else "#f59f00"
                actualizarColorBadge(binding.tvEstadoMembresia, colorHex)

                beneficiosAdapter.submitList(membresia.beneficios)
            } else {
                binding.tvNombrePlan.text = "Sin membresía activa"
                binding.tvDescripcionPlan.text = "Adquiere o renueva tu plan para ingresar al gimnasio"
                binding.tvFechasVigencia.text = "No disponible"
                binding.tvDiasRestantes.text = "0"
                binding.tvEstadoMembresia.text = "INACTIVA"
                actualizarColorBadge(binding.tvEstadoMembresia, "#e03131")

                if (membresia != null && membresia.beneficios.isNotEmpty()) {
                    beneficiosAdapter.submitList(membresia.beneficios)
                }
            }
        }

        viewModel.saldoData.observe(viewLifecycleOwner) { saldo ->
            if (saldo == null) return@observe

            binding.tvMontoSaldo.text = String.format(Locale.getDefault(), "$%.2f %s", saldo.saldoPendiente, saldo.moneda)

            if (saldo.alCorriente) {
                binding.tvEstadoSaldo.text = "AL CORRIENTE"
                binding.tvMontoSaldo.setTextColor(Color.parseColor("#2b8a3e"))
                actualizarColorBadge(binding.tvEstadoSaldo, "#2b8a3e")
                binding.tvProximoPago.text = if (saldo.proximoVencimiento != null) {
                    "Próximo vencimiento: ${saldo.proximoVencimiento} ($${String.format(Locale.getDefault(), "%.2f", saldo.proximoMontoAPagar)} ${saldo.moneda})"
                } else {
                    "Sin pagos pendientes"
                }
            } else {
                binding.tvEstadoSaldo.text = "PAGO PENDIENTE"
                binding.tvMontoSaldo.setTextColor(Color.parseColor("#e03131"))
                actualizarColorBadge(binding.tvEstadoSaldo, "#e03131")
                binding.tvProximoPago.text = "Favor de renovar en recepción o en línea para reactivar tu acceso."
            }
        }
    }

    private fun actualizarColorBadge(view: View, colorHex: String) {
        try {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 40f
                setColor(Color.parseColor(colorHex))
            }
            view.background = drawable
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

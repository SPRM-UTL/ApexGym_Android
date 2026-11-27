package com.sprm.apexgym.feature.caja

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.EmpresaTicketConfig
import com.sprm.apexgym.core.network.api.CorteCajaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.ui.TicketImageSharer
import com.sprm.apexgym.databinding.FragmentCorteDetalleBinding
import kotlinx.coroutines.launch
import kotlin.math.abs

class CorteDetalleFragment : BaseFragment<FragmentCorteDetalleBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_corte_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: Bundle()
        val corteId = args.getInt(ARG_CORTE_ID, 0)
        val cajaNombre = args.getString(ARG_CAJA_NOMBRE).orEmpty().ifBlank { "Caja" }
        val responsable = args.getString(ARG_RESPONSABLE).orEmpty().ifBlank { "Empleado" }
        val fechaCorte = args.getString(ARG_FECHA_CORTE)
        var estado = args.getString(ARG_ESTADO).orEmpty().ifBlank { "PENDIENTE" }.uppercase()
        val efectivoContado = args.getDouble(ARG_EFECTIVO_CONTADO, 0.0)
        val totalVentas = args.getDouble(ARG_TOTAL_VENTAS, 0.0)
        val totalVentasEfectivo = args.getDouble(ARG_TOTAL_VENTAS_EFECTIVO, 0.0)
        val totalEntradas = args.getDouble(ARG_TOTAL_ENTRADAS, 0.0)
        val totalSalidas = args.getDouble(ARG_TOTAL_SALIDAS, 0.0)
        val diferencia = args.getDouble(ARG_DIFERENCIA, 0.0)
        val observaciones = args.getString(ARG_OBSERVACIONES).orEmpty().trim()

        binding.flBackCorteDetalle.setOnClickListener { parentFragmentManager.popBackStack() }

        val fechaFormateada = formatearFecha(fechaCorte, "dd/MM/yyyy HH:mm")
        binding.tvCorteDetalleNumero.text = "Corte de Caja #$corteId"
        binding.tvCorteDetalleCaja.text = cajaNombre
        binding.tvCorteDetalleFecha.text = fechaFormateada
        binding.tvCorteDetalleResponsable.text = responsable
        binding.tvCorteDetalleEfectivoContado.text = "${efectivoContado.formatearMonedaSinSufijo()} MXN"
        binding.tvCorteDetalleVentasTotales.text = "${totalVentas.formatearMonedaSinSufijo()} MXN"
        binding.tvCorteDetalleVentasEfectivo.text = "${totalVentasEfectivo.formatearMonedaSinSufijo()} MXN"
        binding.tvCorteDetalleEntradas.text = "+ ${totalEntradas.formatearMonedaSinSufijo()} MXN"
        binding.tvCorteDetalleSalidas.text = "- ${totalSalidas.formatearMonedaSinSufijo()} MXN"
        binding.tvCorteDetalleDiferencia.text = "${diferencia.formatearMonedaSinSufijo()} MXN"
        binding.tvCorteDetalleDiferencia.setTextColor(
            when {
                abs(diferencia) < 0.01 -> Color.parseColor("#16A34A")
                diferencia < 0 -> Color.parseColor("#EF4444")
                else -> Color.parseColor("#F97316")
            }
        )

        if (observaciones.isNotEmpty()) {
            binding.llCorteDetalleObservaciones.visibility = View.VISIBLE
            binding.tvCorteDetalleObservaciones.text = observaciones
        } else {
            binding.llCorteDetalleObservaciones.visibility = View.GONE
        }

        fun aplicarEstadoUi(estadoActual: String) {
            val validado = estadoActual.equals("VALIDADO", ignoreCase = true)
            binding.tvCorteDetalleEstado.text = estadoActual
            binding.tvCorteDetalleEstado.setBackgroundResource(
                if (validado) R.drawable.bg_badge_green else R.drawable.bg_badge_orange
            )
            binding.btnValidarCorteDetalle.visibility = if (validado) View.GONE else View.VISIBLE
        }
        aplicarEstadoUi(estado)

        var mensajePieTicket = EmpresaTicketConfig.obtenerMensajeInicial()
        binding.tvCorteDetalleFooterMessage.text = mensajePieTicket
        viewLifecycleOwner.lifecycleScope.launch {
            val mensajeRemoto = EmpresaTicketConfig.obtenerMensajeTicket()
            if (!isAdded || getView() == null) return@launch
            mensajePieTicket = mensajeRemoto
            binding.tvCorteDetalleFooterMessage.text = mensajePieTicket
        }

        binding.flShareCorteDetalle.setOnClickListener {
            val resumen = buildString {
                appendLine("APEX GYM - Corte de Caja #$corteId")
                appendLine("Caja: $cajaNombre")
                appendLine("Responsable: $responsable")
                appendLine("Fecha: $fechaFormateada")
                appendLine("Estado: $estado")
                appendLine("Efectivo contado: ${efectivoContado.formatearMonedaSinSufijo()} MXN")
                appendLine("Ventas totales: ${totalVentas.formatearMonedaSinSufijo()} MXN")
                appendLine("Ventas efectivo: ${totalVentasEfectivo.formatearMonedaSinSufijo()} MXN")
                appendLine("Entradas: +${totalEntradas.formatearMonedaSinSufijo()} MXN")
                appendLine("Salidas: -${totalSalidas.formatearMonedaSinSufijo()} MXN")
                appendLine("Diferencia: ${diferencia.formatearMonedaSinSufijo()} MXN")
                if (observaciones.isNotEmpty()) {
                    appendLine("Observaciones: $observaciones")
                }
                append(mensajePieTicket)
            }
            TicketImageSharer.compartirComprobanteComoImagen(
                context = requireContext(),
                cardView = binding.cvCorteDetalleCard,
                nombreArchivoBase = "corte_caja_$corteId",
                tituloChooser = "Compartir detalle de corte",
                asunto = "Corte de Caja #$corteId - ApexGym",
                textoRespaldo = resumen
            )
        }

        binding.btnValidarCorteDetalle.setOnClickListener {
            if (corteId <= 0) return@setOnClickListener
            binding.btnValidarCorteDetalle.isEnabled = false
            binding.btnValidarCorteDetalle.text = "Validando…"

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val res = RetrofitClient.empleadoService.validarCorte(corteId)
                    if (!isAdded || view == null) return@launch
                    if (res.dataExitosa() == null) {
                        binding.btnValidarCorteDetalle.isEnabled = true
                        binding.btnValidarCorteDetalle.text = "Validar corte"
                        Toast.makeText(
                            requireContext(),
                            res.mensajeDeError() ?: "No se pudo validar el corte",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        estado = "VALIDADO"
                        aplicarEstadoUi(estado)
                        parentFragmentManager.setFragmentResult(
                            RESULT_KEY_CORTE_VALIDADO,
                            Bundle().apply { putInt(ARG_CORTE_ID, corteId) }
                        )
                        Toast.makeText(requireContext(), "Corte validado", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    if (isAdded && view != null) {
                        binding.btnValidarCorteDetalle.isEnabled = true
                        binding.btnValidarCorteDetalle.text = "Validar corte"
                        Toast.makeText(
                            requireContext(),
                            "No se pudo conectar con el servidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    companion object {
        const val RESULT_KEY_CORTE_VALIDADO = "apex_corte_validado_result"

        private const val ARG_CORTE_ID = "arg_corte_id"
        private const val ARG_CAJA_NOMBRE = "arg_caja_nombre"
        private const val ARG_RESPONSABLE = "arg_responsable"
        private const val ARG_FECHA_CORTE = "arg_fecha_corte"
        private const val ARG_ESTADO = "arg_estado"
        private const val ARG_EFECTIVO_CONTADO = "arg_efectivo_contado"
        private const val ARG_TOTAL_VENTAS = "arg_total_ventas"
        private const val ARG_TOTAL_VENTAS_EFECTIVO = "arg_total_ventas_efectivo"
        private const val ARG_TOTAL_ENTRADAS = "arg_total_entradas"
        private const val ARG_TOTAL_SALIDAS = "arg_total_salidas"
        private const val ARG_DIFERENCIA = "arg_diferencia"
        private const val ARG_OBSERVACIONES = "arg_observaciones"

        fun newInstance(
            corte: CorteCajaDto,
            cajaNombre: String,
            responsable: String
        ): CorteDetalleFragment = CorteDetalleFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_CORTE_ID, corte.id)
                putString(ARG_CAJA_NOMBRE, cajaNombre)
                putString(ARG_RESPONSABLE, responsable)
                putString(ARG_FECHA_CORTE, corte.fechaCorte)
                putString(ARG_ESTADO, corte.estado)
                putDouble(ARG_EFECTIVO_CONTADO, corte.efectivoContado)
                putDouble(ARG_TOTAL_VENTAS, corte.totalVentas)
                putDouble(ARG_TOTAL_VENTAS_EFECTIVO, corte.totalVentasEfectivo)
                putDouble(ARG_TOTAL_ENTRADAS, corte.totalEntradas)
                putDouble(ARG_TOTAL_SALIDAS, corte.totalSalidas)
                putDouble(ARG_DIFERENCIA, corte.diferencia)
                putString(ARG_OBSERVACIONES, corte.observaciones)
            }
        }
    }
}

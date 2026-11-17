package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.AperturaCajaDto
import com.sprm.apexgym.core.network.api.CrearCorteRequest
import com.sprm.apexgym.core.network.api.ResumenAperturaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentCorteCajaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CorteCajaFragment : BaseFragment<FragmentCorteCajaBinding>() {

    private var aperturaId: Int = 0
    private var apertura: AperturaCajaDto? = null
    private var resumen: ResumenAperturaDto? = null
    private var efectivoEsperado: Double = 0.0

    override fun getLayoutRes(): Int = R.layout.fragment_corte_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aperturaId = arguments?.getInt(ARG_APERTURA_ID, 0) ?: 0

        binding.rvCorteVentasMetodo.layoutManager = LinearLayoutManager(requireContext())
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        val ahora = Date()
        binding.tvCorteFecha.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora)
        binding.tvCorteHora.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora)
        binding.tvCorteResponsable.text = SessionManager.obtenerNombre(requireContext()) ?: "—"
        binding.tvCorteEstado.text = "EN PROCESO"

        binding.etCorteEfectivoContado.doAfterTextChanged { actualizarDiferencia() }
        binding.btnFinalizarCorte.setOnClickListener { confirmarCorte() }

        cargarCorte()
    }

    private fun cargarCorte() {
        binding.tvCorteCajaNombre.text = "Cargando caja…"
        binding.tvCorteVentasTotales.text = "…"
        binding.tvCorteNumTransacciones.text = "…"
        binding.tvCorteEfectivoCaja.text = "…"
        binding.tvCorteDiferencia.text = "Diferencia: —"
        binding.cvCorteDiferencias.visibility = View.GONE
        binding.btnFinalizarCorte.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val aperturas = RetrofitClient.empleadoService.obtenerAperturas().dataExitosa().orEmpty()
                val activa = if (aperturaId > 0) {
                    aperturas.firstOrNull { it.id == aperturaId }
                } else {
                    aperturas.filter { it.estado?.uppercase() != "CERRADA" }.maxByOrNull { it.id }
                }

                if (activa == null) {
                    binding.tvCorteCajaNombre.text = "No hay caja abierta"
                    binding.tvCorteVentasTotales.text = "—"
                    binding.tvCorteNumTransacciones.text = "—"
                    binding.tvCorteEfectivoCaja.text = "—"
                    Toast.makeText(requireContext(), "No hay una caja abierta", Toast.LENGTH_LONG).show()
                    return@launch
                }

                apertura = activa
                aperturaId = activa.id
                binding.tvCorteCajaNombre.text = activa.caja?.nombre ?: "Caja #${activa.id}"

                val resumenCargado = RetrofitClient.empleadoService
                    .obtenerResumenApertura(activa.id)
                    .dataExitosa()

                resumen = resumenCargado
                efectivoEsperado = resumenCargado?.saldoDisponible ?: activa.montoInicial

                binding.tvCorteVentasTotales.text = (resumenCargado?.totales?.totalVentas ?: 0.0)
                    .formatearMonedaSinSufijo()
                binding.tvCorteNumTransacciones.text =
                    "${resumenCargado?.totales?.transaccionesVentas ?: 0} transacciones"
                binding.tvCorteEfectivoCaja.text = efectivoEsperado.formatearMonedaSinSufijo()
                binding.tvCorteDiferencia.text = "Esperado: ${efectivoEsperado.formatearMonedaSinSufijo()}"
                binding.cvCorteDiferencias.visibility = View.GONE
                binding.btnFinalizarCorte.isEnabled = true

                renderizarVentasMetodo(resumenCargado)
            } catch (_: Exception) {
                binding.tvCorteCajaNombre.text = "Sin conexión"
                binding.tvCorteVentasTotales.text = "—"
                binding.tvCorteNumTransacciones.text = "—"
                binding.tvCorteEfectivoCaja.text = "—"
                binding.tvCorteDiferencia.text = "No se pudo conectar con el servidor"
                Toast.makeText(requireContext(), "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderizarVentasMetodo(resumenCargado: ResumenAperturaDto?) {
        val metodos = resumenCargado?.totales?.ventasPorMetodoPago.orEmpty()
        binding.rvCorteVentasMetodo.adapter = object : RecyclerView.Adapter<SimpleRowHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleRowHolder {
                return crearFila(parent.context)
            }

            override fun onBindViewHolder(holder: SimpleRowHolder, position: Int) {
                if (metodos.isEmpty()) {
                    holder.tvTitulo.text = "Sin ventas registradas en esta apertura"
                    holder.tvDetalle.text = ""
                    holder.tvMonto.text = ""
                } else {
                    val metodo = metodos[position]
                    holder.tvTitulo.text = metodo.nombre ?: "Método ${metodo.metodoPagoId}"
                    holder.tvDetalle.text = "${metodo.transacciones} transacciones"
                    holder.tvMonto.text = metodo.total.formatearMonedaSinSufijo()
                }
            }

            override fun getItemCount(): Int = metodos.size.coerceAtLeast(1)
        }
    }

    private fun actualizarDiferencia() {
        val texto = binding.etCorteEfectivoContado.text?.toString()?.trim()?.replace(',', '.')
        val contado = texto?.toDoubleOrNull()
        if (contado == null) {
            binding.tvCorteEfectivoCaja.text = efectivoEsperado.formatearMonedaSinSufijo()
            binding.tvCorteDiferencia.text = "Esperado: ${efectivoEsperado.formatearMonedaSinSufijo()}"
            binding.cvCorteDiferencias.visibility = View.GONE
            return
        }

        binding.tvCorteEfectivoCaja.text = contado.formatearMonedaSinSufijo()
        val diferencia = contado - efectivoEsperado
        binding.tvCorteDiferencia.text = "Diferencia: ${diferencia.formatearMonedaSinSufijo()}"
        if (diferencia == 0.0) {
            binding.cvCorteDiferencias.visibility = View.GONE
        } else {
            binding.cvCorteDiferencias.visibility = View.VISIBLE
            binding.tvCorteSobranteFaltante.text = if (diferencia > 0) {
                "Sobrante ${diferencia.formatearMonedaSinSufijo()}"
            } else {
                "Faltante ${(-diferencia).formatearMonedaSinSufijo()}"
            }
        }
    }

    private fun confirmarCorte() {
        val actual = apertura
        if (actual == null) {
            Toast.makeText(requireContext(), "No hay una caja abierta", Toast.LENGTH_SHORT).show()
            return
        }

        val pendientes = resumen?.movimientosPendientes ?: 0
        if (pendientes > 0) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Movimientos pendientes")
                .setMessage(
                    "Hay $pendientes movimiento(s) pendiente(s) de validar. " +
                        "El corte requiere que todos estén validados."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Ver movimientos") { _, _ ->
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                        .replace(R.id.fragment_container, MovimientosCajaFragment.newInstance(actual.id))
                        .addToBackStack(null)
                        .commit()
                }
                .show()
            return
        }

        val empleadoId = SessionManager.obtenerEmpleadoId(requireContext())
        if (empleadoId == null) {
            Toast.makeText(
                requireContext(),
                "Tu usuario no tiene un empleado asociado. No es posible hacer el corte.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val contado = binding.etCorteEfectivoContado.text
            ?.toString()
            ?.trim()
            ?.replace(',', '.')
            ?.toDoubleOrNull()
        if (contado == null || contado < 0) {
            Toast.makeText(requireContext(), "Ingresa el efectivo contado", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnFinalizarCorte.isEnabled = false
        binding.btnFinalizarCorte.text = "Procesando..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearCorte(
                    CrearCorteRequest(
                        aperturaCajaId = actual.id,
                        empleadoId = empleadoId,
                        efectivoContado = contado,
                        observaciones = binding.etCorteObservaciones.text
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                    )
                )

                val corte = response.dataExitosa()
                if (corte == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo registrar el corte",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Corte registrado")
                        .setMessage(
                            "La caja se cerró correctamente.\n\n" +
                                "Efectivo contado: ${corte.efectivoContado.formatearMonedaSinSufijo()}\n" +
                                "Ventas: ${corte.totalVentas.formatearMonedaSinSufijo()}\n" +
                                "Diferencia: ${corte.diferencia.formatearMonedaSinSufijo()}\n" +
                                "Fecha: ${formatearFecha(corte.fechaCorte, "dd/MM/yyyy HH:mm")}"
                        )
                        .setCancelable(false)
                        .setPositiveButton("Aceptar") { _, _ ->
                            parentFragmentManager.popBackStack()
                        }
                        .show()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                if (isAdded) {
                    binding.btnFinalizarCorte.isEnabled = true
                    binding.btnFinalizarCorte.text = "Finalizar Corte"
                }
            }
        }
    }

    private class SimpleRowHolder(
        itemView: View,
        val tvTitulo: TextView,
        val tvDetalle: TextView,
        val tvMonto: TextView
    ) : RecyclerView.ViewHolder(itemView)

    private fun crearFila(context: android.content.Context): SimpleRowHolder {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_bg_screen))
            radius = dpToPx(14).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
        }

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitulo = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        col.addView(tvTitulo)
        val tvDetalle = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
        }
        col.addView(tvDetalle)
        row.addView(col)

        val tvMonto = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        row.addView(tvMonto)
        card.addView(row)
        return SimpleRowHolder(card, tvTitulo, tvDetalle, tvMonto)
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()

    companion object {
        private const val ARG_APERTURA_ID = "apertura_id"

        fun newInstance(aperturaId: Int): CorteCajaFragment {
            return CorteCajaFragment().apply {
                arguments = Bundle().apply { putInt(ARG_APERTURA_ID, aperturaId) }
            }
        }
    }
}

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.AperturaCajaDto
import com.sprm.apexgym.core.network.api.MovimientoCajaDto
import com.sprm.apexgym.core.network.api.ResumenAperturaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMoneda
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentResumenAperturaBinding
import kotlinx.coroutines.launch

class ResumenAperturaFragment : BaseFragment<FragmentResumenAperturaBinding>() {

    private var aperturaActiva: AperturaCajaDto? = null

    override fun getLayoutRes(): Int = R.layout.fragment_resumen_apertura

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvResumenVentasMetodo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResumenUltimosMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResumenAlertas.layoutManager = LinearLayoutManager(requireContext())

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flRefresh.setOnClickListener { cargarResumen() }

        binding.tvVerTodosMovimientos.setOnClickListener {
            val apertura = aperturaActiva
            if (apertura == null) {
                Toast.makeText(requireContext(), "No hay una caja abierta", Toast.LENGTH_SHORT).show()
            } else {
                navegar(MovimientosCajaFragment.newInstance(apertura.id))
            }
        }

        binding.btnAccionAbrirCaja.setOnClickListener { navegar(AperturaCajaFragment()) }

        binding.btnAccionCorteCaja.setOnClickListener {
            val apertura = aperturaActiva
            if (apertura == null) {
                Toast.makeText(requireContext(), "Abre una caja antes de hacer el corte", Toast.LENGTH_SHORT).show()
            } else {
                navegar(CorteCajaFragment.newInstance(apertura.id))
            }
        }

        binding.btnAccionHistorialCortes.setOnClickListener { navegar(HistorialCortesFragment()) }
        binding.btnAccionConfigCaja.setOnClickListener { navegar(ConfiguracionCajaFragment()) }

        cargarResumen()
    }

    private fun cargarResumen() {
        binding.tvResumenCajaNombre.text = "Cargando caja…"
        binding.tvResumenAperturaInfo.text = ""
        binding.tvResumenSaldoActual.text = "…"
        binding.tvResumenVentasHoy.text = "…"
        binding.tvResumenTransacciones.text = "…"
        binding.tvResumenEfectivoEsperado.text = "…"
        binding.tvAlertCount.text = "—"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val aperturas = RetrofitClient.empleadoService.obtenerAperturas().dataExitosa()
                aperturaActiva = aperturas
                    ?.filter { it.estado?.uppercase() != "CERRADA" }
                    ?.maxByOrNull { it.id }

                val apertura = aperturaActiva
                if (apertura == null) {
                    mostrarSinApertura()
                    return@launch
                }

                val resumen = RetrofitClient.empleadoService
                    .obtenerResumenApertura(apertura.id)
                    .dataExitosa()

                if (resumen == null) {
                    binding.tvResumenCajaNombre.text = "Caja abierta #${apertura.id}"
                    binding.tvResumenAperturaInfo.text = "No se pudo cargar el resumen"
                    binding.tvResumenSaldoActual.text = "—"
                    binding.tvResumenVentasHoy.text = "—"
                    binding.tvResumenTransacciones.text = "—"
                    binding.tvResumenEfectivoEsperado.text = "—"
                    renderizarMovimientos(emptyList())
                    renderizarVentasMetodo(resumen)
                    renderizarAlertas(0)
                    return@launch
                }

                mostrarResumen(apertura, resumen)
            } catch (_: Exception) {
                binding.tvResumenCajaNombre.text = "Sin conexión"
                binding.tvResumenAperturaInfo.text = "No se pudo conectar con el servidor"
                binding.tvResumenSaldoActual.text = "—"
                binding.tvResumenVentasHoy.text = "—"
                binding.tvResumenTransacciones.text = "—"
                binding.tvResumenEfectivoEsperado.text = "—"
                binding.tvAlertCount.text = "—"
                renderizarMovimientos(emptyList())
                renderizarVentasMetodo(null)
                renderizarAlertas(0)
            }
        }
    }

    private fun mostrarSinApertura() {
        binding.tvResumenCajaNombre.text = "No hay caja abierta"
        binding.tvResumenAperturaInfo.text = "Abre una caja para comenzar a operar"
        binding.tvResumenSaldoActual.text = 0.0.formatearMonedaSinSufijo()
        binding.tvResumenVentasHoy.text = 0.0.formatearMonedaSinSufijo()
        binding.tvResumenTransacciones.text = "0"
        binding.tvResumenEfectivoEsperado.text = 0.0.formatearMonedaSinSufijo()
        binding.tvAlertCount.text = "0"
        renderizarMovimientos(emptyList())
        renderizarVentasMetodo(null)
        renderizarAlertas(0)
    }

    private fun mostrarResumen(apertura: AperturaCajaDto, resumen: ResumenAperturaDto) {
        binding.tvResumenCajaNombre.text = apertura.caja?.nombre
            ?.let { "Caja: $it" }
            ?: "Caja abierta #${apertura.id}"
        binding.tvResumenAperturaInfo.text = buildString {
            append("Abierta: ")
            append(formatearFecha(apertura.fechaApertura, "dd/MM/yyyy HH:mm"))
            append(" • Fondo: ")
            append(resumen.montoInicial.formatearMonedaSinSufijo())
        }

        val ventas = resumen.totales
        binding.tvResumenSaldoActual.text =
            resumen.saldoDisponible.formatearMonedaSinSufijo()
        binding.tvResumenVentasHoy.text =
            (ventas?.totalVentas ?: 0.0).formatearMonedaSinSufijo()
        binding.tvResumenTransacciones.text =
            (ventas?.transaccionesVentas ?: 0).toString()
        binding.tvResumenEfectivoEsperado.text =
            resumen.saldoDisponible.formatearMonedaSinSufijo()
        binding.tvAlertCount.text = resumen.movimientosPendientes.toString()

        renderizarVentasMetodo(resumen)
        renderizarMovimientos(resumen.movimientos.orEmpty().takeLast(5).reversed())
        renderizarAlertas(resumen.movimientosPendientes)
    }

    private fun renderizarVentasMetodo(resumen: ResumenAperturaDto?) {
        val metodos = resumen?.totales?.ventasPorMetodoPago.orEmpty()

        binding.rvResumenVentasMetodo.adapter = object : RecyclerView.Adapter<SimpleRowHolder>() {
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

    private fun renderizarMovimientos(movimientos: List<MovimientoCajaDto>) {
        binding.rvResumenUltimosMovimientos.adapter =
            object : RecyclerView.Adapter<SimpleRowHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleRowHolder {
                    return crearFila(parent.context)
                }

                override fun onBindViewHolder(holder: SimpleRowHolder, position: Int) {
                    if (movimientos.isEmpty()) {
                        holder.tvTitulo.text = "Sin movimientos registrados"
                        holder.tvDetalle.text = ""
                        holder.tvMonto.text = ""
                    } else {
                        val movimiento = movimientos[position]
                        holder.tvTitulo.text = movimiento.concepto ?: "Movimiento"
                        holder.tvDetalle.text = buildString {
                            append(movimiento.tipo ?: "")
                            append(" • ")
                            append(movimiento.estado ?: "")
                        }.trim(' ', '•')
                        holder.tvMonto.text = (if (movimiento.tipo?.uppercase() == "SALIDA") {
                            "-"
                        } else {
                            "+"
                        }) + movimiento.monto.formatearMonedaSinSufijo()
                    }
                }

                override fun getItemCount(): Int = movimientos.size.coerceAtLeast(1)
            }
    }

    private fun renderizarAlertas(pendientes: Int) {
        binding.rvResumenAlertas.adapter = object : RecyclerView.Adapter<SimpleRowHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleRowHolder {
                return crearFila(parent.context)
            }

            override fun onBindViewHolder(holder: SimpleRowHolder, position: Int) {
                holder.tvTitulo.text = if (pendientes > 0) {
                    "$pendientes movimiento(s) pendiente(s) de validar"
                } else {
                    "Sin movimientos pendientes"
                }
                holder.tvDetalle.text = if (pendientes > 0) {
                    "Valida los movimientos pendientes antes de realizar el corte"
                } else {
                    "Todos los movimientos de la apertura están validados"
                }
                holder.tvMonto.text = ""
            }

            override fun getItemCount(): Int = 1
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
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
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
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
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

    private fun navegar(fragment: androidx.fragment.app.Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()

    override fun onResume() {
        super.onResume()
        if (view != null) cargarResumen()
    }
}

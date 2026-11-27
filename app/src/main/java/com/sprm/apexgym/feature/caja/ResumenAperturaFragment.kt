package com.sprm.apexgym.feature.caja

import android.graphics.Typeface
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

    private var aperturasAbiertas: List<AperturaCajaDto> = emptyList()
    private var aperturaSeleccionadaId: Int? = null
    private var aperturaActiva: AperturaCajaDto? = null
    private var primerResume = true

    override fun getLayoutRes(): Int = R.layout.fragment_resumen_apertura

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        primerResume = true
        binding.rvResumenVentasMetodo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResumenUltimosMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResumenAlertas.layoutManager = LinearLayoutManager(requireContext())

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarResumen() }

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
        binding.tvAlertCount.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val aperturas = RetrofitClient.empleadoService.obtenerAperturas().dataExitosa().orEmpty()
                if (!isAdded || view == null) return@launch

                aperturasAbiertas = aperturas
                    .filter { CajaValidacionLogic.esAperturaAbierta(it.estado) }
                    .sortedByDescending { it.id }

                val elegida = aperturasAbiertas.firstOrNull { it.id == aperturaSeleccionadaId }
                    ?: aperturasAbiertas.firstOrNull()
                aperturaActiva = elegida
                aperturaSeleccionadaId = elegida?.id

                renderizarSelectorCajasAbiertas()

                if (elegida == null) {
                    mostrarSinApertura()
                    return@launch
                }

                cargarResumenDeApertura(elegida)
            } catch (_: Exception) {
                if (!isAdded || view == null) return@launch
                mostrarErrorConexion()
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private suspend fun cargarResumenDeApertura(apertura: AperturaCajaDto) {
        val responseResumen = RetrofitClient.empleadoService.obtenerResumenApertura(apertura.id)
        if (!isAdded || view == null) return

        val resumen = responseResumen.dataExitosa()
        if (resumen == null) {
            binding.tvResumenCajaNombre.text = apertura.caja?.nombre?.let { "Caja: $it" }
                ?: "Caja abierta #${apertura.id}"
            binding.tvResumenAperturaInfo.text =
                responseResumen.mensajeDeError() ?: "No se pudo cargar el resumen"
            binding.tvResumenSaldoActual.text = "—"
            binding.tvResumenVentasHoy.text = "—"
            binding.tvResumenTransacciones.text = "—"
            binding.tvResumenEfectivoEsperado.text = apertura.montoInicial.formatearMonedaSinSufijo()
            binding.tvAlertCount.visibility = View.GONE
            renderizarMovimientos(emptyList())
            renderizarVentasMetodo(null)
            renderizarAlertas(0)
            return
        }

        mostrarResumen(apertura, resumen)
    }

    private fun renderizarSelectorCajasAbiertas() {
        val container = binding.llOpenCajaChips
        container.removeAllViews()
        if (aperturasAbiertas.size <= 1) {
            binding.hsvOpenCajaChips.visibility = View.GONE
            return
        }

        binding.hsvOpenCajaChips.visibility = View.VISIBLE
        val context = requireContext()
        val blanco = ContextCompat.getColor(context, R.color.white)
        val primario = ContextCompat.getColor(context, R.color.apex_text_primary)

        aperturasAbiertas.forEachIndexed { index, item ->
            val seleccionada = item.id == aperturaActiva?.id
            val chip = TextView(context).apply {
                text = item.caja?.nombre?.takeIf { it.isNotBlank() } ?: "Caja #${item.cajaId}"
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dpToPx(16), 0, dpToPx(16), 0)
                setBackgroundResource(
                    if (seleccionada) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
                )
                setTextColor(if (seleccionada) blanco else primario)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(36)
                ).apply {
                    if (index > 0) marginStart = dpToPx(8)
                }
                setOnClickListener {
                    if (aperturaActiva?.id != item.id) {
                        aperturaActiva = item
                        aperturaSeleccionadaId = item.id
                        renderizarSelectorCajasAbiertas()
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                binding.swipeRefresh.isRefreshing = true
                                cargarResumenDeApertura(item)
                            } catch (_: Exception) {
                                if (isAdded && view != null) mostrarErrorConexion()
                            } finally {
                                if (isAdded && view != null) binding.swipeRefresh.isRefreshing = false
                            }
                        }
                    }
                }
            }
            container.addView(chip)
        }
    }

    private fun mostrarErrorConexion() {
        binding.tvResumenCajaNombre.text = "Sin conexión"
        binding.tvResumenAperturaInfo.text = "No se pudo conectar con el servidor"
        binding.tvResumenSaldoActual.text = "—"
        binding.tvResumenVentasHoy.text = "—"
        binding.tvResumenTransacciones.text = "—"
        binding.tvResumenEfectivoEsperado.text = "—"
        binding.tvAlertCount.visibility = View.GONE
        renderizarMovimientos(emptyList())
        renderizarVentasMetodo(null)
        renderizarAlertas(0)
    }

    private fun mostrarSinApertura() {
        binding.tvResumenCajaNombre.text = "No hay caja abierta"
        binding.tvResumenAperturaInfo.text = "Abre una caja para comenzar a operar"
        binding.tvResumenSaldoActual.text = 0.0.formatearMonedaSinSufijo()
        binding.tvResumenVentasHoy.text = 0.0.formatearMonedaSinSufijo()
        binding.tvResumenTransacciones.text = "0"
        binding.tvResumenEfectivoEsperado.text = 0.0.formatearMonedaSinSufijo()
        binding.tvAlertCount.visibility = View.GONE
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
            append(" • Saldo inicial: ")
            append(resumen.montoInicial.formatearMonedaSinSufijo())
        }

        val ventas = resumen.totales
        binding.tvResumenSaldoActual.text = resumen.saldoDisponible.formatearMonedaSinSufijo()
        binding.tvResumenVentasHoy.text = (ventas?.totalVentas ?: 0.0).formatearMonedaSinSufijo()
        binding.tvResumenTransacciones.text = (ventas?.transaccionesVentas ?: 0).toString()
        binding.tvResumenEfectivoEsperado.text = resumen.montoInicial.formatearMonedaSinSufijo()

        if (resumen.movimientosPendientes > 0) {
            binding.tvAlertCount.visibility = View.VISIBLE
            binding.tvAlertCount.text = resumen.movimientosPendientes.toString()
        } else {
            binding.tvAlertCount.visibility = View.GONE
        }

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
        binding.rvResumenUltimosMovimientos.adapter = object : RecyclerView.Adapter<SimpleRowHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleRowHolder {
                return crearFila(parent.context)
            }

            override fun onBindViewHolder(holder: SimpleRowHolder, position: Int) {
                if (movimientos.isEmpty()) {
                    holder.tvTitulo.text = "Sin movimientos en esta apertura"
                    holder.tvDetalle.text = ""
                    holder.tvMonto.text = ""
                } else {
                    val mov = movimientos[position]
                    val esSalida = mov.tipo?.uppercase() == "SALIDA"
                    holder.tvTitulo.text = mov.concepto ?: "Movimiento #${mov.id}"
                    holder.tvDetalle.text = listOfNotNull(
                        mov.tipo?.uppercase(),
                        mov.estado?.uppercase(),
                        mov.createdAt?.takeIf { it.isNotBlank() }?.let { formatearFecha(it, "dd/MM HH:mm") }
                    ).joinToString(" • ")
                    holder.tvMonto.text =
                        (if (esSalida) "-" else "+") + mov.monto.formatearMonedaSinSufijo()
                    holder.itemView.setOnClickListener {
                        aperturaActiva?.let { navegar(MovimientosCajaFragment.newInstance(it.id)) }
                    }
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
                    "Toca para revisar y validar antes de realizar el corte"
                } else {
                    "Todos los movimientos de la apertura están validados"
                }
                holder.tvMonto.text = if (pendientes > 0) "Revisar" else ""
                if (pendientes > 0) {
                    holder.itemView.isClickable = true
                    holder.itemView.setOnClickListener {
                        aperturaActiva?.let { navegar(MovimientosCajaFragment.newInstance(it.id)) }
                    }
                } else {
                    holder.itemView.isClickable = false
                    holder.itemView.setOnClickListener(null)
                }
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
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitulo = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, Typeface.BOLD)
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
            setTypeface(typeface, Typeface.BOLD)
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

    override fun onResume() {
        super.onResume()
        if (primerResume) {
            primerResume = false
        } else if (view != null) {
            cargarResumen()
        }
    }
}

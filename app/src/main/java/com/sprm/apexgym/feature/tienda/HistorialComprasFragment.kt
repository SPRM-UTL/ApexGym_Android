package com.sprm.apexgym.feature.tienda

import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.TransaccionPagoDto
import com.sprm.apexgym.core.network.api.VentaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentHistorialComprasBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class HistorialComprasFragment : BaseFragment<FragmentHistorialComprasBinding>() {

    private var ventas: List<VentaDto> = emptyList()
    private var transacciones: List<TransaccionPagoDto> = emptyList()
    private var filtroSeleccionado: String = "Todas"
    private var textoBusqueda: String = ""
    private val chips = mutableListOf<TextView>()
    private val adapter = TransaccionesAdapter()

    override fun getLayoutRes(): Int = R.layout.fragment_historial_compras

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flFilter.visibility = View.INVISIBLE
        binding.rvHistorialCompras.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorialCompras.adapter = adapter

        binding.etSearchPurchases.doAfterTextChanged { texto ->
            textoBusqueda = texto?.toString()?.trim().orEmpty()
            aplicarFiltros()
        }

        construirChips()
        cargarTransacciones()
    }

    private fun mostrarNoDisponible() {
        binding.tvTotalGastado.text = "—"
        binding.tvTotalCompras.text = "—"
        binding.tvEsteMesGasto.text = "—"
        binding.llFilterChips.visibility = View.GONE
        binding.rvHistorialCompras.visibility = View.GONE
        binding.llEmptyCompras.visibility = View.VISIBLE
        binding.etSearchPurchases.isEnabled = false
        binding.etSearchPurchases.hint = "No disponible para tu rol"
        textoEstado(binding.llEmptyCompras, 1)?.text = "Historial no disponible"
        textoEstado(binding.llEmptyCompras, 2)?.text =
            "El historial de ventas está disponible solo para empleados."
    }

    private fun cargarTransacciones() {
        binding.rvHistorialCompras.visibility = View.GONE
        binding.llEmptyCompras.visibility = View.VISIBLE
        textoEstado(binding.llEmptyCompras, 1)?.text = "Cargando compras…"
        textoEstado(binding.llEmptyCompras, 2)?.text = "Consultando las transacciones del servidor."
        binding.tvTotalGastado.text = "…"
        binding.tvTotalCompras.text = "…"
        binding.tvEsteMesGasto.text = "…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerVentas(limite = 50)
                ventas = response.dataExitosa()?.registros.orEmpty()
                transacciones = ventas.map { v ->
                    val primerDetalle = v.detalleVenta?.firstOrNull()
                    val nombreConcepto = primerDetalle?.let {
                        it.descripcion ?: it.producto?.nombre ?: it.servicio?.nombre ?: it.tipoMembresia?.nombre
                    } ?: "Compra #${v.id}"
                    val conceptoFinal = if ((v.detalleVenta?.size ?: 0) > 1) {
                        "$nombreConcepto (+${v.detalleVenta!!.size - 1} más)"
                    } else {
                        nombreConcepto
                    }
                    val proveedor = v.metodoPago?.codigo
                        ?: v.transaccionesPago?.firstOrNull()?.proveedor
                        ?: when (v.metodoPagoId) {
                            1 -> "TERMINAL_MANUAL"
                            2 -> "EFECTIVO"
                            3 -> "TRANSFERENCIA"
                            4 -> "MERCADO_PAGO"
                            else -> "EFECTIVO"
                        }
                    TransaccionPagoDto(
                        id = v.id,
                        monto = v.total,
                        estado = when (v.estado?.lowercase()) {
                            "completada", "completado" -> "APROBADO"
                            "pendiente_pago" -> "PENDIENTE"
                            "cancelada", "cancelado" -> "CANCELADO"
                            else -> v.estado?.uppercase() ?: "APROBADO"
                        },
                        concepto = conceptoFinal,
                        folioVoucher = v.folioVenta?.id?.let { "APX-$it" } ?: "APX-${v.id}",
                        proveedor = proveedor,
                        createdAt = v.createdAt
                    )
                }
            } catch (_: Exception) {
                ventas = emptyList()
                transacciones = emptyList()
            }

            if (!isAdded) return@launch

            actualizarResumen()
            aplicarFiltros()
        }
    }

    private fun actualizarResumen() {
        val aprobadas = transacciones.filter {
            it.estado.equals("APROBADO", ignoreCase = true)
        }
        val totalAprobado = aprobadas.sumOf { it.monto }
        val mesActual = SimpleDateFormat("yyyy-MM", Locale.US).format(System.currentTimeMillis())
        val totalMes = aprobadas
            .filter { it.createdAt?.startsWith(mesActual) == true }
            .sumOf { it.monto }

        binding.tvTotalGastado.text = totalAprobado.formatearMonedaSinSufijo()
        binding.tvTotalCompras.text = transacciones.size.toString()
        binding.tvEsteMesGasto.text = totalMes.formatearMonedaSinSufijo()
    }

    private fun construirChips() {
        binding.llFilterChips.removeAllViews()
        chips.clear()

        val tarjetaLabel = if (SessionManager.esEmpleado(requireContext())) "Terminal" else "Tarjeta"
        listOf("Todas", "Efectivo", tarjetaLabel, "Transferencia", "Mercado Pago", "Aprobadas", "Pendientes").forEachIndexed {
                indice,
                filtro ->
            val chip = TextView(requireContext()).apply {
                text = filtro
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(18), 0, dp(18), 0)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(38)
                ).apply {
                    if (indice > 0) marginStart = dp(8)
                }
                setOnClickListener {
                    filtroSeleccionado = filtro
                    actualizarEstiloChips()
                    aplicarFiltros()
                }
            }
            chips.add(chip)
            binding.llFilterChips.addView(chip)
        }

        actualizarEstiloChips()
    }

    private fun actualizarEstiloChips() {
        chips.forEach { chip ->
            if (chip.text.toString().equals(filtroSeleccionado, ignoreCase = true)) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.apex_text_primary)
                )
            }
        }
    }

    private fun aplicarFiltros() {
        val filtradas = transacciones.filter { transaccion ->
            val coincideFiltro = when (filtroSeleccionado) {
                "Efectivo" -> transaccion.proveedor.equals("EFECTIVO", ignoreCase = true)
                "Terminal", "Tarjeta" -> transaccion.proveedor.equals("TERMINAL_MANUAL", ignoreCase = true) ||
                    transaccion.proveedor.equals("TARJETA", ignoreCase = true)
                "Transferencia" -> transaccion.proveedor.equals("TRANSFERENCIA", ignoreCase = true)
                "Mercado Pago" -> transaccion.proveedor.equals("MERCADO_PAGO", ignoreCase = true)
                "Aprobadas" -> transaccion.estado.equals("APROBADO", ignoreCase = true)
                "Pendientes" -> transaccion.estado.equals("PENDIENTE", ignoreCase = true)
                else -> true
            }

            val coincideBusqueda = textoBusqueda.isEmpty() ||
                transaccion.concepto?.contains(textoBusqueda, ignoreCase = true) == true ||
                transaccion.id.toString().contains(textoBusqueda, ignoreCase = true) ||
                transaccion.folioVoucher?.contains(textoBusqueda, ignoreCase = true) == true ||
                transaccion.proveedor?.contains(textoBusqueda, ignoreCase = true) == true

            coincideFiltro && coincideBusqueda
        }

        adapter.actualizar(filtradas)

        val vacio = filtradas.isEmpty()
        binding.rvHistorialCompras.visibility = if (vacio) View.GONE else View.VISIBLE
        binding.llEmptyCompras.visibility = if (vacio) View.VISIBLE else View.GONE

        if (!vacio) return

        if (transacciones.isEmpty()) {
            textoEstado(binding.llEmptyCompras, 1)?.text = "No hay compras registradas"
            textoEstado(binding.llEmptyCompras, 2)?.text =
                "Las ventas cobradas desde la tienda aparecerán aquí."
        } else {
            textoEstado(binding.llEmptyCompras, 1)?.text = "Sin resultados"
            textoEstado(binding.llEmptyCompras, 2)?.text =
                "No hay compras que coincidan con el filtro o la búsqueda."
        }
    }

    private fun abrirTicket(transaccion: TransaccionPagoDto) {
        val ventaAsociada = ventas.firstOrNull { it.id == transaccion.id }
        val detalles = ventaAsociada?.detalleVenta
        val itemsJson = if (!detalles.isNullOrEmpty()) {
            JSONArray().apply {
                detalles.forEach { d ->
                    val nombre = d.descripcion
                        ?: d.producto?.nombre
                        ?: d.servicio?.nombre
                        ?: d.tipoMembresia?.nombre
                        ?: "Artículo"
                    put(JSONObject().apply {
                        put("nombre", nombre)
                        put("cantidad", d.cantidad)
                        put("subtotal", d.subtotal ?: (transaccion.monto / detalles.size))
                    })
                }
            }.toString()
        } else null

        val metodoTexto = when (transaccion.proveedor?.uppercase()) {
            "EFECTIVO" -> "Pago en recepción / Efectivo"
            "MERCADO_PAGO" -> "Mercado Pago / Pago en línea"
            "TERMINAL_MANUAL", "TARJETA" -> "Tarjeta de débito"
            "TRANSFERENCIA" -> "Transferencia bancaria"
            else -> transaccion.proveedor ?: "Efectivo"
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                TicketCompraFragment.newInstance(
                    transaccion = transaccion,
                    itemsJson = itemsJson,
                    metodoPago = metodoTexto,
                    subtotal = ventaAsociada?.subtotal?.takeIf { it > 0.0 },
                    iva = ventaAsociada?.let { v ->
                        if (v.subtotal > 0.0) (v.total - v.subtotal).coerceAtLeast(0.0) else null
                    }
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun etiquetaProveedor(proveedor: String?): String = when (proveedor?.uppercase()) {
        "EFECTIVO" -> "Efectivo"
        "TERMINAL_MANUAL" -> "Terminal manual"
        "MERCADO_PAGO" -> "Mercado Pago"
        null -> "Sin método"
        else -> proveedor.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun textoEstado(contenedor: LinearLayout, indice: Int): TextView? =
        contenedor.getChildAt(indice) as? TextView

    private inner class TransaccionesAdapter : RecyclerView.Adapter<TransaccionViewHolder>() {

        private var items: List<TransaccionPagoDto> = emptyList()

        fun actualizar(nuevos: List<TransaccionPagoDto>) {
            items = nuevos
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransaccionViewHolder {
            val context = parent.context
            val card = MaterialCardView(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                radius = dp(16).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
            }

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }

            val iconBox = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                setBackgroundResource(R.drawable.bg_icon_circle_orange_outline)
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                        setImageResource(R.drawable.ic_shopping_cart)
                        setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                    }
                )
            }
            row.addView(iconBox)

            val infoCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply {
                        marginStart = dp(12)
                        marginEnd = dp(8)
                    }
            }

            val tvConcepto = TextView(context).apply {
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            infoCol.addView(tvConcepto)

            val tvDetalle = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(3) }
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            infoCol.addView(tvDetalle)
            row.addView(infoCol)

            val rightCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }

            val tvMonto = TextView(context).apply {
                gravity = Gravity.END
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            rightCol.addView(tvMonto)

            val tvEstado = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(3) }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            rightCol.addView(tvEstado)
            row.addView(rightCol)

            card.addView(row)
            return TransaccionViewHolder(card, tvConcepto, tvDetalle, tvMonto, tvEstado)
        }

        override fun onBindViewHolder(holder: TransaccionViewHolder, position: Int) {
            val transaccion = items[position]
            holder.tvConcepto.text = transaccion.concepto?.takeIf { it.isNotBlank() }
                ?: "Venta #${transaccion.id}"
            holder.tvDetalle.text = buildString {
                append(formatearFecha(transaccion.createdAt))
                append(" • ")
                append(etiquetaProveedor(transaccion.proveedor))
            }
            holder.tvMonto.text = transaccion.monto.formatearMonedaSinSufijo()
            holder.tvEstado.text = transaccion.estado ?: "—"
            holder.tvEstado.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    when (transaccion.estado?.uppercase()) {
                        "APROBADO" -> R.color.apex_text_secondary
                        "PENDIENTE" -> R.color.apex_orange
                        else -> R.color.apex_nav_inactive
                    }
                )
            )
            holder.card.setOnClickListener { abrirTicket(transaccion) }
        }

        override fun getItemCount(): Int = items.size
    }

    private class TransaccionViewHolder(
        val card: MaterialCardView,
        val tvConcepto: TextView,
        val tvDetalle: TextView,
        val tvMonto: TextView,
        val tvEstado: TextView
    ) : RecyclerView.ViewHolder(card)

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()
}

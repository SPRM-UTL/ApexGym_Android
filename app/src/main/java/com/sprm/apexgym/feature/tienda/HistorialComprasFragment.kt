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
    private var paginaActual: Int = 1
    private val chips = mutableListOf<TextView>()
    private val adapter = TransaccionesAdapter()

    override fun getLayoutRes(): Int = R.layout.fragment_historial_compras

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val esEmpleado = SessionManager.esEmpleado(requireContext())
        binding.tvHistorialTitle.text = if (esEmpleado) "Historial de Ventas" else "Historial de Compras"
        binding.etSearchPurchases.hint = if (esEmpleado) {
            "Buscar venta, cliente, producto o ticket..."
        } else {
            "Buscar compra, producto o ticket..."
        }

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flFilter.visibility = View.INVISIBLE
        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarTransacciones() }

        binding.rvHistorialCompras.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorialCompras.adapter = adapter

        binding.btnPaginaAnterior.setOnClickListener {
            if (paginaActual > 1) {
                paginaActual -= 1
                aplicarFiltros()
            }
        }
        binding.btnPaginaSiguiente.setOnClickListener {
            paginaActual += 1
            aplicarFiltros()
        }

        binding.etSearchPurchases.doAfterTextChanged { texto ->
            textoBusqueda = texto?.toString()?.trim().orEmpty()
            paginaActual = 1
            aplicarFiltros()
        }

        construirChips()
        cargarTransacciones()
    }

    private fun cargarTransacciones() {
        val esEmpleado = SessionManager.esEmpleado(requireContext())
        binding.rvHistorialCompras.visibility = View.GONE
        binding.llPaginacionHistorial.visibility = View.GONE
        binding.llEmptyCompras.visibility = View.VISIBLE
        textoEstado(binding.llEmptyCompras, 1)?.text =
            if (esEmpleado) "Cargando ventas…" else "Cargando compras…"
        textoEstado(binding.llEmptyCompras, 2)?.text = "Consultando los registros del servidor."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerVentas(limite = 100)
                if (!isAdded || view == null) return@launch
                ventas = response.dataExitosa()?.registros.orEmpty()
                transacciones = ventas.map { v ->
                    val primerDetalle = v.detalleVenta?.firstOrNull()
                    val nombreConcepto = primerDetalle?.let {
                        it.descripcion ?: it.producto?.nombre ?: it.servicio?.nombre ?: it.tipoMembresia?.nombre
                    } ?: "Venta #${v.id}"
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
                    val folioNum = v.folioVenta?.id ?: v.id
                    TransaccionPagoDto(
                        id = v.id,
                        monto = v.total,
                        estado = when (v.estado?.lowercase()) {
                            "completada", "completado", "pagado" -> "APROBADO"
                            "pendiente_pago", "pendiente" -> "PENDIENTE"
                            "cancelada", "cancelado" -> "CANCELADO"
                            else -> v.estado?.uppercase() ?: "APROBADO"
                        },
                        concepto = conceptoFinal,
                        folioVoucher = "APX-${folioNum.toString().padStart(5, '0')}",
                        proveedor = proveedor,
                        createdAt = v.createdAt
                    )
                }
            } catch (_: Exception) {
                if (!isAdded || view == null) return@launch
                ventas = emptyList()
                transacciones = emptyList()
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }

            if (!isAdded || view == null) return@launch
            paginaActual = 1
            aplicarFiltros()
        }
    }

    private fun construirChips() {
        binding.llFilterChips.removeAllViews()
        chips.clear()

        val tarjetaLabel = if (SessionManager.esEmpleado(requireContext())) "Terminal" else "Tarjeta"
        listOf("Todas", "Efectivo", tarjetaLabel, "Transferencia", "Mercado Pago", "Aprobadas", "Pendientes")
            .forEachIndexed { indice, filtro ->
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
                        paginaActual = 1
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
        val ventasPorId = ventas.associateBy { it.id }
        val filtradas = transacciones.filter { transaccion ->
            val ventaAsociada = ventasPorId[transaccion.id]
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
                transaccion.proveedor?.contains(textoBusqueda, ignoreCase = true) == true ||
                ventaAsociada?.cliente?.nombreCompleto?.contains(textoBusqueda, ignoreCase = true) == true

            coincideFiltro && coincideBusqueda
        }

        val totalPaginas = ((filtradas.size + TAMANO_PAGINA - 1) / TAMANO_PAGINA).coerceAtLeast(1)
        paginaActual = paginaActual.coerceIn(1, totalPaginas)
        val paginaItems = filtradas.drop((paginaActual - 1) * TAMANO_PAGINA).take(TAMANO_PAGINA)

        adapter.actualizar(paginaItems)

        val vacio = filtradas.isEmpty()
        binding.rvHistorialCompras.visibility = if (vacio) View.GONE else View.VISIBLE
        binding.llEmptyCompras.visibility = if (vacio) View.VISIBLE else View.GONE

        if (!vacio && filtradas.size > TAMANO_PAGINA) {
            binding.llPaginacionHistorial.visibility = View.VISIBLE
            binding.tvInfoPaginacion.text = "Página $paginaActual de $totalPaginas"
            binding.btnPaginaAnterior.isEnabled = paginaActual > 1
            binding.btnPaginaAnterior.alpha = if (paginaActual > 1) 1f else 0.45f
            binding.btnPaginaSiguiente.isEnabled = paginaActual < totalPaginas
            binding.btnPaginaSiguiente.alpha = if (paginaActual < totalPaginas) 1f else 0.45f
        } else {
            binding.llPaginacionHistorial.visibility = View.GONE
        }

        if (!vacio) return

        val esEmpleado = SessionManager.esEmpleado(requireContext())
        if (transacciones.isEmpty()) {
            textoEstado(binding.llEmptyCompras, 1)?.text =
                if (esEmpleado) "No hay ventas registradas" else "No hay compras registradas"
            textoEstado(binding.llEmptyCompras, 2)?.text = if (esEmpleado) {
                "Las ventas cobradas desde la tienda aparecerán aquí."
            } else {
                "Tus compras en tienda aparecerán aquí."
            }
        } else {
            textoEstado(binding.llEmptyCompras, 1)?.text = "Sin resultados"
            textoEstado(binding.llEmptyCompras, 2)?.text =
                "No hay registros que coincidan con el filtro o la búsqueda."
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

        val metodoTexto = ventaAsociada?.metodoPago?.nombre?.takeIf { it.isNotBlank() }
            ?: when (transaccion.proveedor?.uppercase()) {
                "EFECTIVO" -> "Efectivo"
                "MERCADO_PAGO" -> "Mercado Pago"
                "TERMINAL_MANUAL", "TARJETA" -> "Terminal / Tarjeta"
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
                    },
                    clienteNombre = ventaAsociada?.cliente?.nombreCompleto?.takeIf { it.isNotBlank() },
                    empleadoNombre = ventaAsociada?.empleado?.nombreCompleto?.takeIf { it.isNotBlank() }
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun etiquetaProveedor(proveedor: String?): String = when (proveedor?.uppercase()) {
        "EFECTIVO" -> "Efectivo"
        "TERMINAL_MANUAL", "TARJETA" -> "Terminal / Tarjeta"
        "MERCADO_PAGO" -> "Mercado Pago"
        "TRANSFERENCIA" -> "Transferencia"
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
            val venta = ventas.firstOrNull { it.id == transaccion.id }
            val clienteNombre = venta?.cliente?.nombreCompleto?.takeIf { it.isNotBlank() }

            holder.tvConcepto.text = transaccion.concepto?.takeIf { it.isNotBlank() }
                ?: "Venta #${transaccion.id}"
            holder.tvDetalle.text = buildString {
                if (clienteNombre != null) {
                    append(clienteNombre)
                    append(" • ")
                }
                append(formatearFecha(transaccion.createdAt))
                append(" • ")
                append(etiquetaProveedor(transaccion.proveedor))
            }
            holder.tvMonto.text = transaccion.monto.formatearMonedaSinSufijo()
            holder.tvEstado.text = when (transaccion.estado?.uppercase()) {
                "APROBADO" -> "Aprobado"
                "PENDIENTE" -> "Pendiente"
                "CANCELADO" -> "Cancelado"
                else -> transaccion.estado ?: "—"
            }
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

    companion object {
        private const val TAMANO_PAGINA = 10
    }
}

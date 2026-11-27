package com.sprm.apexgym.feature.tienda

import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.EmpresaTicketConfig
import com.sprm.apexgym.core.network.api.TransaccionPagoDto
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.core.ui.TicketImageSharer
import kotlinx.coroutines.launch
import org.json.JSONArray

class TicketCompraFragment : Fragment(R.layout.fragment_ticket_compra) {

    private data class ItemTicket(
        val nombre: String,
        val cantidad: Int,
        val subtotal: Double
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: Bundle()
        val id = args.getInt(ARG_ID, 0)
        val concepto = args.getString(ARG_CONCEPTO).orEmpty().ifBlank { "Venta ApexGym" }
        val monto = args.getDouble(ARG_MONTO, 0.0)
        val estadoRaw = args.getString(ARG_ESTADO).orEmpty().ifBlank { "APROBADO" }
        val estadoLegible = formatearEstado(estadoRaw)
        val proveedor = args.getString(ARG_PROVEEDOR).orEmpty().ifBlank { "EFECTIVO" }
        val folio = args.getString(ARG_FOLIO)
        val fecha = args.getString(ARG_FECHA)
        val itemsJson = args.getString(ARG_ITEMS_JSON)
        val clienteNombreArg = args.getString(ARG_CLIENTE_NOMBRE)?.takeIf { it.isNotBlank() }
        val empleadoNombreArg = args.getString(ARG_EMPLEADO_NOMBRE)?.takeIf { it.isNotBlank() }

        val subtotalArg = if (args.containsKey(ARG_SUBTOTAL)) args.getDouble(ARG_SUBTOTAL) else Double.NaN
        val ivaArg = if (args.containsKey(ARG_IVA)) args.getDouble(ARG_IVA) else Double.NaN

        val tvTicketNumber = view.findViewById<TextView>(R.id.tvTicketNumber)
        val tvTicketDate = view.findViewById<TextView>(R.id.tvTicketDate)
        val tvTicketClient = view.findViewById<TextView>(R.id.tvTicketClient)
        val tvTicketMembership = view.findViewById<TextView>(R.id.tvTicketMembership)
        val tvTicketTotal = view.findViewById<TextView>(R.id.tvTicketTotal)
        val tvTicketSubtotal = view.findViewById<TextView>(R.id.tvTicketSubtotal)
        val tvTicketIgv = view.findViewById<TextView>(R.id.tvTicketIgv)
        val tvTicketDescuento = view.findViewById<TextView>(R.id.tvTicketDescuento)
        val rvTicketItems = view.findViewById<RecyclerView>(R.id.rvTicketItems)
        val rvTicketPayments = view.findViewById<RecyclerView>(R.id.rvTicketPayments)

        val items = parseItems(itemsJson).ifEmpty {
            listOf(ItemTicket(concepto, 1, monto))
        }
        val sumaItems = items.sumOf { it.subtotal }
        val subtotalFinal = when {
            subtotalArg.isFinite() && subtotalArg > 0.0 -> subtotalArg
            sumaItems > 0.0 && sumaItems <= monto + 0.01 -> sumaItems
            else -> CalculoVentaPos.redondearDosDecimales(monto / (1.0 + CalculoVentaPos.TASA_IVA))
        }
        val ivaFinal = when {
            ivaArg.isFinite() && ivaArg >= 0.0 -> ivaArg
            else -> CalculoVentaPos.redondearDosDecimales((monto - subtotalFinal).coerceAtLeast(0.0))
        }

        val esEmpleado = SessionManager.esEmpleado(requireContext())
        val nombreUsuario = SessionManager.obtenerNombre(requireContext())
        val folioMostrado = if (!folio.isNullOrBlank() && folio.startsWith("APX-", ignoreCase = true)) {
            "#${folio.removePrefix("#")}"
        } else {
            "#APX-${id.toString().padStart(5, '0')}"
        }
        val folioVoucherPago = folio?.takeIf {
            it.isNotBlank() && !it.equals(folioMostrado.removePrefix("#"), ignoreCase = true)
        }

        tvTicketNumber?.text = folioMostrado
        tvTicketDate?.text = formatearFecha(fecha)
        tvTicketClient?.text = clienteNombreArg ?: when {
            esEmpleado -> "Cliente en mostrador"
            !nombreUsuario.isNullOrBlank() -> nombreUsuario
            else -> "Socio ApexGym"
        }
        tvTicketMembership?.text = when {
            empleadoNombreArg != null -> "Atendido por: $empleadoNombreArg"
            esEmpleado -> "Atendido por: ${nombreUsuario ?: "Empleado ApexGym"}"
            else -> "Compra en aplicación"
        }

        tvTicketSubtotal?.text = "${subtotalFinal.formatearMonedaSinSufijo()} MXN"
        tvTicketIgv?.text = "${ivaFinal.formatearMonedaSinSufijo()} MXN"
        tvTicketTotal?.text = "${monto.formatearMonedaSinSufijo()} MXN"
        tvTicketDescuento?.text = "- $0.00"
        view.findViewById<View>(R.id.llTicketIgvRow)?.visibility =
            if (ivaFinal > 0.0) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.llTicketDescuentoRow)?.visibility = View.GONE
        view.findViewById<View>(R.id.vDividerQr)?.visibility = View.GONE
        view.findViewById<View>(R.id.llQrVerificationSection)?.visibility = View.GONE

        rvTicketItems?.layoutManager = LinearLayoutManager(requireContext())
        rvTicketItems?.adapter = ItemsAdapter(items)
        rvTicketItems?.isNestedScrollingEnabled = false

        val metodoLegible = etiquetaProveedor(proveedor)
        rvTicketPayments?.layoutManager = LinearLayoutManager(requireContext())
        rvTicketPayments?.adapter = PaymentsAdapter(
            listOf(
                PagoTicket(
                    metodo = metodoLegible,
                    estado = estadoLegible,
                    folio = folioVoucherPago,
                    monto = monto
                )
            )
        )
        rvTicketPayments?.isNestedScrollingEnabled = false

        val tvTicketFooterMessage = view.findViewById<TextView>(R.id.tvTicketFooterMessage)
        var mensajePieTicket = EmpresaTicketConfig.obtenerMensajeInicial()
        tvTicketFooterMessage?.text = mensajePieTicket
        viewLifecycleOwner.lifecycleScope.launch {
            val mensajeRemoto = EmpresaTicketConfig.obtenerMensajeTicket()
            if (!isAdded || getView() == null) return@launch
            mensajePieTicket = mensajeRemoto
            tvTicketFooterMessage?.text = mensajePieTicket
        }

        view.findViewById<View>(R.id.flClose)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.flShare)?.setOnClickListener {
            val textoRespaldo = buildString {
                appendLine("ApexGym — Ticket $folioMostrado")
                appendLine("Fecha: ${formatearFecha(fecha)}")
                clienteNombreArg?.let { appendLine("Cliente: $it") }
                appendLine("Concepto: $concepto")
                appendLine("Método: $metodoLegible")
                appendLine("Estado: $estadoLegible")
                folioVoucherPago?.let { appendLine("Referencia: $it") }
                appendLine("Subtotal: ${subtotalFinal.formatearMonedaSinSufijo()} MXN")
                if (ivaFinal > 0.0) {
                    appendLine("IVA (16%): ${ivaFinal.formatearMonedaSinSufijo()} MXN")
                }
                appendLine("Total: ${monto.formatearMonedaSinSufijo()} MXN")
                append(mensajePieTicket)
            }
            TicketImageSharer.compartirComprobanteComoImagen(
                context = requireContext(),
                cardView = view.findViewById(R.id.cvTicket),
                nombreArchivoBase = "ticket_${folioMostrado.removePrefix("#")}",
                tituloChooser = "Compartir ticket",
                asunto = "Ticket $folioMostrado - ApexGym",
                textoRespaldo = textoRespaldo
            )
        }

        view.findViewById<View>(R.id.btnDownloadTicket)?.visibility = View.GONE
        view.findViewById<View>(R.id.btnPrintTicket)?.visibility = View.GONE
    }

    private fun parseItems(itemsJson: String?): List<ItemTicket> {
        if (itemsJson.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(itemsJson)
            buildList {
                for (indice in 0 until array.length()) {
                    val obj = array.getJSONObject(indice)
                    add(
                        ItemTicket(
                            nombre = obj.optString("nombre", "Artículo"),
                            cantidad = obj.optInt("cantidad", 1),
                            subtotal = obj.optDouble("subtotal", 0.0)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun etiquetaProveedor(proveedor: String): String {
        val limpio = proveedor.trim()
        return when (limpio.uppercase()) {
            "EFECTIVO" -> "Efectivo"
            "TERMINAL_MANUAL", "TARJETA" -> "Terminal / Tarjeta"
            "MERCADO_PAGO" -> "Mercado Pago"
            "TRANSFERENCIA" -> "Transferencia bancaria"
            else -> if ('_' in limpio && ' ' !in limpio) {
                limpio.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            } else {
                limpio
            }
        }
    }

    private fun formatearEstado(estado: String): String = when (estado.trim().uppercase()) {
        "APROBADO", "COMPLETADA", "COMPLETADO", "PAGADO" -> "Aprobado"
        "PENDIENTE", "PENDIENTE_PAGO" -> "Pendiente"
        "CANCELADO", "CANCELADA", "RECHAZADO" -> "Cancelado"
        else -> estado.lowercase().replaceFirstChar { it.uppercase() }
    }

    private inner class ItemsAdapter(
        private val items: List<ItemTicket>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val context = parent.context
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
            }

            val tvNombre = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            row.addView(tvNombre)

            val tvSubtotal = TextView(context).apply {
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            row.addView(tvSubtotal)

            return ViewHolderItems(row)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val itemHolder = holder as ViewHolderItems
            val item = items[position]
            itemHolder.nombreView.text = if (item.cantidad > 1) {
                "${item.nombre} × ${item.cantidad}"
            } else {
                item.nombre
            }
            itemHolder.subtotalView.text = "${item.subtotal.formatearMonedaSinSufijo()} MXN"
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolderItems(val row: LinearLayout) : RecyclerView.ViewHolder(row) {
            val nombreView: TextView = row.getChildAt(0) as TextView
            val subtotalView: TextView = row.getChildAt(1) as TextView
        }
    }

    private data class PagoTicket(
        val metodo: String,
        val estado: String,
        val folio: String?,
        val monto: Double
    )

    private inner class PaymentsAdapter(
        private val pagos: List<PagoTicket>
    ) : RecyclerView.Adapter<PaymentsAdapter.PagoViewHolder>() {

        inner class PagoViewHolder(val row: LinearLayout) : RecyclerView.ViewHolder(row) {
            val tvMetodo: TextView = row.getChildAt(0) as TextView
            val tvDetalle: TextView = row.getChildAt(1) as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoViewHolder {
            val context = parent.context
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(6) }
            }

            row.addView(
                TextView(context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    setTypeface(typeface, Typeface.BOLD)
                }
            )
            row.addView(
                TextView(context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                }
            )
            return PagoViewHolder(row)
        }

        override fun onBindViewHolder(holder: PagoViewHolder, position: Int) {
            val pago = pagos[position]
            holder.tvMetodo.text = "${pago.metodo} • ${pago.monto.formatearMonedaSinSufijo()} MXN"
            holder.tvDetalle.text = buildString {
                append("Estado: ${pago.estado}")
                pago.folio?.takeIf { it.isNotBlank() }?.let { append(" • Folio: $it") }
            }
        }

        override fun getItemCount(): Int = pagos.size
    }

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_CONCEPTO = "arg_concepto"
        private const val ARG_MONTO = "arg_monto"
        private const val ARG_ESTADO = "arg_estado"
        private const val ARG_PROVEEDOR = "arg_proveedor"
        private const val ARG_FOLIO = "arg_folio"
        private const val ARG_FECHA = "arg_fecha"
        private const val ARG_ITEMS_JSON = "arg_items_json"
        private const val ARG_SUBTOTAL = "arg_subtotal"
        private const val ARG_IVA = "arg_iva"
        private const val ARG_CLIENTE_NOMBRE = "arg_cliente_nombre"
        private const val ARG_EMPLEADO_NOMBRE = "arg_empleado_nombre"

        fun newInstance(
            transaccion: TransaccionPagoDto,
            itemsJson: String? = null,
            metodoPago: String? = null,
            subtotal: Double? = null,
            iva: Double? = null,
            clienteNombre: String? = null,
            empleadoNombre: String? = null
        ): TicketCompraFragment {
            return TicketCompraFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, transaccion.id)
                    putString(ARG_CONCEPTO, transaccion.concepto)
                    putDouble(ARG_MONTO, transaccion.monto)
                    putString(ARG_ESTADO, transaccion.estado)
                    putString(ARG_PROVEEDOR, metodoPago ?: transaccion.proveedor)
                    putString(ARG_FOLIO, transaccion.folioVoucher)
                    putString(ARG_FECHA, transaccion.createdAt)
                    putString(ARG_ITEMS_JSON, itemsJson)
                    if (subtotal != null) putDouble(ARG_SUBTOTAL, subtotal)
                    if (iva != null) putDouble(ARG_IVA, iva)
                    if (!clienteNombre.isNullOrBlank()) putString(ARG_CLIENTE_NOMBRE, clienteNombre)
                    if (!empleadoNombre.isNullOrBlank()) putString(ARG_EMPLEADO_NOMBRE, empleadoNombre)
                }
            }
        }
    }
}

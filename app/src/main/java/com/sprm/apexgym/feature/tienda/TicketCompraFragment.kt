package com.sprm.apexgym.feature.tienda

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.api.TransaccionPagoDto
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.session.SessionManager
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
        val estado = args.getString(ARG_ESTADO).orEmpty().ifBlank { "APROBADO" }
        val proveedor = args.getString(ARG_PROVEEDOR).orEmpty().ifBlank { "EFECTIVO" }
        val folio = args.getString(ARG_FOLIO)
        val fecha = args.getString(ARG_FECHA)
        val itemsJson = args.getString(ARG_ITEMS_JSON)

        val tvTicketNumber = view.findViewById<TextView>(R.id.tvTicketNumber)
        val tvTicketDate = view.findViewById<TextView>(R.id.tvTicketDate)
        val tvTicketClient = view.findViewById<TextView>(R.id.tvTicketClient)
        val tvTicketMembership = view.findViewById<TextView>(R.id.tvTicketMembership)
        val tvTicketTotal = view.findViewById<TextView>(R.id.tvTicketTotal)
        val tvTicketSubtotal = view.findViewById<TextView>(R.id.tvTicketSubtotal)
        val tvTicketDescuento = view.findViewById<TextView>(R.id.tvTicketDescuento)
        val rvTicketItems = view.findViewById<RecyclerView>(R.id.rvTicketItems)
        val rvTicketPayments = view.findViewById<RecyclerView>(R.id.rvTicketPayments)

        tvTicketNumber?.text = "#APX-${id.toString().padStart(5, '0')}"
        tvTicketDate?.text = formatearFecha(fecha)
        tvTicketClient?.text = "Venta en mostrador"
        tvTicketMembership?.text = buildString {
            append("Atendido por: ")
            append(SessionManager.obtenerNombre(requireContext()) ?: "Empleado ApexGym")
        }
        tvTicketSubtotal?.text = "${monto.formatearMonedaSinSufijo()} MXN"
        tvTicketTotal?.text = "${monto.formatearMonedaSinSufijo()} MXN"
        tvTicketDescuento?.text = "- $0.00"
        view.findViewById<View>(R.id.llTicketIgvRow)?.visibility = View.GONE
        view.findViewById<View>(R.id.llQrVerificationSection)?.visibility = View.GONE

        val items = parseItems(itemsJson).ifEmpty {
            listOf(ItemTicket(concepto, 1, monto))
        }

        rvTicketItems?.layoutManager = LinearLayoutManager(requireContext())
        rvTicketItems?.adapter = ItemsAdapter(items)
        rvTicketItems?.isNestedScrollingEnabled = false

        rvTicketPayments?.layoutManager = LinearLayoutManager(requireContext())
        rvTicketPayments?.adapter = PaymentsAdapter(
            listOf(
                PagoTicket(
                    metodo = etiquetaProveedor(proveedor),
                    estado = estado,
                    folio = folio,
                    monto = monto
                )
            )
        )
        rvTicketPayments?.isNestedScrollingEnabled = false

        view.findViewById<View>(R.id.flClose)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.flShare)?.setOnClickListener {
            val texto = buildString {
                appendLine("ApexGym — Ticket #APX-${id.toString().padStart(5, '0')}")
                appendLine("Fecha: ${formatearFecha(fecha)}")
                appendLine("Concepto: $concepto")
                appendLine("Método: ${etiquetaProveedor(proveedor)}")
                appendLine("Estado: $estado")
                folio?.takeIf { it.isNotBlank() }?.let { appendLine("Folio: $it") }
                append("Total: ${monto.formatearMonedaSinSufijo()} MXN")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, texto)
            }
            startActivity(Intent.createChooser(intent, "Compartir ticket"))
        }

        val noDisponible = View.OnClickListener {
            Toast.makeText(
                requireContext(),
                "La API actual no permite descargar ni imprimir tickets.",
                Toast.LENGTH_SHORT
            ).show()
        }
        view.findViewById<View>(R.id.btnDownloadTicket)?.setOnClickListener(noDisponible)
        view.findViewById<View>(R.id.btnPrintTicket)?.setOnClickListener(noDisponible)
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

    private fun etiquetaProveedor(proveedor: String): String = when (proveedor.uppercase()) {
        "EFECTIVO" -> "Efectivo"
        "TERMINAL_MANUAL" -> "Terminal manual"
        "MERCADO_PAGO" -> "Mercado Pago"
        else -> proveedor.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
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

        fun newInstance(
            transaccion: TransaccionPagoDto,
            itemsJson: String? = null,
            metodoPago: String? = null
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
                }
            }
        }
    }
}

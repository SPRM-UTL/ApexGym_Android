package com.sprm.apexgym.feature.inventario

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.AlmacenDto
import com.sprm.apexgym.core.network.api.CrearPedidoCompraRequest
import com.sprm.apexgym.core.network.api.DetallePedidoCompraRequest
import com.sprm.apexgym.core.network.api.PedidoCompraDto
import com.sprm.apexgym.core.network.client.ProductoClienteDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMoneda
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.network.mensajeUsuario
import com.sprm.apexgym.databinding.FragmentPedidosCompraBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Permite a empleados levantar pedidos de compra desde Android.
 * Los pedidos quedan en estado PENDIENTE hasta que se validan en Web para sumarse al inventario.
 */
class PedidosCompraFragment : BaseFragment<FragmentPedidosCompraBinding>() {

    private var productos: List<ProductoClienteDto> = emptyList()
    private var productosFiltrados: List<ProductoClienteDto> = emptyList()
    private var pedidos: List<PedidoCompraDto> = emptyList()
    private var almacenes: List<AlmacenDto> = emptyList()
    private val cantidadesSeleccionadas = linkedMapOf<Int, Int>()
    private var textoBusqueda = ""

    private lateinit var productoAdapter: ProductoPedidoAdapter
    private val historialAdapter = HistorialPedidosAdapter()

    override fun getLayoutRes(): Int = R.layout.fragment_pedidos_compra

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flRefresh.setOnClickListener { cargarDatos() }

        productoAdapter = ProductoPedidoAdapter(
            cantidades = cantidadesSeleccionadas,
            onCambioCantidad = { actualizarResumenPedido() }
        )

        binding.rvProductosPedido.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            adapter = productoAdapter
        }

        binding.rvHistorialPedidos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historialAdapter
        }

        binding.btnTabNuevoPedido.setOnClickListener { mostrarTabNuevoPedido() }
        binding.btnTabHistorialPedidos.setOnClickListener { mostrarTabHistorial() }
        binding.tvLimpiarPedido.setOnClickListener { limpiarCantidades() }
        binding.btnEnviarPedido.setOnClickListener { enviarPedido() }

        binding.etBuscarProductoPedido.doAfterTextChanged { texto ->
            textoBusqueda = texto?.toString()?.trim().orEmpty()
            aplicarFiltroProductos()
        }

        renderizarSelectorAlmacenes()
        actualizarResumenPedido()
        cargarDatos()
    }

    private fun mostrarTabNuevoPedido() {
        binding.llNuevoPedido.visibility = View.VISIBLE
        binding.llHistorialPedidos.visibility = View.GONE
        binding.btnTabNuevoPedido.setBackgroundResource(R.drawable.bg_chip_selected)
        binding.btnTabNuevoPedido.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.btnTabHistorialPedidos.setBackgroundResource(R.drawable.bg_chip_unselected)
        binding.btnTabHistorialPedidos.setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_primary))
    }

    private fun mostrarTabHistorial() {
        binding.llNuevoPedido.visibility = View.GONE
        binding.llHistorialPedidos.visibility = View.VISIBLE
        binding.btnTabHistorialPedidos.setBackgroundResource(R.drawable.bg_chip_selected)
        binding.btnTabHistorialPedidos.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.btnTabNuevoPedido.setBackgroundResource(R.drawable.bg_chip_unselected)
        binding.btnTabNuevoPedido.setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_primary))
    }

    private fun cargarDatos() {
        binding.tvProductosPedidoVacio.visibility = View.VISIBLE
        binding.tvProductosPedidoVacio.text = "Cargando catálogo de productos…"
        binding.rvProductosPedido.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val productosDef = async {
                    RetrofitClient.empleadoService.obtenerProductos().dataExitosa().orEmpty()
                }
                val pedidosDef = async {
                    try {
                        RetrofitClient.empleadoService.obtenerPedidosCompra().dataExitosa().orEmpty()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val almacenesDef = async {
                    try {
                        RetrofitClient.empleadoService.obtenerAlmacenes()
                            .dataExitosa()
                            .orEmpty()
                            .filter { it.activo }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                val cargados = productosDef.await()
                val historial = pedidosDef.await()
                val almacenesCargados = almacenesDef.await()

                if (!isAdded || view == null) return@launch

                productos = cargados.sortedWith(
                    compareBy<ProductoClienteDto> { it.stockActual > it.stockMinimo }
                        .thenBy { it.stockActual }
                        .thenBy { it.nombre }
                )
                pedidos = historial
                almacenes = almacenesCargados

                renderizarSelectorAlmacenes()
                aplicarFiltroProductos()
                renderizarHistorial()
            } catch (e: Exception) {
                if (!isAdded || view == null) return@launch
                binding.tvProductosPedidoVacio.visibility = View.VISIBLE
                binding.tvProductosPedidoVacio.text = e.mensajeUsuario()
                binding.rvProductosPedido.visibility = View.GONE
            }
        }
    }

    private fun aplicarFiltroProductos() {
        val termino = textoBusqueda.lowercase()
        productosFiltrados = if (termino.isEmpty()) {
            productos
        } else {
            productos.filter { prod ->
                prod.nombre.lowercase().contains(termino) ||
                    prod.codigoBarras.orEmpty().lowercase().contains(termino) ||
                    prod.categoria?.nombre.orEmpty().lowercase().contains(termino)
            }
        }

        productoAdapter.actualizar(productosFiltrados)
        val vacio = productosFiltrados.isEmpty()
        binding.tvProductosPedidoVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvProductosPedido.visibility = if (vacio) View.GONE else View.VISIBLE
        if (vacio) {
            binding.tvProductosPedidoVacio.text = if (productos.isEmpty()) {
                "No hay productos registrados en esta sucursal."
            } else {
                "No hay productos que coincidan con \"$textoBusqueda\"."
            }
        }
    }

    private fun actualizarResumenPedido() {
        val productosById = productos.associateBy { it.id }
        var totalProductos = 0
        var totalPiezas = 0
        var totalEstimado = 0.0

        for ((productoId, cantidad) in cantidadesSeleccionadas) {
            if (cantidad <= 0) continue
            totalProductos++
            totalPiezas += cantidad
            val costo = productosById[productoId]?.precioCompra ?: 0.0
            totalEstimado += costo * cantidad
        }

        binding.tvLimpiarPedido.visibility = if (totalPiezas > 0) View.VISIBLE else View.GONE
        binding.tvTotalEstimadoPedido.text = totalEstimado.formatearMoneda()
        binding.tvResumenPiezasPedido.text =
            "$totalProductos producto(s) • $totalPiezas pieza(s) • Pendiente de validar en web"
    }

    private fun limpiarCantidades() {
        cantidadesSeleccionadas.clear()
        productoAdapter.notifyDataSetChanged()
        actualizarResumenPedido()
    }

    private fun renderizarSelectorAlmacenes() {
        val opciones = mutableListOf("Automático (Almacén principal)")
        almacenes.forEach { almacen ->
            opciones.add(
                if (almacen.esPrincipal) "${almacen.nombre} (Principal)" else almacen.nombre
            )
        }
        binding.spAlmacenDestinoPedido.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            opciones
        )
    }

    private fun obtenerAlmacenDestinoSeleccionado(): Int? {
        val pos = binding.spAlmacenDestinoPedido.selectedItemPosition
        if (pos <= 0) return null
        return almacenes.getOrNull(pos - 1)?.id
    }

    private fun enviarPedido() {
        val detalles = cantidadesSeleccionadas.entries
            .filter { it.value > 0 }
            .map { (productoId, cantidad) ->
                val prod = productos.firstOrNull { it.id == productoId }
                DetallePedidoCompraRequest(
                    productoId = productoId,
                    cantidad = cantidad,
                    costoUnitario = prod?.precioCompra
                )
            }

        if (detalles.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Agrega al menos un producto con cantidad mayor a cero",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val proveedor = binding.etProveedorPedido.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val observaciones = binding.etObservacionesPedido.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val almacenId = obtenerAlmacenDestinoSeleccionado()

        binding.btnEnviarPedido.isEnabled = false
        binding.btnEnviarPedido.text = "Enviando pedido…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearPedidoCompra(
                    CrearPedidoCompraRequest(
                        proveedor = proveedor,
                        observaciones = observaciones,
                        detalles = detalles,
                        almacenId = almacenId
                    )
                )
                val creado = response.dataExitosa()
                if (!isAdded || view == null) return@launch

                if (creado == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo levantar el pedido",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                Toast.makeText(
                    requireContext(),
                    "Pedido #${creado.id} enviado. Se sumará al inventario cuando sea aceptado en web.",
                    Toast.LENGTH_LONG
                ).show()

                binding.etProveedorPedido.setText("")
                binding.etObservacionesPedido.setText("")
                binding.spAlmacenDestinoPedido.setSelection(0)
                limpiarCantidades()
                cargarDatos()
                mostrarTabHistorial()
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), e.mensajeUsuario(), Toast.LENGTH_LONG).show()
                }
            } finally {
                if (isAdded && view != null) {
                    binding.btnEnviarPedido.isEnabled = true
                    binding.btnEnviarPedido.text = "Enviar Pedido a Validación"
                }
            }
        }
    }

    private fun renderizarHistorial() {
        historialAdapter.actualizar(pedidos)
        val vacio = pedidos.isEmpty()
        binding.tvHistorialVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvHistorialPedidos.visibility = if (vacio) View.GONE else View.VISIBLE
        binding.btnTabHistorialPedidos.text = if (pedidos.isEmpty()) {
            "Historial"
        } else {
            "Historial (${pedidos.size})"
        }
    }

    private class ProductoPedidoAdapter(
        private val cantidades: MutableMap<Int, Int>,
        private val onCambioCantidad: () -> Unit
    ) : RecyclerView.Adapter<ProductoPedidoAdapter.Holder>() {

        private var items: List<ProductoClienteDto> = emptyList()

        fun actualizar(nuevos: List<ProductoClienteDto>) {
            items = nuevos
            notifyDataSetChanged()
        }

        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNombre: TextView = itemView.findViewById(R.id.tvProductoPedidoNombre)
            val tvStock: TextView = itemView.findViewById(R.id.tvProductoPedidoStock)
            val tvCosto: TextView = itemView.findViewById(R.id.tvProductoPedidoCosto)
            val btnMenos: TextView = itemView.findViewById(R.id.btnMenosPedido)
            val etCantidad: EditText = itemView.findViewById(R.id.etCantidadPedido)
            val btnMas: TextView = itemView.findViewById(R.id.btnMasPedido)
            val vDivider: View = itemView.findViewById(R.id.vDividerPedido)
            var watcher: TextWatcher? = null
            var actualizandoTexto = false
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val vista = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_producto_pedido, parent, false)
            return Holder(vista)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val prod = items[position]
            val context = holder.itemView.context
            val cantidadActual = (cantidades[prod.id] ?: 0).coerceIn(0, MAX_CANTIDAD)

            holder.tvNombre.text = prod.nombre
            holder.tvStock.text = "Stock actual: ${prod.stockActual} • Mín: ${prod.stockMinimo}"
            holder.tvStock.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (prod.stockActual <= prod.stockMinimo) R.color.apex_orange else R.color.apex_text_secondary
                )
            )
            val costo = prod.precioCompra ?: 0.0
            holder.tvCosto.text = if (costo > 0) {
                "Costo ref.: ${costo.formatearMonedaSinSufijo()} MXN"
            } else {
                "Sin costo de compra registrado"
            }
            holder.vDivider.visibility = if (position == items.lastIndex) View.GONE else View.VISIBLE

            holder.watcher?.let { holder.etCantidad.removeTextChangedListener(it) }
            holder.actualizandoTexto = true
            holder.etCantidad.setText(if (cantidadActual > 0) cantidadActual.toString() else "")
            holder.actualizandoTexto = false

            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (holder.actualizandoTexto) return
                    val crudo = s?.toString()?.trim()?.toIntOrNull() ?: 0
                    val normalizada = crudo.coerceIn(0, MAX_CANTIDAD)
                    if (crudo > MAX_CANTIDAD) {
                        holder.actualizandoTexto = true
                        val textoMax = normalizada.toString()
                        holder.etCantidad.setText(textoMax)
                        holder.etCantidad.setSelection(textoMax.length)
                        holder.actualizandoTexto = false
                    }
                    if (normalizada > 0) {
                        cantidades[prod.id] = normalizada
                    } else {
                        cantidades.remove(prod.id)
                    }
                    onCambioCantidad()
                }
            }
            holder.watcher = watcher
            holder.etCantidad.addTextChangedListener(watcher)

            holder.btnMenos.setOnClickListener {
                val actual = cantidades[prod.id] ?: 0
                aplicarCantidad(holder, prod.id, actual - 1)
            }
            holder.btnMas.setOnClickListener {
                val actual = cantidades[prod.id] ?: 0
                aplicarCantidad(holder, prod.id, actual + 1)
            }
        }

        private fun aplicarCantidad(holder: Holder, productoId: Int, candidato: Int) {
            val nueva = candidato.coerceIn(0, MAX_CANTIDAD)
            if (nueva > 0) {
                cantidades[productoId] = nueva
            } else {
                cantidades.remove(productoId)
            }
            holder.actualizandoTexto = true
            val texto = if (nueva > 0) nueva.toString() else ""
            holder.etCantidad.setText(texto)
            holder.etCantidad.setSelection(texto.length)
            holder.actualizandoTexto = false
            onCambioCantidad()
        }

        override fun getItemCount(): Int = items.size

        companion object {
            private const val MAX_CANTIDAD = 10000
        }
    }

    private inner class HistorialPedidosAdapter : RecyclerView.Adapter<HistorialPedidosAdapter.Holder>() {

        private var items: List<PedidoCompraDto> = emptyList()

        fun actualizar(nuevos: List<PedidoCompraDto>) {
            items = nuevos
            notifyDataSetChanged()
        }

        inner class Holder(
            val card: MaterialCardView,
            val tvFolio: TextView,
            val tvEstado: TextView,
            val tvMeta: TextView,
            val tvDetalles: TextView,
            val tvMotivo: TextView,
            val tvTotal: TextView
        ) : RecyclerView.ViewHolder(card)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
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

            val body = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val tvFolio = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
            }
            header.addView(tvFolio)

            val tvEstado = TextView(context).apply {
                setPadding(dp(10), dp(3), dp(10), dp(3))
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            header.addView(tvEstado)
            body.addView(header)

            val tvMeta = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(4) }
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            body.addView(tvMeta)

            val tvDetalles = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            }
            body.addView(tvDetalles)

            val tvMotivo = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
                setTextColor(android.graphics.Color.parseColor("#DC2626"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                visibility = View.GONE
            }
            body.addView(tvMotivo)

            val tvTotal = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
            }
            body.addView(tvTotal)

            card.addView(body)
            return Holder(card, tvFolio, tvEstado, tvMeta, tvDetalles, tvMotivo, tvTotal)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val pedido = items[position]
            holder.tvFolio.text = "Pedido #${pedido.id}"

            val (etiqueta, colorHex) = when (pedido.estado.uppercase()) {
                "APROBADO" -> "Aprobado" to "#2B8A3E"
                "RECHAZADO" -> "Rechazado" to "#DC2626"
                else -> "Pendiente" to "#F59E0B"
            }
            holder.tvEstado.text = etiqueta
            holder.tvEstado.background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(android.graphics.Color.parseColor(colorHex))
            }

            holder.tvMeta.text = buildString {
                append(formatearFecha(pedido.createdAt, "dd/MM/yyyy HH:mm"))
                pedido.proveedor?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                pedido.almacen?.nombre?.takeIf { it.isNotBlank() }?.let { append(" • Almacén: $it") }
            }

            holder.tvDetalles.text = pedido.detalles.joinToString("\n") { det ->
                val nombre = det.producto?.nombre ?: "Producto #${det.productoId}"
                "• $nombre × ${det.cantidad}"
            }

            if (!pedido.motivoRechazo.isNullOrBlank()) {
                holder.tvMotivo.visibility = View.VISIBLE
                holder.tvMotivo.text = "Motivo de rechazo: ${pedido.motivoRechazo}"
            } else {
                holder.tvMotivo.visibility = View.GONE
            }

            holder.tvTotal.text = "Total estimado: ${pedido.totalEstimado.formatearMonedaSinSufijo()} MXN"
        }

        override fun getItemCount(): Int = items.size
    }

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()
}

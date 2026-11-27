package com.sprm.apexgym.feature.inventario

import android.graphics.Color
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
import com.sprm.apexgym.core.ui.InputFilters
import com.sprm.apexgym.databinding.FragmentPedidosCompraBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Permite a empleados levantar pedidos de compra desde Android.
 * Los pedidos quedan en estado PENDIENTE hasta que se validan en Web para sumarse al inventario.
 */
class PedidosCompraFragment : BaseFragment<FragmentPedidosCompraBinding>() {

    private enum class FiltroCatalogo { TODOS, STOCK_BAJO, SELECCIONADOS }
    private enum class FiltroHistorial { TODOS, PENDIENTES, APROBADOS, RECHAZADOS }

    private var productos: List<ProductoClienteDto> = emptyList()
    private var productosFiltrados: List<ProductoClienteDto> = emptyList()
    private var pedidos: List<PedidoCompraDto> = emptyList()
    private var almacenes: List<AlmacenDto> = emptyList()
    private var almacenSeleccionadoId: Int? = null
    private val cantidadesSeleccionadas = linkedMapOf<Int, Int>()
    private var textoBusqueda = ""
    private var filtroCatalogo = FiltroCatalogo.TODOS
    private var filtroHistorial = FiltroHistorial.TODOS

    private lateinit var productoAdapter: ProductoPedidoAdapter
    private val historialAdapter = HistorialPedidosAdapter()

    override fun getLayoutRes(): Int = R.layout.fragment_pedidos_compra

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarDatos() }

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
        binding.btnIrNuevoPedidoVacio.setOnClickListener { mostrarTabNuevoPedido() }
        binding.tvLimpiarPedido.setOnClickListener { limpiarCantidades() }
        binding.btnEnviarPedido.setOnClickListener { confirmarYEnviarPedido() }
        binding.etProveedorPedido.filters = InputFilters.plainText(150)
        binding.etObservacionesPedido.filters = InputFilters.plainText(500, allowNewLines = true)

        binding.ivLimpiarBusquedaPedido.setOnClickListener {
            binding.etBuscarProductoPedido.setText("")
        }
        binding.etBuscarProductoPedido.doAfterTextChanged { texto ->
            textoBusqueda = texto?.toString()?.trim().orEmpty()
            binding.ivLimpiarBusquedaPedido.visibility =
                if (textoBusqueda.isNotEmpty()) View.VISIBLE else View.GONE
            aplicarFiltroProductos()
        }

        binding.chipPedidoTodos.setOnClickListener {
            filtroCatalogo = FiltroCatalogo.TODOS
            actualizarChipsCatalogo()
            aplicarFiltroProductos()
        }
        binding.chipPedidoStockBajo.setOnClickListener {
            filtroCatalogo = FiltroCatalogo.STOCK_BAJO
            actualizarChipsCatalogo()
            aplicarFiltroProductos()
        }
        binding.chipPedidoSeleccionados.setOnClickListener {
            filtroCatalogo = FiltroCatalogo.SELECCIONADOS
            actualizarChipsCatalogo()
            aplicarFiltroProductos()
        }

        binding.chipHistTodos.setOnClickListener {
            filtroHistorial = FiltroHistorial.TODOS
            actualizarChipsHistorial()
            renderizarHistorial()
        }
        binding.chipHistPendientes.setOnClickListener {
            filtroHistorial = FiltroHistorial.PENDIENTES
            actualizarChipsHistorial()
            renderizarHistorial()
        }
        binding.chipHistAprobados.setOnClickListener {
            filtroHistorial = FiltroHistorial.APROBADOS
            actualizarChipsHistorial()
            renderizarHistorial()
        }
        binding.chipHistRechazados.setOnClickListener {
            filtroHistorial = FiltroHistorial.RECHAZADOS
            actualizarChipsHistorial()
            renderizarHistorial()
        }

        renderizarSelectorAlmacenes()
        actualizarChipsCatalogo()
        actualizarChipsHistorial()
        actualizarResumenPedido()
        cargarDatos()
    }

    private fun mostrarTabNuevoPedido() {
        binding.llNuevoPedido.visibility = View.VISIBLE
        binding.llBarraInferiorPedido.visibility = View.VISIBLE
        binding.llHistorialPedidos.visibility = View.GONE
        binding.btnTabNuevoPedido.setBackgroundResource(R.drawable.bg_chip_selected)
        binding.btnTabNuevoPedido.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.btnTabHistorialPedidos.setBackgroundResource(R.drawable.bg_chip_unselected)
        binding.btnTabHistorialPedidos.setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_primary))
    }

    private fun mostrarTabHistorial() {
        binding.llNuevoPedido.visibility = View.GONE
        binding.llBarraInferiorPedido.visibility = View.GONE
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
                actualizarChipsCatalogo()
                aplicarFiltroProductos()
                renderizarHistorial()
            } catch (e: Exception) {
                if (!isAdded || view == null) return@launch
                binding.tvProductosPedidoVacio.visibility = View.VISIBLE
                binding.tvProductosPedidoVacio.text = e.mensajeUsuario()
                binding.rvProductosPedido.visibility = View.GONE
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun actualizarChipsCatalogo() {
        val ctx = requireContext()
        val stockBajoCount = productos.count { it.stockActual <= it.stockMinimo }
        val seleccionadosCount = cantidadesSeleccionadas.count { it.value > 0 }

        binding.chipPedidoTodos.text = "Todos (${productos.size})"
        binding.chipPedidoStockBajo.text = "Stock bajo ($stockBajoCount)"
        binding.chipPedidoSeleccionados.text = "Seleccionados ($seleccionadosCount)"

        val mapping = listOf(
            binding.chipPedidoTodos to (filtroCatalogo == FiltroCatalogo.TODOS),
            binding.chipPedidoStockBajo to (filtroCatalogo == FiltroCatalogo.STOCK_BAJO),
            binding.chipPedidoSeleccionados to (filtroCatalogo == FiltroCatalogo.SELECCIONADOS)
        )
        mapping.forEach { (chip, activo) ->
            chip.setBackgroundResource(if (activo) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(ContextCompat.getColor(ctx, if (activo) R.color.white else R.color.apex_text_primary))
        }
    }

    private fun actualizarChipsHistorial() {
        val ctx = requireContext()
        val mapping = listOf(
            binding.chipHistTodos to (filtroHistorial == FiltroHistorial.TODOS),
            binding.chipHistPendientes to (filtroHistorial == FiltroHistorial.PENDIENTES),
            binding.chipHistAprobados to (filtroHistorial == FiltroHistorial.APROBADOS),
            binding.chipHistRechazados to (filtroHistorial == FiltroHistorial.RECHAZADOS)
        )
        mapping.forEach { (chip, activo) ->
            chip.setBackgroundResource(if (activo) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(ContextCompat.getColor(ctx, if (activo) R.color.white else R.color.apex_text_primary))
        }
    }

    private fun aplicarFiltroProductos() {
        val termino = textoBusqueda.lowercase()
        productosFiltrados = productos.filter { prod ->
            val coincideChip = when (filtroCatalogo) {
                FiltroCatalogo.TODOS -> true
                FiltroCatalogo.STOCK_BAJO -> prod.stockActual <= prod.stockMinimo
                FiltroCatalogo.SELECCIONADOS -> (cantidadesSeleccionadas[prod.id] ?: 0) > 0
            }
            val coincideBusqueda = termino.isEmpty() ||
                prod.nombre.lowercase().contains(termino) ||
                prod.codigoBarras.orEmpty().lowercase().contains(termino) ||
                prod.categoria?.nombre.orEmpty().lowercase().contains(termino)
            coincideChip && coincideBusqueda
        }

        productoAdapter.actualizar(productosFiltrados)
        val vacio = productosFiltrados.isEmpty()
        binding.tvProductosPedidoVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvProductosPedido.visibility = if (vacio) View.GONE else View.VISIBLE
        if (vacio) {
            binding.tvProductosPedidoVacio.text = when {
                productos.isEmpty() -> "No hay productos registrados en esta sucursal."
                filtroCatalogo == FiltroCatalogo.SELECCIONADOS -> "Aún no has agregado productos a este pedido."
                filtroCatalogo == FiltroCatalogo.STOCK_BAJO -> "No hay productos con stock bajo."
                else -> "No hay productos que coincidan con \"$textoBusqueda\"."
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
        binding.tvResumenPiezasPedido.text = buildString {
            append(if (totalProductos == 1) "1 producto" else "$totalProductos productos")
            append(" • ")
            append(if (totalPiezas == 1) "1 pieza" else "$totalPiezas piezas")
        }
        binding.btnEnviarPedido.alpha = if (totalPiezas > 0) 1f else 0.55f
        actualizarChipsCatalogo()
        if (filtroCatalogo == FiltroCatalogo.SELECCIONADOS) {
            aplicarFiltroProductos()
        }
    }

    private fun limpiarCantidades() {
        cantidadesSeleccionadas.clear()
        productoAdapter.notifyDataSetChanged()
        actualizarResumenPedido()
    }

    private fun renderizarSelectorAlmacenes() {
        val etiquetas = mutableListOf("Automático (Principal)")
        val ids = mutableListOf<Int?>(null)
        almacenes.forEach { almacen ->
            etiquetas.add(almacen.nombre)
            ids.add(almacen.id)
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            etiquetas
        )
        binding.actvAlmacenDestinoPedido.setAdapter(adapter)

        val idxActual = ids.indexOf(almacenSeleccionadoId).takeIf { it >= 0 } ?: 0
        binding.actvAlmacenDestinoPedido.setText(etiquetas[idxActual], false)

        binding.actvAlmacenDestinoPedido.setOnItemClickListener { _, _, position, _ ->
            almacenSeleccionadoId = ids.getOrNull(position)
        }
    }

    private fun confirmarYEnviarPedido() {
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

        val totalPiezas = detalles.sumOf { it.cantidad }
        val totalEst = detalles.sumOf { (it.costoUnitario ?: 0.0) * it.cantidad }
        val nombreAlmacen = binding.actvAlmacenDestinoPedido.text?.toString().orEmpty()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar pedido")
            .setMessage(
                "Se enviará a validación un pedido con ${detalles.size} productos ($totalPiezas piezas).\n\n" +
                    "Destino: $nombreAlmacen\n" +
                    "Total estimado: ${totalEst.formatearMoneda()}"
            )
            .setNegativeButton("Revisar", null)
            .setPositiveButton("Enviar pedido") { _, _ ->
                enviarPedido(detalles)
            }
            .show()
    }

    private fun enviarPedido(detalles: List<DetallePedidoCompraRequest>) {
        val proveedor = binding.etProveedorPedido.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val observaciones = binding.etObservacionesPedido.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val almacenId = almacenSeleccionadoId

        binding.btnEnviarPedido.isEnabled = false
        binding.btnEnviarPedido.text = "Enviando…"

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
                    "Pedido #${creado.id} enviado a validación.",
                    Toast.LENGTH_LONG
                ).show()

                binding.etProveedorPedido.setText("")
                binding.etObservacionesPedido.setText("")
                almacenSeleccionadoId = null
                renderizarSelectorAlmacenes()
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
                    binding.btnEnviarPedido.text = "Enviar pedido"
                }
            }
        }
    }

    private fun renderizarHistorial() {
        val filtrados = pedidos.filter { pedido ->
            when (filtroHistorial) {
                FiltroHistorial.TODOS -> true
                FiltroHistorial.PENDIENTES -> pedido.estado.equals("PENDIENTE", ignoreCase = true)
                FiltroHistorial.APROBADOS -> pedido.estado.equals("APROBADO", ignoreCase = true)
                FiltroHistorial.RECHAZADOS -> pedido.estado.equals("RECHAZADO", ignoreCase = true)
            }
        }

        historialAdapter.actualizar(filtrados)
        val vacio = filtrados.isEmpty()
        binding.llHistorialVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvHistorialPedidos.visibility = if (vacio) View.GONE else View.VISIBLE
        if (vacio) {
            binding.tvHistorialVacio.text = if (pedidos.isEmpty()) {
                "No hay pedidos de compra registrados en esta sucursal."
            } else {
                "No hay pedidos en el estado seleccionado."
            }
        }
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
            val card: MaterialCardView = itemView.findViewById(R.id.cardProductoPedido)
            val tvNombre: TextView = itemView.findViewById(R.id.tvProductoPedidoNombre)
            val tvBadgeStock: TextView = itemView.findViewById(R.id.tvBadgeStockPedido)
            val tvStock: TextView = itemView.findViewById(R.id.tvProductoPedidoStock)
            val tvCosto: TextView = itemView.findViewById(R.id.tvProductoPedidoCosto)
            val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotalProductoPedido)
            val btnMenos: TextView = itemView.findViewById(R.id.btnMenosPedido)
            val etCantidad: EditText = itemView.findViewById(R.id.etCantidadPedido)
            val btnMas: TextView = itemView.findViewById(R.id.btnMasPedido)
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
            holder.tvStock.text = "Existencia actual: ${prod.stockActual} u. • Mínimo: ${prod.stockMinimo} u."

            val (badgeTexto, bgColor, textColor) = when {
                prod.stockActual <= 0 -> Triple("Sin stock", "#FEE2E2", "#991B1B")
                prod.stockActual <= prod.stockMinimo -> Triple("Stock bajo", "#FFEDD5", "#9A3412")
                else -> Triple("En stock", "#DCFCE7", "#166534")
            }
            holder.tvBadgeStock.text = badgeTexto
            holder.tvBadgeStock.setTextColor(Color.parseColor(textColor))
            holder.tvBadgeStock.background = GradientDrawable().apply {
                cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 100f, context.resources.displayMetrics
                )
                setColor(Color.parseColor(bgColor))
            }

            val costo = prod.precioCompra ?: 0.0
            holder.tvCosto.text = if (costo > 0) {
                "Costo unitario ref.: ${costo.formatearMonedaSinSufijo()} MXN"
            } else {
                "Costo unitario no definido"
            }

            actualizarVisualSeleccion(holder, prod, cantidadActual)

            holder.watcher?.let { holder.etCantidad.removeTextChangedListener(it) }
            holder.etCantidad.filters = InputFilters.digitsOnly(5)
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
                    actualizarVisualSeleccion(holder, prod, normalizada)
                    onCambioCantidad()
                }
            }
            holder.watcher = watcher
            holder.etCantidad.addTextChangedListener(watcher)

            holder.btnMenos.setOnClickListener {
                val actual = cantidades[prod.id] ?: 0
                aplicarCantidad(holder, prod, actual - 1)
            }
            holder.btnMas.setOnClickListener {
                val actual = cantidades[prod.id] ?: 0
                aplicarCantidad(holder, prod, actual + 1)
            }
        }

        private fun actualizarVisualSeleccion(
            holder: Holder,
            prod: ProductoClienteDto,
            cantidad: Int
        ) {
            val ctx = holder.itemView.context
            val seleccionado = cantidad > 0
            holder.card.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                if (seleccionado) 1.5f else 1f,
                ctx.resources.displayMetrics
            ).toInt()
            holder.card.strokeColor = ContextCompat.getColor(
                ctx,
                if (seleccionado) R.color.apex_orange else R.color.apex_stroke_light
            )
            holder.btnMenos.alpha = if (seleccionado) 1f else 0.4f
            val costo = prod.precioCompra ?: 0.0
            if (seleccionado && costo > 0) {
                holder.tvSubtotal.visibility = View.VISIBLE
                holder.tvSubtotal.text = "Subtotal: ${(costo * cantidad).formatearMonedaSinSufijo()} MXN"
            } else if (seleccionado) {
                holder.tvSubtotal.visibility = View.VISIBLE
                holder.tvSubtotal.text = "$cantidad piezas seleccionadas"
            } else {
                holder.tvSubtotal.visibility = View.INVISIBLE
            }
        }

        private fun aplicarCantidad(holder: Holder, prod: ProductoClienteDto, candidato: Int) {
            val nueva = candidato.coerceIn(0, MAX_CANTIDAD)
            if (nueva > 0) {
                cantidades[prod.id] = nueva
            } else {
                cantidades.remove(prod.id)
            }
            holder.actualizandoTexto = true
            val texto = if (nueva > 0) nueva.toString() else ""
            holder.etCantidad.setText(texto)
            holder.etCantidad.setSelection(texto.length)
            holder.actualizandoTexto = false
            actualizarVisualSeleccion(holder, prod, nueva)
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

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvFolio: TextView = itemView.findViewById(R.id.tvPedidoHistFolio)
            val tvEstado: TextView = itemView.findViewById(R.id.tvPedidoHistEstado)
            val tvMeta: TextView = itemView.findViewById(R.id.tvPedidoHistMeta)
            val tvDetalles: TextView = itemView.findViewById(R.id.tvPedidoHistDetalles)
            val tvObservaciones: TextView = itemView.findViewById(R.id.tvPedidoHistObservaciones)
            val tvMotivo: TextView = itemView.findViewById(R.id.tvPedidoHistMotivo)
            val tvTotal: TextView = itemView.findViewById(R.id.tvPedidoHistTotal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val vista = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pedido_historial, parent, false)
            return Holder(vista)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val pedido = items[position]
            val ctx = holder.itemView.context
            holder.tvFolio.text = "Pedido #${pedido.id}"

            val (etiqueta, bgHex, textHex) = when (pedido.estado.uppercase()) {
                "APROBADO" -> Triple("Aprobado", "#DCFCE7", "#166534")
                "RECHAZADO" -> Triple("Rechazado", "#FEE2E2", "#991B1B")
                else -> Triple("Pendiente", "#FEF3C7", "#92400E")
            }
            holder.tvEstado.text = etiqueta
            holder.tvEstado.setTextColor(Color.parseColor(textHex))
            holder.tvEstado.background = GradientDrawable().apply {
                cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 100f, ctx.resources.displayMetrics
                )
                setColor(Color.parseColor(bgHex))
            }

            holder.tvMeta.text = buildString {
                append(formatearFecha(pedido.createdAt, "dd/MM/yyyy HH:mm"))
                pedido.almacen?.nombre?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                pedido.proveedor?.takeIf { it.isNotBlank() }?.let { append(" • Prov: $it") }
            }

            holder.tvDetalles.text = pedido.detalles.joinToString("\n") { det ->
                val nombre = det.producto?.nombre ?: "Producto #${det.productoId}"
                if (det.subtotal > 0) {
                    "• $nombre × ${det.cantidad} (${det.subtotal.formatearMonedaSinSufijo()})"
                } else {
                    "• $nombre × ${det.cantidad}"
                }
            }

            if (!pedido.observaciones.isNullOrBlank()) {
                holder.tvObservaciones.visibility = View.VISIBLE
                holder.tvObservaciones.text = "Notas: ${pedido.observaciones}"
            } else {
                holder.tvObservaciones.visibility = View.GONE
            }

            if (!pedido.motivoRechazo.isNullOrBlank()) {
                holder.tvMotivo.visibility = View.VISIBLE
                holder.tvMotivo.text = "Motivo de rechazo: ${pedido.motivoRechazo}"
                holder.tvMotivo.setTextColor(Color.parseColor("#991B1B"))
                holder.tvMotivo.background = GradientDrawable().apply {
                    cornerRadius = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 12f, ctx.resources.displayMetrics
                    )
                    setColor(Color.parseColor("#FEE2E2"))
                }
            } else {
                holder.tvMotivo.visibility = View.GONE
            }

            holder.tvTotal.text = "${pedido.totalEstimado.formatearMonedaSinSufijo()} MXN"
        }

        override fun getItemCount(): Int = items.size
    }
}

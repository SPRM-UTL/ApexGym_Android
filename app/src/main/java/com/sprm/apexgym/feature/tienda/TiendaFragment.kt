package com.sprm.apexgym.feature.tienda

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.client.ProductoClienteDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.client.ServicioClienteDto
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.databinding.FragmentTiendaBinding
import com.sprm.apexgym.feature.main.CartManager
import com.sprm.apexgym.feature.main.ProductDetailBottomSheetFragment
import com.sprm.apexgym.feature.main.ServiceDetailBottomSheetFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Punto de venta para empleados. Carga el catálogo de productos y servicios,
 * separa ambas secciones por pestañas y reutiliza [CartManager] para armar la venta.
 */
class TiendaFragment : BaseFragment<FragmentTiendaBinding>() {

    private var productos: List<ProductoClienteDto> = emptyList()
    private var servicios: List<ServicioClienteDto> = emptyList()
    private var categoriaSeleccionada: String = "Todos"
    private var textoBusqueda: String = ""
    private var mostrandoServicios: Boolean = false
    private val chips = mutableListOf<TextView>()
    private val bitmapCache = LruCache<String, Bitmap>(40)

    private val productoAdapter = ProductoAdapter()
    private val servicioAdapter = ServicioAdapter()

    private var cartListener: (() -> Unit)? = null
    private var omitirPrimerResume = false

    override fun getLayoutRes(): Int = R.layout.fragment_tienda

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = productoAdapter
        binding.rvProducts.isNestedScrollingEnabled = false

        binding.rvServices.layoutManager = LinearLayoutManager(requireContext())
        binding.rvServices.adapter = servicioAdapter
        binding.rvServices.isNestedScrollingEnabled = false

        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarCatalogo() }

        view.findViewWithTag<View>("fl_back_tienda")?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.flCartButton.setOnClickListener { abrirCarrito() }
        binding.flHistoryButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, HistorialComprasFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnTabPosProductos.setOnClickListener { seleccionarSeccion(serviciosTab = false) }
        binding.btnTabPosServicios.setOnClickListener { seleccionarSeccion(serviciosTab = true) }

        binding.ivClearSearchTiendaPos.setOnClickListener {
            binding.etSearchTiendaPos.setText("")
        }
        binding.etSearchTiendaPos.doAfterTextChanged { editable ->
            textoBusqueda = editable?.toString()?.trim().orEmpty()
            binding.ivClearSearchTiendaPos.visibility =
                if (textoBusqueda.isNotEmpty()) View.VISIBLE else View.GONE
            renderizarProductos()
            renderizarServicios()
        }

        val listener: () -> Unit = { actualizarBadgeCarrito() }
        cartListener = listener
        CartManager.addListener(listener)
        actualizarBadgeCarrito()

        seleccionarSeccion(serviciosTab = mostrandoServicios)
        omitirPrimerResume = true
        cargarCatalogo()
    }

    override fun onResume() {
        super.onResume()
        if (omitirPrimerResume) {
            omitirPrimerResume = false
        } else if (view != null) {
            cargarCatalogo()
        }
    }

    override fun onDestroyView() {
        cartListener?.let { CartManager.removeListener(it) }
        cartListener = null
        super.onDestroyView()
    }

    private fun seleccionarSeccion(serviciosTab: Boolean) {
        mostrandoServicios = serviciosTab
        val ctx = requireContext()

        binding.llPosProductsSection.visibility = if (serviciosTab) View.GONE else View.VISIBLE
        binding.llPosServicesSection.visibility = if (serviciosTab) View.VISIBLE else View.GONE

        if (serviciosTab) {
            binding.btnTabPosServicios.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnTabPosServicios.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            binding.btnTabPosProductos.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.btnTabPosProductos.setTextColor(ContextCompat.getColor(ctx, R.color.apex_text_primary))
            binding.etSearchTiendaPos.hint = "Buscar servicio por nombre o descripción..."
        } else {
            binding.btnTabPosProductos.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.btnTabPosProductos.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            binding.btnTabPosServicios.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.btnTabPosServicios.setTextColor(ContextCompat.getColor(ctx, R.color.apex_text_primary))
            binding.etSearchTiendaPos.hint = "Buscar producto o código de barras..."
        }
    }

    private fun cargarCatalogo() {
        mostrarCargando()

        viewLifecycleOwner.lifecycleScope.launch {
            val productosDeferred = async {
                try {
                    val listaAdmin = RetrofitClient.empleadoService.obtenerProductos()
                        .dataExitosa()
                        .orEmpty()
                        .filter { it.estado.isNullOrBlank() || it.estado.equals("activo", ignoreCase = true) }
                    if (listaAdmin.isNotEmpty()) {
                        listaAdmin
                    } else {
                        RetrofitClient.authService.obtenerProductosCliente().dataExitosa().orEmpty()
                    }
                } catch (_: Exception) {
                    try {
                        RetrofitClient.authService.obtenerProductosCliente().dataExitosa().orEmpty()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
            val serviciosDeferred = async {
                try {
                    val listaAdmin = RetrofitClient.empleadoService.obtenerServicios()
                        .dataExitosa()
                        .orEmpty()
                        .filter { it.estado.isNullOrBlank() || it.estado.equals("activo", ignoreCase = true) }
                    if (listaAdmin.isNotEmpty()) {
                        listaAdmin
                    } else {
                        RetrofitClient.authService.obtenerServiciosCliente().dataExitosa().orEmpty()
                    }
                } catch (_: Exception) {
                    try {
                        RetrofitClient.authService.obtenerServiciosCliente().dataExitosa().orEmpty()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }

            productos = productosDeferred.await()
            servicios = serviciosDeferred.await()

            if (!isAdded || view == null) return@launch

            binding.swipeRefresh.isRefreshing = false
            construirChips()
            renderizarProductos()
            renderizarServicios()
        }
    }

    private fun mostrarCargando() {
        binding.rvProducts.visibility = View.GONE
        binding.rvServices.visibility = View.GONE
        binding.llEmptyProducts.visibility = View.VISIBLE
        binding.llEmptyServices.visibility = View.GONE
        binding.tvProductCount.text = "Cargando…"
        binding.tvServiceCount.text = "Cargando…"
        textoEstado(binding.llEmptyProducts)?.text = "Cargando catálogo de productos…"
    }

    private fun construirChips() {
        binding.llCategoryChips.removeAllViews()
        chips.clear()

        val categorias = mutableListOf("Todos")
        productos.mapNotNull { it.categoria?.nombre?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { categorias.add(it) }

        if (categoriaSeleccionada !in categorias) {
            categoriaSeleccionada = "Todos"
        }

        categorias.forEachIndexed { index, categoria ->
            val chip = TextView(requireContext()).apply {
                text = categoria
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
                    if (index > 0) marginStart = dp(8)
                }
                setOnClickListener {
                    categoriaSeleccionada = categoria
                    actualizarEstiloChips()
                    renderizarProductos()
                }
            }
            chips.add(chip)
            binding.llCategoryChips.addView(chip)
        }

        actualizarEstiloChips()
    }

    private fun actualizarEstiloChips() {
        chips.forEach { chip ->
            if (chip.text.toString().equals(categoriaSeleccionada, ignoreCase = true)) {
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

    private fun renderizarProductos() {
        val filtrados = productos.filter { prod ->
            val coincideCategoria = categoriaSeleccionada == "Todos" ||
                prod.categoria?.nombre?.equals(categoriaSeleccionada, ignoreCase = true) == true
            val coincideBusqueda = textoBusqueda.isEmpty() ||
                prod.nombre.contains(textoBusqueda, ignoreCase = true) ||
                prod.codigoBarras.orEmpty().contains(textoBusqueda, ignoreCase = true) ||
                prod.categoria?.nombre.orEmpty().contains(textoBusqueda, ignoreCase = true)
            coincideCategoria && coincideBusqueda
        }.sortedBy { it.stockActual <= 0 }

        productoAdapter.actualizar(filtrados)
        binding.tvProductCount.text = if (filtrados.size == 1) {
            "1 producto"
        } else {
            "${filtrados.size} productos"
        }

        val vacio = filtrados.isEmpty()
        binding.llEmptyProducts.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvProducts.visibility = if (vacio) View.GONE else View.VISIBLE
        textoEstado(binding.llEmptyProducts)?.text = when {
            productos.isEmpty() -> "No hay productos disponibles para venta en este momento."
            textoBusqueda.isNotEmpty() -> "No hay productos que coincidan con \"$textoBusqueda\"."
            else -> "No hay productos en esta categoría."
        }
    }

    private fun renderizarServicios() {
        val filtrados = servicios.filter { serv ->
            textoBusqueda.isEmpty() ||
                serv.nombre.contains(textoBusqueda, ignoreCase = true) ||
                serv.descripcion.orEmpty().contains(textoBusqueda, ignoreCase = true)
        }

        servicioAdapter.actualizar(filtrados)
        binding.tvServiceCount.text = if (filtrados.size == 1) {
            "1 servicio"
        } else {
            "${filtrados.size} servicios"
        }

        val vacio = filtrados.isEmpty()
        binding.llEmptyServices.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvServices.visibility = if (vacio) View.GONE else View.VISIBLE
        textoEstado(binding.llEmptyServices)?.text = if (servicios.isEmpty()) {
            "No hay servicios disponibles"
        } else {
            "No hay servicios que coincidan con \"$textoBusqueda\"."
        }
    }

    private fun abrirCarrito() {
        val existente = childFragmentManager.findFragmentByTag(CarritoBottomSheetFragment.TAG)
        if (existente == null) {
            CarritoBottomSheetFragment().show(
                childFragmentManager,
                CarritoBottomSheetFragment.TAG
            )
        }
    }

    private fun abrirDetalleProducto(producto: ProductoClienteDto) {
        val existente = childFragmentManager.findFragmentByTag(ProductDetailBottomSheetFragment.TAG)
        if (existente == null) {
            ProductDetailBottomSheetFragment.newInstance(producto)
                .show(childFragmentManager, ProductDetailBottomSheetFragment.TAG)
        }
    }

    private fun abrirDetalleServicio(servicio: ServicioClienteDto) {
        val existente = childFragmentManager.findFragmentByTag(ServiceDetailBottomSheetFragment.TAG)
        if (existente == null) {
            ServiceDetailBottomSheetFragment.newInstance(servicio)
                .show(childFragmentManager, ServiceDetailBottomSheetFragment.TAG)
        }
    }

    private fun agregarProducto(producto: ProductoClienteDto, cantidad: Int = 1) {
        val resultado = CartManager.agregarProducto(requireContext(), producto, cantidad)
        Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun agregarServicio(servicio: ServicioClienteDto) {
        val resultado = CartManager.agregarServicio(requireContext(), servicio)
        Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun actualizarBadgeCarrito() {
        if (!isAdded || view == null) return
        val cantidad = CartManager.obtenerCantidadTotalArticulos(requireContext())
        binding.tvCartBadge.visibility = if (cantidad > 0) View.VISIBLE else View.GONE
        binding.tvCartBadge.text = cantidad.toString()
    }

    private fun textoEstado(contenedor: LinearLayout): TextView? =
        contenedor.getChildAt(1) as? TextView

    private inner class ProductoAdapter : RecyclerView.Adapter<ProductoViewHolder>() {

        private var items: List<ProductoClienteDto> = emptyList()

        fun actualizar(nuevos: List<ProductoClienteDto>) {
            items = nuevos
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
            val context = parent.context
            val card = MaterialCardView(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(232)
                ).apply {
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
                radius = dp(22).toFloat()
                cardElevation = dp(3).toFloat()
                strokeWidth = 0
            }

            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }

            val imgBox = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(108)
                )
                setBackgroundResource(R.drawable.bg_product_image_container)
                clipToOutline = true
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER)
                        setImageResource(R.drawable.ic_supplement_bottle)
                    }
                )
            }
            inner.addView(imgBox)

            val tvNombre = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            inner.addView(tvNombre)

            val tvMeta = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) }
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            inner.addView(tvMeta)

            inner.addView(
                View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                }
            )

            val bottomRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val tvPrecio = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            bottomRow.addView(tvPrecio)

            val plusIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER)
                setImageResource(R.drawable.ic_plus)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                )
            }
            val btnAdd = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
                setBackgroundResource(R.drawable.bg_circle_orange)
                addView(plusIcon)
            }
            bottomRow.addView(btnAdd)

            inner.addView(bottomRow)
            card.addView(inner)

            return ProductoViewHolder(card, imgBox, tvNombre, tvMeta, tvPrecio, btnAdd, plusIcon)
        }

        override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
            val producto = items[position]
            val context = holder.itemView.context
            val agotado = producto.stockActual <= 0
            val precioNeto = CalculoVentaPos.precioConDescuento(
                producto.precioVenta,
                producto.descuentoPorcentaje
            )

            holder.producto = producto
            holder.card.setCardBackgroundColor(
                if (agotado) Color.parseColor("#6B7280")
                else ContextCompat.getColor(context, R.color.apex_card_dark)
            )
            holder.card.alpha = if (agotado) 0.78f else 1f
            holder.tvNombre.text = producto.nombre
            holder.tvNombre.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.tvMeta.text = buildString {
                append(producto.categoria?.nombre ?: "General")
                if (agotado) {
                    append(" • Agotado")
                } else {
                    append(" • Stock: ${producto.stockActual}")
                }
            }
            holder.tvMeta.setTextColor(
                ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary)
            )
            holder.tvPrecio.text = "${precioNeto.formatearMonedaSinSufijo()} MXN"
            holder.tvPrecio.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (agotado) R.color.apex_text_on_dark_secondary else R.color.apex_orange
                )
            )

            holder.btnAdd.setBackgroundResource(
                if (agotado) R.drawable.bg_chip_unselected else R.drawable.bg_circle_orange
            )
            ImageViewCompat.setImageTintList(
                holder.plusIcon,
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (agotado) R.color.apex_nav_inactive else R.color.white
                    )
                )
            )

            holder.imgBox.tag = null
            holder.imgBox.removeAllViews()
            val placeholderIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER)
                setImageResource(R.drawable.ic_supplement_bottle)
                colorFilter = if (agotado) {
                    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                } else {
                    null
                }
            }
            holder.imgBox.addView(placeholderIcon)
            if (agotado) {
                holder.imgBox.addView(
                    TextView(context).apply {
                        text = "AGOTADO"
                        setTextColor(Color.WHITE)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                        setTypeface(typeface, Typeface.BOLD)
                        setPadding(dp(7), dp(2), dp(7), dp(2))
                        setBackgroundColor(Color.parseColor("#CC334155"))
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP or Gravity.START
                        ).apply {
                            setMargins(dp(6), dp(6), 0, 0)
                        }
                    }
                )
            }
            cargarImagen(producto.imagenUrl, holder.imgBox, desaturar = agotado)

            holder.card.setOnClickListener {
                if (agotado) {
                    Toast.makeText(
                        context,
                        "${producto.nombre} está agotado temporalmente.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    abrirDetalleProducto(producto)
                }
            }
            holder.btnAdd.setOnClickListener {
                if (agotado) {
                    Toast.makeText(
                        context,
                        "${producto.nombre} está agotado temporalmente.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    agregarProducto(holder.producto ?: producto)
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private inner class ServicioAdapter : RecyclerView.Adapter<ServicioViewHolder>() {

        private var items: List<ServicioClienteDto> = emptyList()

        fun actualizar(nuevos: List<ServicioClienteDto>) {
            items = nuevos
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicioViewHolder {
            val context = parent.context
            val card = MaterialCardView(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                radius = dp(18).toFloat()
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
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                setBackgroundResource(R.drawable.bg_circle_orange_light)
                clipToOutline = true
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER)
                        setImageResource(R.drawable.ic_fitness_service)
                        ImageViewCompat.setImageTintList(
                            this,
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_orange))
                        )
                    }
                )
            }
            row.addView(iconBox)

            val infoCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply {
                        marginStart = dp(12)
                        marginEnd = dp(10)
                    }
            }

            val tvNombre = TextView(context).apply {
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            infoCol.addView(tvNombre)

            val tvDescripcion = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(3) }
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            infoCol.addView(tvDescripcion)

            val tvPrecio = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(5) }
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
            }
            infoCol.addView(tvPrecio)

            row.addView(infoCol)

            val btnAdd = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
                setBackgroundResource(R.drawable.bg_circle_orange)
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER)
                        setImageResource(R.drawable.ic_plus)
                        ImageViewCompat.setImageTintList(
                            this,
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                        )
                    }
                )
            }
            row.addView(btnAdd)

            card.addView(row)
            return ServicioViewHolder(card, iconBox, tvNombre, tvDescripcion, tvPrecio, btnAdd)
        }

        override fun onBindViewHolder(holder: ServicioViewHolder, position: Int) {
            val servicio = items[position]
            val context = holder.itemView.context
            holder.servicio = servicio
            holder.tvNombre.text = servicio.nombre
            holder.tvDescripcion.text = servicio.descripcion?.takeIf { it.isNotBlank() }
                ?: "Servicio disponible en sucursal ApexGym"
            holder.tvPrecio.text = "${servicio.precio.formatearMonedaSinSufijo()} MXN"

            holder.iconBox.tag = null
            holder.iconBox.removeAllViews()
            holder.iconBox.addView(
                ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER)
                    setImageResource(R.drawable.ic_fitness_service)
                    ImageViewCompat.setImageTintList(
                        this,
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_orange))
                    )
                }
            )
            cargarImagen(servicio.imagenUrl, holder.iconBox)

            holder.card.setOnClickListener { abrirDetalleServicio(servicio) }
            holder.btnAdd.setOnClickListener {
                agregarServicio(holder.servicio ?: servicio)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private class ProductoViewHolder(
        val card: MaterialCardView,
        val imgBox: FrameLayout,
        val tvNombre: TextView,
        val tvMeta: TextView,
        val tvPrecio: TextView,
        val btnAdd: FrameLayout,
        val plusIcon: ImageView
    ) : RecyclerView.ViewHolder(card) {
        var producto: ProductoClienteDto? = null
    }

    private class ServicioViewHolder(
        val card: MaterialCardView,
        val iconBox: FrameLayout,
        val tvNombre: TextView,
        val tvDescripcion: TextView,
        val tvPrecio: TextView,
        val btnAdd: FrameLayout
    ) : RecyclerView.ViewHolder(card) {
        var servicio: ServicioClienteDto? = null
    }

    private fun cargarImagen(
        ruta: String?,
        contenedor: FrameLayout,
        desaturar: Boolean = false
    ) {
        val url = RetrofitClient.resolverUrlImagen(ruta) ?: return
        contenedor.tag = url

        val cached = bitmapCache.get(url)
        if (cached != null) {
            mostrarBitmapEnContenedor(contenedor, cached, desaturar)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = decodificarImagen(url) ?: return@launch
            bitmapCache.put(url, bitmap)

            withContext(Dispatchers.Main) {
                if (!isAdded || contenedor.tag != url) return@withContext
                mostrarBitmapEnContenedor(contenedor, bitmap, desaturar)
            }
        }
    }

    private fun mostrarBitmapEnContenedor(
        contenedor: FrameLayout,
        bitmap: Bitmap,
        desaturar: Boolean = false
    ) {
        val badges = (0 until contenedor.childCount)
            .map { contenedor.getChildAt(it) }
            .filter { it is TextView }
        contenedor.removeAllViews()
        contenedor.addView(
            ImageView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bitmap)
                colorFilter = if (desaturar) {
                    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                } else {
                    null
                }
            }
        )
        badges.forEach { contenedor.addView(it) }
    }

    private fun decodificarImagen(url: String): Bitmap? = try {
        if (url.startsWith("data:image")) {
            val base64 = url.substringAfter("base64,", "")
            if (base64.isNotBlank()) {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else {
                null
            }
        } else {
            URL(url).openStream().use { BitmapFactory.decodeStream(it) }
        }
    } catch (_: Exception) {
        null
    }

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()
}

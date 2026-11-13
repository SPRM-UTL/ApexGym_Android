package com.sprm.apexgym.feature.tienda

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
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
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentTiendaBinding
import com.sprm.apexgym.feature.main.CartManager
import com.sprm.apexgym.feature.main.ProductDetailBottomSheetFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Punto de venta para empleados. Carga el catálogo público de productos y servicios
 * y reutiliza [CartManager] para armar la venta.
 */
class TiendaFragment : BaseFragment<FragmentTiendaBinding>() {

    private var productos: List<ProductoClienteDto> = emptyList()
    private var servicios: List<ServicioClienteDto> = emptyList()
    private var categoriaSeleccionada: String = "Todos"
    private val chips = mutableListOf<TextView>()

    private val productoAdapter = ProductoAdapter()
    private val servicioAdapter = ServicioAdapter()

    private var cartListener: (() -> Unit)? = null

    override fun getLayoutRes(): Int = R.layout.fragment_tienda

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = productoAdapter
        binding.rvProducts.isNestedScrollingEnabled = false

        binding.rvServices.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvServices.adapter = servicioAdapter
        binding.rvServices.isNestedScrollingEnabled = false

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

        val listener: () -> Unit = { actualizarBadgeCarrito() }
        cartListener = listener
        CartManager.addListener(listener)
        actualizarBadgeCarrito()

        cargarCatalogo()
    }

    override fun onDestroyView() {
        cartListener?.let { CartManager.removeListener(it) }
        cartListener = null
        super.onDestroyView()
    }

    private fun cargarCatalogo() {
        mostrarCargando()

        viewLifecycleOwner.lifecycleScope.launch {
            val productosDeferred = async {
                try {
                    RetrofitClient.authService.obtenerProductosCliente().dataExitosa().orEmpty()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val serviciosDeferred = async {
                try {
                    RetrofitClient.authService.obtenerServiciosCliente().dataExitosa().orEmpty()
                } catch (_: Exception) {
                    emptyList()
                }
            }

            productos = productosDeferred.await()
            servicios = serviciosDeferred.await()

            if (!isAdded) return@launch

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
        val filtrados = productos.filter {
            categoriaSeleccionada == "Todos" ||
                it.categoria?.nombre?.equals(categoriaSeleccionada, ignoreCase = true) == true
        }

        productoAdapter.actualizar(filtrados)
        binding.tvProductCount.text = if (filtrados.size == 1) {
            "1 producto"
        } else {
            "${filtrados.size} productos"
        }

        val vacio = filtrados.isEmpty()
        binding.llEmptyProducts.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvProducts.visibility = if (vacio) View.GONE else View.VISIBLE
        textoEstado(binding.llEmptyProducts)?.text = if (productos.isEmpty()) {
            "No hay productos disponibles para venta en este momento."
        } else {
            "No hay productos en esta categoría."
        }
    }

    private fun renderizarServicios() {
        servicioAdapter.actualizar(servicios)
        binding.tvServiceCount.text = if (servicios.size == 1) {
            "1 servicio"
        } else {
            "${servicios.size} servicios"
        }

        val vacio = servicios.isEmpty()
        binding.llEmptyServices.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvServices.visibility = if (vacio) View.GONE else View.VISIBLE
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
                    dp(226)
                ).apply {
                    setMargins(dp(5), dp(5), dp(5), dp(5))
                }
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
                radius = dp(20).toFloat()
                cardElevation = dp(3).toFloat()
                strokeWidth = 0
            }

            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }

            val imgBox = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(98)
                )
                setBackgroundResource(R.drawable.bg_product_image_container)
                clipToOutline = true
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER)
                        setImageResource(R.drawable.ic_supplement_bottle)
                    }
                )
            }
            inner.addView(imgBox)

            val tvNombre = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(9) }
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            inner.addView(tvNombre)

            val tvMeta = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) }
                setTextColor(
                    ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary)
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
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
            }

            val tvPrecio = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
            }
            bottomRow.addView(tvPrecio)

            val btnAdd = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
                setBackgroundResource(R.drawable.bg_circle_orange)
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER)
                        setImageResource(R.drawable.ic_plus)
                    }
                )
            }
            bottomRow.addView(btnAdd)

            inner.addView(bottomRow)
            card.addView(inner)

            return ProductoViewHolder(card, imgBox, tvNombre, tvMeta, tvPrecio, btnAdd)
        }

        override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
            val producto = items[position]
            holder.producto = producto
            holder.tvNombre.text = producto.nombre
            holder.tvMeta.text = buildString {
                append(producto.categoria?.nombre ?: "Producto")
                if (producto.stockActual > 0) append(" • Stock: ${producto.stockActual}")
            }
            holder.tvPrecio.text = "${producto.precioVenta.formatearMonedaSinSufijo()} MXN"

            holder.imgBox.removeAllViews()
            holder.imgBox.addView(
                ImageView(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER)
                    setImageResource(R.drawable.ic_supplement_bottle)
                }
            )
            cargarImagen(producto.imagenUrl, holder.imgBox)

            holder.card.setOnClickListener {
                val existente = childFragmentManager.findFragmentByTag(
                    ProductDetailBottomSheetFragment.TAG
                )
                if (existente == null) {
                    ProductDetailBottomSheetFragment.newInstance(producto)
                        .show(childFragmentManager, ProductDetailBottomSheetFragment.TAG)
                }
            }
            holder.btnAdd.setOnClickListener {
                agregarProducto(holder.producto ?: producto)
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
                layoutParams = RecyclerView.LayoutParams(dp(238), ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { marginEnd = dp(10) }
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
                radius = dp(20).toFloat()
                cardElevation = dp(3).toFloat()
                strokeWidth = 0
            }

            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
            }

            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val iconBox = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                setBackgroundResource(R.drawable.bg_product_image_container)
                clipToOutline = true
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                        setImageResource(R.drawable.ic_fitness_service)
                    }
                )
            }
            topRow.addView(iconBox)

            val tvNombre = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply { marginStart = dp(10) }
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            topRow.addView(tvNombre)
            inner.addView(topRow)

            val tvDescripcion = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                    bottomMargin = dp(10)
                }
                setTextColor(
                    ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary)
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            inner.addView(tvDescripcion)

            val bottomRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvPrecio = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            bottomRow.addView(tvPrecio)

            val btnAdd = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
                setBackgroundResource(R.drawable.bg_circle_orange)
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER)
                        setImageResource(R.drawable.ic_plus)
                    }
                )
            }
            bottomRow.addView(btnAdd)
            inner.addView(bottomRow)

            card.addView(inner)
            return ServicioViewHolder(card, iconBox, tvNombre, tvDescripcion, tvPrecio, btnAdd)
        }

        override fun onBindViewHolder(holder: ServicioViewHolder, position: Int) {
            val servicio = items[position]
            holder.servicio = servicio
            holder.tvNombre.text = servicio.nombre
            holder.tvDescripcion.text = servicio.descripcion?.takeIf { it.isNotBlank() }
                ?: "Servicio disponible en sucursal ApexGym"
            holder.tvPrecio.text = "${servicio.precio.formatearMonedaSinSufijo()} MXN"

            holder.iconBox.removeAllViews()
            holder.iconBox.addView(
                ImageView(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                    setImageResource(R.drawable.ic_fitness_service)
                }
            )
            cargarImagen(servicio.imagenUrl, holder.iconBox)

            holder.card.setOnClickListener { agregarServicio(servicio) }
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
        val btnAdd: FrameLayout
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

    private fun cargarImagen(ruta: String?, contenedor: FrameLayout) {
        val url = RetrofitClient.resolverUrlImagen(ruta) ?: return
        contenedor.tag = url

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = decodificarImagen(url)
            if (bitmap == null) return@launch

            withContext(Dispatchers.Main) {
                if (!isAdded || contenedor.tag != url) return@withContext
                contenedor.removeAllViews()
                contenedor.addView(
                    ImageView(requireContext()).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageBitmap(bitmap)
                    }
                )
            }
        }
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

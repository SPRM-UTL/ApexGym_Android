package com.sprm.apexgym.feature.main

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.transition.AutoTransition
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Base64
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.ClienteMembresiaDto
import com.sprm.apexgym.core.network.client.MiMembresiaResponseDto
import com.sprm.apexgym.core.network.client.ProductoClienteDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.client.ServicioClienteDto
import com.sprm.apexgym.core.network.client.TipoMembresiaDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var selectedTabIndex = 0
    private var isQrVisible = false
    private var pillAnimator: ValueAnimator? = null

    private var listaProductos: List<ProductoClienteDto> = emptyList()
    private var listaServicios: List<ServicioClienteDto> = emptyList()
    private var listaPlanesMembresia: List<TipoMembresiaDto> = emptyList()
    private var miMembresiaData: MiMembresiaResponseDto? = null
    private var categoriaSeleccionada: String = "Todos"
    private var textoBusqueda: String = ""
    private val bitmapCache = LruCache<String, Bitmap>(40)

    private data class NavTabItem(
        val container: LinearLayout,
        val icon: ImageView,
        val label: TextView
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollHome = view.findViewById<NestedScrollView>(R.id.scrollHome)
        val llMainContentRoot = view.findViewById<ViewGroup>(R.id.llMainContentRoot)
        val llTopHeaderAndSearch = view.findViewById<View>(R.id.llTopHeaderAndSearch)
        val llSearchBarRow = view.findViewById<View>(R.id.llSearchBarRow)
        val llHomeSection = view.findViewById<View>(R.id.llHomeSection)
        val llMembresiaSection = view.findViewById<View>(R.id.llMembresiaSection)
        val llTiendaSection = view.findViewById<View>(R.id.llTiendaSection)
        val llServiciosSection = view.findViewById<View>(R.id.llServiciosSection)
        val llQrAccessSection = view.findViewById<View>(R.id.llQrAccessSection)
        val etSearchMockup = view.findViewById<EditText>(R.id.etSearchMockup)

        val llHomePopularProductsContainer = view.findViewById<LinearLayout>(R.id.llHomePopularProductsContainer)
        val llTiendaCategoryChipsContainer = view.findViewById<LinearLayout>(R.id.llTiendaCategoryChipsContainer)
        val llTiendaProductsGridContainer = view.findViewById<LinearLayout>(R.id.llTiendaProductsGridContainer)
        val tvTiendaEmptyState = view.findViewById<TextView>(R.id.tvTiendaEmptyState)

        val llServiciosListContainer = view.findViewById<LinearLayout>(R.id.llServiciosListContainer)
        val tvServiciosEmptyState = view.findViewById<TextView>(R.id.tvServiciosEmptyState)

        val flNotifications = view.findViewById<View>(R.id.flNotifications)
        flNotifications?.setOnClickListener {
            val existing = childFragmentManager.findFragmentByTag(NotificationsBottomSheetFragment.TAG)
            if (existing == null) {
                NotificationsBottomSheetFragment().show(
                    childFragmentManager,
                    NotificationsBottomSheetFragment.TAG
                )
            }
        }

        val flAvatar = view.findViewById<View>(R.id.flAvatar)
        flAvatar?.setOnClickListener {
            val existing = childFragmentManager.findFragmentByTag(ProfileBottomSheetFragment.TAG)
            if (existing == null) {
                ProfileBottomSheetFragment().show(
                    childFragmentManager,
                    ProfileBottomSheetFragment.TAG
                )
            }
        }

        childFragmentManager.setFragmentResultListener(
            EditProfileBottomSheetFragment.RESULT_KEY_PERFIL_ACTUALIZADO,
            viewLifecycleOwner
        ) { _, _ ->
            cargarMembresiaDesdeBackend(view)
        }

        etSearchMockup?.doAfterTextChanged { editable ->
            textoBusqueda = editable?.toString()?.trim().orEmpty()
            renderizarGridProductos(llTiendaProductsGridContainer, tvTiendaEmptyState)
            renderizarListaServicios(llServiciosListContainer, tvServiciosEmptyState)
        }

        val llNavTabsRow = view.findViewById<LinearLayout>(R.id.llNavTabsRow)
        val viewActiveNavPill = view.findViewById<View>(R.id.viewActiveNavPill)

        val tabs = listOf(
            NavTabItem(
                view.findViewById(R.id.navItemInicio),
                view.findViewById(R.id.ivNavInicio),
                view.findViewById(R.id.tvNavInicio)
            ),
            NavTabItem(
                view.findViewById(R.id.navItemMembresia),
                view.findViewById(R.id.ivNavMembresia),
                view.findViewById(R.id.tvNavMembresia)
            ),
            NavTabItem(
                view.findViewById(R.id.navItemTienda),
                view.findViewById(R.id.ivNavTienda),
                view.findViewById(R.id.tvNavTienda)
            ),
            NavTabItem(
                view.findViewById(R.id.navItemPerfil),
                view.findViewById(R.id.ivNavPerfil),
                view.findViewById(R.id.tvNavPerfil)
            )
        )

        fun showSectionForTab(tabIndex: Int) {
            isQrVisible = false

            val transition = AutoTransition().apply {
                duration = 220L
                interpolator = FastOutSlowInInterpolator()
            }
            TransitionManager.beginDelayedTransition(llMainContentRoot, transition)

            llQrAccessSection.visibility = View.GONE
            llTopHeaderAndSearch.visibility = View.VISIBLE

            when (tabIndex) {
                1 -> {
                    llHomeSection.visibility = View.GONE
                    llTiendaSection.visibility = View.GONE
                    llServiciosSection.visibility = View.GONE
                    llMembresiaSection?.visibility = View.VISIBLE
                    llSearchBarRow?.visibility = View.GONE
                }
                2 -> {
                    llHomeSection.visibility = View.GONE
                    llMembresiaSection?.visibility = View.GONE
                    llServiciosSection.visibility = View.GONE
                    llTiendaSection.visibility = View.VISIBLE
                    llSearchBarRow?.visibility = View.VISIBLE
                    etSearchMockup.setHint(R.string.tienda_search_hint)
                }
                3 -> {
                    llHomeSection.visibility = View.GONE
                    llMembresiaSection?.visibility = View.GONE
                    llTiendaSection.visibility = View.GONE
                    llServiciosSection.visibility = View.VISIBLE
                    llSearchBarRow?.visibility = View.VISIBLE
                    etSearchMockup.setHint(R.string.servicios_search_hint)
                }
                else -> {
                    llMembresiaSection?.visibility = View.GONE
                    llTiendaSection.visibility = View.GONE
                    llServiciosSection.visibility = View.GONE
                    llHomeSection.visibility = View.VISIBLE
                    llSearchBarRow?.visibility = View.VISIBLE
                    etSearchMockup.setHint(R.string.home_search_hint)
                }
            }

            scrollHome.smoothScrollTo(0, 0)
        }

        llNavTabsRow.post {
            val activeTab = tabs[selectedTabIndex].container
            viewActiveNavPill.translationX = activeTab.left.toFloat()
            val pillParams = viewActiveNavPill.layoutParams
            pillParams.width = activeTab.width
            viewActiveNavPill.layoutParams = pillParams
        }

        tabs.forEachIndexed { index, tabItem ->
            tabItem.container.setOnClickListener {
                if (isQrVisible && index == selectedTabIndex) {
                    showSectionForTab(index)
                } else {
                    animateBottomNavSelection(
                        targetIndex = index,
                        tabs = tabs,
                        llNavTabsRow = llNavTabsRow,
                        viewActiveNavPill = viewActiveNavPill
                    )
                    showSectionForTab(index)
                }

                if (index == 1) {
                    cargarMembresiaDesdeBackend(view)
                } else {
                    cargarCatalogoDesdeBackend(
                        llHomePopularProductsContainer = llHomePopularProductsContainer,
                        llTiendaCategoryChipsContainer = llTiendaCategoryChipsContainer,
                        llTiendaProductsGridContainer = llTiendaProductsGridContainer,
                        tvTiendaEmptyState = tvTiendaEmptyState,
                        llServiciosListContainer = llServiciosListContainer,
                        tvServiciosEmptyState = tvServiciosEmptyState
                    )
                }
            }
        }

        val goToMembresiaListener = View.OnClickListener {
            animateBottomNavSelection(
                targetIndex = 1,
                tabs = tabs,
                llNavTabsRow = llNavTabsRow,
                viewActiveNavPill = viewActiveNavPill
            )
            showSectionForTab(1)
            cargarMembresiaDesdeBackend(view)
        }
        view.findViewById<View>(R.id.cardPagos)?.setOnClickListener(goToMembresiaListener)
        view.findViewById<View>(R.id.btnPagarMockup)?.setOnClickListener(goToMembresiaListener)
        view.findViewById<View>(R.id.chipHomeMembresia)?.setOnClickListener(goToMembresiaListener)

        val goToProductosListener = View.OnClickListener {
            animateBottomNavSelection(
                targetIndex = 2,
                tabs = tabs,
                llNavTabsRow = llNavTabsRow,
                viewActiveNavPill = viewActiveNavPill
            )
            showSectionForTab(2)
        }
        view.findViewById<View>(R.id.cardTienda)?.setOnClickListener(goToProductosListener)
        view.findViewById<View>(R.id.btnComprarMockup)?.setOnClickListener(goToProductosListener)
        view.findViewById<View>(R.id.btnVerCatalogoMockup)?.setOnClickListener(goToProductosListener)
        view.findViewById<View>(R.id.chipHomeInsumos)?.setOnClickListener(goToProductosListener)

        val flQrCodeBox = view.findViewById<View>(R.id.flQrCodeBox)
        val btnGenerarNuevoQr = view.findViewById<View>(R.id.btnGenerarNuevoQr)

        val openQrAccessListener = View.OnClickListener {
            val transition = AutoTransition().apply {
                duration = 240L
                interpolator = FastOutSlowInInterpolator()
            }
            TransitionManager.beginDelayedTransition(llMainContentRoot, transition)

            isQrVisible = true
            llTopHeaderAndSearch.visibility = View.GONE
            llHomeSection.visibility = View.GONE
            llMembresiaSection?.visibility = View.GONE
            llTiendaSection.visibility = View.GONE
            llServiciosSection.visibility = View.GONE
            llQrAccessSection.visibility = View.VISIBLE

            scrollHome.smoothScrollTo(0, 0)
        }

        view.findViewById<View>(R.id.fabQrMockup)?.setOnClickListener(openQrAccessListener)
        view.findViewById<View>(R.id.chipHomeAccesoQr)?.setOnClickListener(openQrAccessListener)
        view.findViewById<View>(R.id.btnMembresiaAbrirQr)?.setOnClickListener(openQrAccessListener)

        val closeQrListener = View.OnClickListener {
            showSectionForTab(selectedTabIndex)
        }
        view.findViewById<View>(R.id.btnQrBack)?.setOnClickListener(closeQrListener)
        view.findViewById<View>(R.id.btnQrClose)?.setOnClickListener(closeQrListener)

        btnGenerarNuevoQr?.setOnClickListener {
            flQrCodeBox?.animate()
                ?.scaleX(0.94f)
                ?.scaleY(0.94f)
                ?.setDuration(110L)
                ?.withEndAction {
                    flQrCodeBox.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160L)
                        .start()
                }
                ?.start()
        }

        cargarCatalogoDesdeBackend(
            llHomePopularProductsContainer = llHomePopularProductsContainer,
            llTiendaCategoryChipsContainer = llTiendaCategoryChipsContainer,
            llTiendaProductsGridContainer = llTiendaProductsGridContainer,
            tvTiendaEmptyState = tvTiendaEmptyState,
            llServiciosListContainer = llServiciosListContainer,
            tvServiciosEmptyState = tvServiciosEmptyState
        )
        cargarMembresiaDesdeBackend(view)
    }

    private fun cargarCatalogoDesdeBackend(
        llHomePopularProductsContainer: LinearLayout?,
        llTiendaCategoryChipsContainer: LinearLayout?,
        llTiendaProductsGridContainer: LinearLayout?,
        tvTiendaEmptyState: TextView?,
        llServiciosListContainer: LinearLayout?,
        tvServiciosEmptyState: TextView?
    ) {
        tvTiendaEmptyState?.visibility = View.VISIBLE
        tvTiendaEmptyState?.text = "Cargando productos disponibles..."

        tvServiciosEmptyState?.visibility = View.VISIBLE
        tvServiciosEmptyState?.text = "Cargando servicios disponibles..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.obtenerProductosCliente()
                if (response.isSuccessful && response.body()?.responseFlag == 0) {
                    listaProductos = response.body()?.data.orEmpty()
                } else {
                    listaProductos = emptyList()
                }
            } catch (_: Exception) {
                listaProductos = emptyList()
            }

            if (!isAdded) return@launch
            construirChipsCategoriasReales(
                llTiendaCategoryChipsContainer,
                llTiendaProductsGridContainer,
                tvTiendaEmptyState
            )
            renderizarProductosPopularesInicio(llHomePopularProductsContainer)
            renderizarGridProductos(llTiendaProductsGridContainer, tvTiendaEmptyState)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.obtenerServiciosCliente()
                if (response.isSuccessful && response.body()?.responseFlag == 0) {
                    listaServicios = response.body()?.data.orEmpty()
                } else {
                    listaServicios = emptyList()
                }
            } catch (_: Exception) {
                listaServicios = emptyList()
            }

            if (!isAdded) return@launch
            renderizarListaServicios(llServiciosListContainer, tvServiciosEmptyState)
        }
    }

    private fun construirChipsCategoriasReales(
        chipsContainer: LinearLayout?,
        gridContainer: LinearLayout?,
        emptyStateView: TextView?
    ) {
        if (chipsContainer == null) return
        chipsContainer.removeAllViews()

        val categoriasUnicas = mutableListOf("Todos")
        listaProductos
            .mapNotNull { it.categoria?.nombre?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { categoriasUnicas.add(it) }

        if (categoriaSeleccionada !in categoriasUnicas) {
            categoriaSeleccionada = "Todos"
        }

        val chipsViews = mutableListOf<TextView>()

        categoriasUnicas.forEachIndexed { index, nombreCategoria ->
            val chip = TextView(requireContext()).apply {
                text = nombreCategoria
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dpToPx(18), 0, dpToPx(18), 0)
                isClickable = true
                isFocusable = true

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(38)
                )
                if (index > 0) {
                    params.marginStart = dpToPx(8)
                }
                layoutParams = params
            }

            chipsViews.add(chip)
            chipsContainer.addView(chip)
        }

        fun actualizarEstiloChips() {
            val context = requireContext()
            val colorWhite = ContextCompat.getColor(context, R.color.white)
            val colorPrimary = ContextCompat.getColor(context, R.color.apex_text_primary)

            chipsViews.forEach { chip ->
                if (chip.text.toString() == categoriaSeleccionada) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(colorWhite)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                    chip.setTextColor(colorPrimary)
                }
            }
        }

        actualizarEstiloChips()

        chipsViews.forEach { chip ->
            chip.setOnClickListener {
                categoriaSeleccionada = chip.text.toString()
                actualizarEstiloChips()
                renderizarGridProductos(gridContainer, emptyStateView)
            }
        }
    }

    private fun renderizarProductosPopularesInicio(container: LinearLayout?) {
        if (container == null) return
        container.removeAllViews()

        if (listaProductos.isEmpty()) {
            val tvVacio = TextView(requireContext()).apply {
                text = "No hay productos disponibles por el momento"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setPadding(dpToPx(4), dpToPx(16), dpToPx(4), dpToPx(16))
            }
            container.addView(tvVacio)
            return
        }

        val productosDestacados = listaProductos.take(8)
        productosDestacados.forEachIndexed { index, producto ->
            val card = crearTarjetaProductoCarruselInicio(
                producto = producto,
                esUltimo = index == productosDestacados.lastIndex
            )
            container.addView(card)
        }
    }

    private fun renderizarGridProductos(
        gridContainer: LinearLayout?,
        emptyStateView: TextView?
    ) {
        if (gridContainer == null) return
        gridContainer.removeAllViews()

        val filtrados = listaProductos.filter { prod ->
            val coincideCategoria = categoriaSeleccionada == "Todos" ||
                prod.categoria?.nombre?.equals(categoriaSeleccionada, ignoreCase = true) == true

            val coincideBusqueda = textoBusqueda.isEmpty() ||
                prod.nombre.contains(textoBusqueda, ignoreCase = true) ||
                prod.descripcion?.contains(textoBusqueda, ignoreCase = true) == true ||
                prod.categoria?.nombre?.contains(textoBusqueda, ignoreCase = true) == true

            coincideCategoria && coincideBusqueda
        }

        if (filtrados.isEmpty()) {
            emptyStateView?.visibility = View.VISIBLE
            emptyStateView?.text = if (listaProductos.isEmpty()) {
                "No hay productos disponibles para compra actualmente."
            } else {
                "No se encontraron productos con ese filtro."
            }
            return
        }

        emptyStateView?.visibility = View.GONE

        filtrados.chunked(2).forEach { parProductos ->
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
                isBaselineAligned = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(14)
                }
            }

            val cardIzquierda = crearTarjetaProductoTiendaGrid(
                producto = parProductos[0],
                esColumnaIzquierda = true
            )
            fila.addView(cardIzquierda)

            if (parProductos.size > 1) {
                val cardDerecha = crearTarjetaProductoTiendaGrid(
                    producto = parProductos[1],
                    esColumnaIzquierda = false
                )
                fila.addView(cardDerecha)
            } else {
                val espacioDerecho = Space(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginStart = dpToPx(7)
                    }
                }
                fila.addView(espacioDerecho)
            }

            gridContainer.addView(fila)
        }
    }

    private fun renderizarListaServicios(
        listContainer: LinearLayout?,
        emptyStateView: TextView?
    ) {
        if (listContainer == null) return
        listContainer.removeAllViews()

        val filtrados = listaServicios.filter { serv ->
            textoBusqueda.isEmpty() ||
                serv.nombre.contains(textoBusqueda, ignoreCase = true) ||
                serv.descripcion?.contains(textoBusqueda, ignoreCase = true) == true
        }

        if (filtrados.isEmpty()) {
            emptyStateView?.visibility = View.VISIBLE
            emptyStateView?.text = if (listaServicios.isEmpty()) {
                "No hay servicios disponibles para compra actualmente."
            } else {
                "No se encontraron servicios que coincidan con tu búsqueda."
            }
            return
        }

        emptyStateView?.visibility = View.GONE

        filtrados.forEach { servicio ->
            listContainer.addView(crearTarjetaServicio(servicio))
        }
    }

    private fun crearTarjetaProductoCarruselInicio(
        producto: ProductoClienteDto,
        esUltimo: Boolean
    ): MaterialCardView {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(148), dpToPx(202)).apply {
                if (!esUltimo) marginEnd = dpToPx(12)
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
            radius = dpToPx(18).toFloat()
            cardElevation = dpToPx(3).toFloat()
            strokeWidth = 0
        }

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        val imgBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(86)
            )
            setBackgroundResource(R.drawable.bg_product_image_container)
            clipToOutline = true

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(38), dpToPx(38), Gravity.CENTER)
                setImageResource(R.drawable.ic_supplement_bottle)
            }
            addView(icon)
        }
        cargarImagenEnContenedor(producto.imagenUrl, imgBox)
        inner.addView(imgBox)

        val tvNombre = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            text = producto.nombre
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(tvNombre)

        val tvCategoria = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            text = producto.categoria?.nombre ?: "Producto"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(tvCategoria)

        inner.addView(
            Space(context).apply {
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
            text = "${formatearPrecio(producto.precioVenta)} MXN"
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        bottomRow.addView(tvPrecio)

        val btnAdd = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(26), dpToPx(26))
            setBackgroundResource(R.drawable.bg_circle_orange)
            val plusIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(14), dpToPx(14), Gravity.CENTER)
                setImageResource(R.drawable.ic_plus)
            }
            addView(plusIcon)
            setOnClickListener {
                abrirDetalleProducto(producto)
            }
        }
        bottomRow.addView(btnAdd)

        inner.addView(bottomRow)
        card.addView(inner)

        card.setOnClickListener {
            abrirDetalleProducto(producto)
        }

        return card
    }

    private fun crearTarjetaProductoTiendaGrid(
        producto: ProductoClienteDto,
        esColumnaIzquierda: Boolean
    ): MaterialCardView {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(232), 1f).apply {
                if (esColumnaIzquierda) {
                    marginEnd = dpToPx(7)
                } else {
                    marginStart = dpToPx(7)
                }
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
            radius = dpToPx(22).toFloat()
            cardElevation = dpToPx(3).toFloat()
            strokeWidth = 0
        }

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val imgBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(108)
            )
            setBackgroundResource(R.drawable.bg_product_image_container)
            clipToOutline = true

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(44), dpToPx(44), Gravity.CENTER)
                setImageResource(R.drawable.ic_supplement_bottle)
            }
            addView(icon)
        }
        cargarImagenEnContenedor(producto.imagenUrl, imgBox)
        inner.addView(imgBox)

        val tvNombre = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            text = producto.nombre
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(tvNombre)

        val subtituloCategoria = buildString {
            append(producto.categoria?.nombre ?: "General")
            if (producto.stockActual > 0) {
                append(" • Stock: ${producto.stockActual}")
            }
        }

        val tvCategoria = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            text = subtituloCategoria
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        inner.addView(tvCategoria)

        inner.addView(
            Space(context).apply {
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
            text = "${formatearPrecio(producto.precioVenta)} MXN"
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        bottomRow.addView(tvPrecio)

        val btnAdd = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            setBackgroundResource(R.drawable.bg_circle_orange)
            val plusIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(16), dpToPx(16), Gravity.CENTER)
                setImageResource(R.drawable.ic_plus)
            }
            addView(plusIcon)
            setOnClickListener {
                abrirDetalleProducto(producto)
            }
        }
        bottomRow.addView(btnAdd)

        inner.addView(bottomRow)
        card.addView(inner)

        card.setOnClickListener {
            abrirDetalleProducto(producto)
        }

        return card
    }

    private fun crearTarjetaServicio(servicio: ServicioClienteDto): MaterialCardView {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
            radius = dpToPx(22).toFloat()
            cardElevation = dpToPx(3).toFloat()
            strokeWidth = dpToPx(1)
            strokeColor = 0x1AFFFFFF
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(16))
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val iconBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
            setBackgroundResource(R.drawable.bg_product_image_container)
            clipToOutline = true

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(24), dpToPx(24), Gravity.CENTER)
                setImageResource(R.drawable.ic_fitness_service)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_orange))
                )
            }
            addView(icon)
        }
        cargarImagenEnContenedor(servicio.imagenUrl, iconBox)
        topRow.addView(iconBox)

        val infoColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(14)
            }
        }

        val tvNombre = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = servicio.nombre
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        }
        infoColumn.addView(tvNombre)

        val descripcionTexto = servicio.descripcion?.takeIf { it.isNotBlank() }
            ?: "Servicio disponible para socios en sucursal ApexGym"

        val tvDescripcion = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
            text = descripcionTexto
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(0f, 1.15f)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        infoColumn.addView(tvDescripcion)

        topRow.addView(infoColumn)
        container.addView(topRow)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(14)
                bottomMargin = dpToPx(12)
            }
            setBackgroundColor(0x1FFFFFFF)
        }
        container.addView(divider)

        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tvPrecio = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = "${formatearPrecio(servicio.precio)} MXN"
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        bottomRow.addView(tvPrecio)

        val btnAdd = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34))
            setBackgroundResource(R.drawable.bg_circle_orange)

            val plusIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(16), dpToPx(16), Gravity.CENTER)
                setImageResource(R.drawable.ic_plus)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                )
            }
            addView(plusIcon)

            setOnClickListener {
                abrirDetalleServicio(servicio)
            }
        }
        bottomRow.addView(btnAdd)

        container.addView(bottomRow)
        card.addView(container)

        card.setOnClickListener {
            abrirDetalleServicio(servicio)
        }

        return card
    }

    private fun abrirDetalleProducto(producto: ProductoClienteDto) {
        val existing = childFragmentManager.findFragmentByTag(ProductDetailBottomSheetFragment.TAG)
        if (existing == null) {
            ProductDetailBottomSheetFragment.newInstance(producto)
                .show(childFragmentManager, ProductDetailBottomSheetFragment.TAG)
        }
    }

    private fun abrirDetalleServicio(servicio: ServicioClienteDto) {
        val existing = childFragmentManager.findFragmentByTag(ServiceDetailBottomSheetFragment.TAG)
        if (existing == null) {
            ServiceDetailBottomSheetFragment.newInstance(servicio)
                .show(childFragmentManager, ServiceDetailBottomSheetFragment.TAG)
        }
    }

    private fun cargarMembresiaDesdeBackend(rootView: View) {
        val tvMembresiaPlanesEmpty = rootView.findViewById<TextView>(R.id.tvMembresiaPlanesEmpty)
        tvMembresiaPlanesEmpty?.visibility = View.VISIBLE
        tvMembresiaPlanesEmpty?.text = "Cargando planes de membresía..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resMiMembresia = RetrofitClient.authService.obtenerMiMembresia()
                if (resMiMembresia.isSuccessful && resMiMembresia.body()?.responseFlag == 0) {
                    miMembresiaData = resMiMembresia.body()?.data
                }
            } catch (_: Exception) {
            }

            try {
                val resPlanes = RetrofitClient.authService.obtenerTiposMembresiaCliente()
                if (resPlanes.isSuccessful && resPlanes.body()?.responseFlag == 0) {
                    listaPlanesMembresia = resPlanes.body()?.data.orEmpty()
                } else {
                    listaPlanesMembresia = emptyList()
                }
            } catch (_: Exception) {
                listaPlanesMembresia = emptyList()
            }

            if (!isAdded) return@launch
            renderizarSeccionMembresia(rootView)
        }
    }

    private fun renderizarSeccionMembresia(rootView: View) {
        val tvMembresiaEstadoBadge = rootView.findViewById<TextView>(R.id.tvMembresiaEstadoBadge)
        val tvMembresiaRenovacionBadge = rootView.findViewById<TextView>(R.id.tvMembresiaRenovacionBadge)
        val tvMembresiaPlanActualNombre = rootView.findViewById<TextView>(R.id.tvMembresiaPlanActualNombre)
        val tvMembresiaDiasRestantesNumero = rootView.findViewById<TextView>(R.id.tvMembresiaDiasRestantesNumero)
        val tvMembresiaDiasRestantesTexto = rootView.findViewById<TextView>(R.id.tvMembresiaDiasRestantesTexto)
        val tvMembresiaFechaInicio = rootView.findViewById<TextView>(R.id.tvMembresiaFechaInicio)
        val tvMembresiaFechaFin = rootView.findViewById<TextView>(R.id.tvMembresiaFechaFin)
        val tvMembresiaPlanesEmpty = rootView.findViewById<TextView>(R.id.tvMembresiaPlanesEmpty)
        val llMembresiaPlanesContainer = rootView.findViewById<LinearLayout>(R.id.llMembresiaPlanesContainer)
        val llMembresiaHistorialWrapper = rootView.findViewById<View>(R.id.llMembresiaHistorialWrapper)
        val llMembresiaHistorialContainer = rootView.findViewById<LinearLayout>(R.id.llMembresiaHistorialContainer)

        val cliente = miMembresiaData?.cliente
        if (cliente != null && !cliente.nombre.isNullOrBlank()) {
            val tvGreeting = rootView.findViewById<TextView>(R.id.tvGreeting)
            tvGreeting?.text = "Hola, ${cliente.nombre.trim().split(" ").firstOrNull() ?: cliente.nombre}"
        }

        val activa = miMembresiaData?.membresiaActiva
        if (activa != null) {
            val estadoNombre = (activa.estadoMembresia?.nombre ?: "VIGENTE").uppercase(Locale.getDefault())
            tvMembresiaEstadoBadge?.text = "MEMBRESÍA $estadoNombre"
            tvMembresiaRenovacionBadge?.text = if (activa.renovacionAutomatica) {
                "Renovación automática"
            } else {
                "Renovación manual"
            }
            tvMembresiaPlanActualNombre?.text = activa.tipoMembresia?.nombre ?: "Plan Activo ApexGym"

            val diasRestantes = calcularDiasRestantes(activa.fechaFin)
            if (diasRestantes != null) {
                tvMembresiaDiasRestantesNumero?.text = diasRestantes.coerceAtLeast(0).toString()
                tvMembresiaDiasRestantesTexto?.text = if (diasRestantes == 1L) {
                    "día restante de vigencia"
                } else {
                    "días restantes de vigencia"
                }
            } else {
                tvMembresiaDiasRestantesNumero?.text = (activa.tipoMembresia?.duracionDias ?: 30).toString()
                tvMembresiaDiasRestantesTexto?.text = "días de duración del plan"
            }

            tvMembresiaFechaInicio?.text = formatearFechaCorta(activa.fechaInicio)
            tvMembresiaFechaFin?.text = formatearFechaCorta(activa.fechaFin)
        } else {
            tvMembresiaEstadoBadge?.text = "SIN MEMBRESÍA ACTIVA"
            tvMembresiaRenovacionBadge?.text = "Inactiva"
            tvMembresiaPlanActualNombre?.text = "Activa tu plan en ApexGym"
            tvMembresiaDiasRestantesNumero?.text = "0"
            tvMembresiaDiasRestantesTexto?.text = "días vigentes • Elige un plan abajo"
            tvMembresiaFechaInicio?.text = "Sin registro"
            tvMembresiaFechaFin?.text = "Sin vigencia"
        }

        llMembresiaPlanesContainer?.removeAllViews()
        if (listaPlanesMembresia.isEmpty()) {
            tvMembresiaPlanesEmpty?.visibility = View.VISIBLE
            tvMembresiaPlanesEmpty?.text = "No hay planes de membresía registrados por el momento"
        } else {
            tvMembresiaPlanesEmpty?.visibility = View.GONE
            val idPlanActivo = activa?.tipoMembresia?.id ?: activa?.tipoMembresiaId
            listaPlanesMembresia.forEach { plan ->
                val esPlanActual = idPlanActivo != null && idPlanActivo == plan.id && activa != null
                llMembresiaPlanesContainer?.addView(crearTarjetaPlanMembresia(plan, esPlanActual))
            }
        }

        val historial = miMembresiaData?.historial.orEmpty()
        llMembresiaHistorialContainer?.removeAllViews()
        if (historial.isEmpty()) {
            llMembresiaHistorialWrapper?.visibility = View.GONE
        } else {
            llMembresiaHistorialWrapper?.visibility = View.VISIBLE
            historial.take(5).forEach { item ->
                llMembresiaHistorialContainer?.addView(crearTarjetaHistorialMembresia(item))
            }
        }
    }

    private fun crearTarjetaPlanMembresia(
        plan: TipoMembresiaDto,
        esPlanActual: Boolean
    ): MaterialCardView {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
            radius = dpToPx(22).toFloat()
            cardElevation = dpToPx(3).toFloat()
            strokeWidth = if (esPlanActual) dpToPx(2) else dpToPx(1)
            strokeColor = if (esPlanActual) {
                ContextCompat.getColor(context, R.color.apex_orange)
            } else {
                0x1AFFFFFF
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(16))
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tvDuracionBadge = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg_badge_vip_outline)
            setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
            text = "${plan.duracionDias} DÍAS"
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        topRow.addView(tvDuracionBadge)

        if (esPlanActual) {
            val tvActualBadge = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(8)
                }
                setBackgroundResource(R.drawable.bg_chip_selected)
                setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                text = "TU PLAN ACTUAL"
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTypeface(typeface, Typeface.BOLD)
            }
            topRow.addView(tvActualBadge)
        }

        container.addView(topRow)

        val tvNombre = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
            text = plan.nombre
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
        }
        container.addView(tvNombre)

        val desc = plan.descripcion?.takeIf { it.isNotBlank() }
            ?: "Acceso completo a instalaciones y áreas de entrenamiento en sucursal ApexGym."

        val tvDescripcion = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
            text = desc
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(0f, 1.15f)
        }
        container.addView(tvDescripcion)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(14)
                bottomMargin = dpToPx(12)
            }
            setBackgroundColor(0x1FFFFFFF)
        }
        container.addView(divider)

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
            text = "${formatearPrecio(plan.precio)} MXN"
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
        }
        bottomRow.addView(tvPrecio)

        val btnElegir = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(36)
            )
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            setBackgroundResource(
                if (esPlanActual) R.drawable.bg_badge_vip_outline else R.drawable.bg_badge_orange
            )
            text = if (esPlanActual) "Renovar" else "Solicitar plan"
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        bottomRow.addView(btnElegir)

        container.addView(bottomRow)
        card.addView(container)

        card.setOnClickListener {
            Toast.makeText(
                context,
                "${plan.nombre} (${plan.duracionDias} días • ${formatearPrecio(plan.precio)} MXN). Solicítalo en recepción.",
                Toast.LENGTH_LONG
            ).show()
        }

        return card
    }

    private fun crearTarjetaHistorialMembresia(item: ClienteMembresiaDto): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            setBackgroundResource(R.drawable.bg_profile_option_card)
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
        }

        val iconCircle = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setBackgroundResource(R.drawable.bg_icon_circle_orange_outline)

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(18), dpToPx(18), Gravity.CENTER)
                setImageResource(R.drawable.ic_membership_card)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_orange))
                )
            }
            addView(icon)
        }
        row.addView(iconCircle)

        val infoCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
                marginEnd = dpToPx(8)
            }
        }

        val tvPlan = TextView(context).apply {
            text = item.tipoMembresia?.nombre ?: "Membresía ApexGym"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        infoCol.addView(tvPlan)

        val tvFechas = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            text = "${formatearFechaCorta(item.fechaInicio)} al ${formatearFechaCorta(item.fechaFin)}"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        infoCol.addView(tvFechas)
        row.addView(infoCol)

        val estadoStr = (item.estadoMembresia?.nombre ?: "Registrada").uppercase(Locale.getDefault())
        val tvEstado = TextView(context).apply {
            text = estadoStr
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(
                if (estadoStr.contains("VIGENTE")) {
                    ContextCompat.getColor(context, R.color.apex_orange)
                } else {
                    ContextCompat.getColor(context, R.color.apex_text_secondary)
                }
            )
        }
        row.addView(tvEstado)

        return row
    }

    private fun calcularDiasRestantes(fechaFinIso: String?): Long? {
        if (fechaFinIso.isNullOrBlank()) return null
        return try {
            val fechaStr = fechaFinIso.take(10)
            val fin = LocalDate.parse(fechaStr)
            val hoy = LocalDate.now()
            ChronoUnit.DAYS.between(hoy, fin)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatearFechaCorta(fechaIso: String?): String {
        if (fechaIso.isNullOrBlank()) return "—"
        return try {
            val fechaStr = fechaIso.take(10)
            val date = LocalDate.parse(fechaStr)
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "MX"))
            date.format(formatter)
        } catch (_: Exception) {
            fechaIso.take(10)
        }
    }

    private fun formatearPrecio(monto: Double): String {
        val formato = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (monto % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$${formato.format(monto)}"
    }

    private fun cargarImagenEnContenedor(rutaImagen: String?, contenedor: FrameLayout) {
        val urlCompleta = RetrofitClient.resolverUrlImagen(rutaImagen) ?: return

        val cached = bitmapCache.get(urlCompleta)
        if (cached != null) {
            mostrarBitmapEnContenedor(cached, contenedor)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = try {
                if (urlCompleta.startsWith("data:image")) {
                    val base64Part = urlCompleta.substringAfter("base64,", "")
                    if (base64Part.isNotEmpty()) {
                        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                } else {
                    URL(urlCompleta).openStream().use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
            } catch (_: Exception) {
                null
            }

            if (bitmap != null) {
                bitmapCache.put(urlCompleta, bitmap)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        mostrarBitmapEnContenedor(bitmap, contenedor)
                    }
                }
            }
        }
    }

    private fun mostrarBitmapEnContenedor(bitmap: Bitmap, contenedor: FrameLayout) {
        val context = contenedor.context
        contenedor.removeAllViews()
        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bitmap)
        }
        contenedor.addView(imageView)
    }

    private fun animateBottomNavSelection(
        targetIndex: Int,
        tabs: List<NavTabItem>,
        llNavTabsRow: LinearLayout,
        viewActiveNavPill: View
    ) {
        if (targetIndex == selectedTabIndex) return
        selectedTabIndex = targetIndex

        val context = requireContext()
        val colorActive = ContextCompat.getColor(context, R.color.apex_text_primary)
        val colorInactive = ContextCompat.getColor(context, R.color.apex_nav_inactive)

        val startX = viewActiveNavPill.translationX
        val startWidth = viewActiveNavPill.width
        val targetContainer = tabs[targetIndex].container

        llNavTabsRow.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                llNavTabsRow.viewTreeObserver.removeOnPreDrawListener(this)

                val endX = targetContainer.left.toFloat()
                val endWidth = targetContainer.width

                pillAnimator?.cancel()
                pillAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 320L
                    interpolator = FastOutSlowInInterpolator()
                    addUpdateListener { anim ->
                        val fraction = anim.animatedFraction
                        viewActiveNavPill.translationX = startX + (endX - startX) * fraction
                        val currentWidth = (startWidth + (endWidth - startWidth) * fraction).toInt()
                        val params = viewActiveNavPill.layoutParams
                        params.width = currentWidth
                        viewActiveNavPill.layoutParams = params
                    }
                    start()
                }
                return true
            }
        })

        val transition = ChangeBounds().apply {
            duration = 320L
            interpolator = FastOutSlowInInterpolator()
        }
        TransitionManager.beginDelayedTransition(llNavTabsRow, transition)

        tabs.forEachIndexed { index, item ->
            val isSelected = index == targetIndex

            if (isSelected) {
                item.container.orientation = LinearLayout.HORIZONTAL
                item.container.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(48),
                    0f
                )
                item.container.setPadding(dpToPx(16), 0, dpToPx(16), 0)

                ImageViewCompat.setImageTintList(item.icon, ColorStateList.valueOf(colorActive))

                val labelParams = item.label.layoutParams as LinearLayout.LayoutParams
                labelParams.topMargin = 0
                labelParams.marginStart = dpToPx(6)
                item.label.layoutParams = labelParams
                item.label.setTextColor(colorActive)
                item.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            } else {
                item.container.orientation = LinearLayout.VERTICAL
                item.container.layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
                )
                item.container.setPadding(0, 0, 0, 0)

                ImageViewCompat.setImageTintList(item.icon, ColorStateList.valueOf(colorInactive))

                val labelParams = item.label.layoutParams as LinearLayout.LayoutParams
                labelParams.topMargin = dpToPx(3)
                labelParams.marginStart = 0
                item.label.layoutParams = labelParams
                item.label.setTextColor(colorInactive)
                item.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroyView() {
        pillAnimator?.cancel()
        pillAnimator = null
        super.onDestroyView()
    }
}

package com.sprm.apexgym.feature.main

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.notifications.ApexGymMessagingService
import com.sprm.apexgym.core.network.client.ClienteMembresiaDto
import com.sprm.apexgym.core.network.client.MiMembresiaResponseDto
import com.sprm.apexgym.core.network.client.ProductoClienteDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.client.ServicioClienteDto
import com.sprm.apexgym.core.network.client.TipoMembresiaDto
import com.sprm.apexgym.core.network.repository.NotificacionRepository
import com.sprm.apexgym.core.network.repository.ResultadoNotificaciones
import com.sprm.apexgym.core.ui.SkeletonViewHelper
import com.sprm.apexgym.feature.tienda.CalculoVentaPos
import com.sprm.apexgym.feature.tienda.HistorialComprasFragment
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
    private var pillAnimating = false

    private var listaProductos: List<ProductoClienteDto> = emptyList()
    private var listaServicios: List<ServicioClienteDto> = emptyList()
    private var listaPlanesMembresia: List<TipoMembresiaDto> = emptyList()
    private var miMembresiaData: MiMembresiaResponseDto? = null
    private var cargandoMembresia: Boolean = true
    private var errorCargaMembresia: Boolean = false
    private var catalogoCargado: Boolean = false
    private var membresiaCargada: Boolean = false
    private var categoriaSeleccionada: String = "Todos"
    private var textoBusqueda: String = ""
    private var cartListener: (() -> Unit)? = null
    private var notificacionReceiverRegistrado = false
    private val notificacionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            view?.let { cargarContadorNotificaciones(it) }
        }
    }
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
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(
                    R.id.fragment_container,
                    com.sprm.apexgym.feature.notificaciones.NotificacionesListFragment()
                )
                .addToBackStack(null)
                .commit()
        }
        cargarContadorNotificaciones(view)

        val flCartHeader = view.findViewById<View>(R.id.flCartHeader)
        val tvCartHeaderBadge = view.findViewById<TextView>(R.id.tvCartHeaderBadge)

        fun actualizarBadgeCarrito() {
            if (!isAdded) return
            val totalArticulos = CartManager.obtenerCantidadTotalArticulos(requireContext())
            if (totalArticulos > 0) {
                tvCartHeaderBadge?.visibility = View.VISIBLE
                tvCartHeaderBadge?.text = if (totalArticulos > 99) "99+" else totalArticulos.toString()
            } else {
                tvCartHeaderBadge?.visibility = View.GONE
            }
        }

        actualizarBadgeCarrito()

        flCartHeader?.setOnClickListener {
            val fm = try { requireActivity().supportFragmentManager } catch (_: Exception) { parentFragmentManager }
            val existing = fm.findFragmentByTag(CartBottomSheetFragment.TAG)
            if (existing == null) {
                CartBottomSheetFragment().show(
                    fm,
                    CartBottomSheetFragment.TAG
                )
            }
        }

        val listenerCarrito: () -> Unit = {
            if (isAdded && this@HomeFragment.view != null) {
                actualizarBadgeCarrito()
                if (listaPlanesMembresia.isNotEmpty() || miMembresiaData != null) {
                    renderizarSeccionMembresia(view)
                }
            }
        }
        cartListener = listenerCarrito
        CartManager.addListener(listenerCarrito)

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

        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh?.setColorSchemeResources(R.color.apex_orange)
        swipeRefresh?.setOnRefreshListener {
            catalogoCargado = false
            membresiaCargada = false
            cargarCatalogoDesdeBackend(
                llHomePopularProductsContainer = llHomePopularProductsContainer,
                llTiendaCategoryChipsContainer = llTiendaCategoryChipsContainer,
                llTiendaProductsGridContainer = llTiendaProductsGridContainer,
                tvTiendaEmptyState = tvTiendaEmptyState,
                llServiciosListContainer = llServiciosListContainer,
                tvServiciosEmptyState = tvServiciosEmptyState
            )
            cargarMembresiaDesdeBackend(view)
            cargarContadorNotificaciones(view)
            cargarResumenComprasInicio(view)
            if (isQrVisible) {
                cargarMiQrDesdeBackend(view, regenerar = false)
            }
        }

        etSearchMockup?.doAfterTextChanged { editable ->
            textoBusqueda = editable?.toString()?.trim().orEmpty()
            renderizarProductosPopularesInicio(llHomePopularProductsContainer)
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

        var cerrarQrSiVisible: (() -> Unit)? = null
        val qrDispatcherCallback = object : OnBackPressedCallback(isQrVisible) {
            override fun handleOnBackPressed() {
                cerrarQrSiVisible?.invoke()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, qrDispatcherCallback)

        fun mostrarSeccionConFade(seccion: View?, animar: Boolean) {
            if (seccion == null) return
            seccion.animate().cancel()
            if (animar) {
                seccion.alpha = 0.88f
                seccion.translationY = dpToPx(5).toFloat()
                seccion.visibility = View.VISIBLE
                seccion.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(140L)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            } else {
                seccion.alpha = 1f
                seccion.translationY = 0f
                seccion.visibility = View.VISIBLE
            }
        }

        fun showSectionForTab(
            tabIndex: Int,
            animar: Boolean = true,
            limpiarBusqueda: Boolean = false
        ) {
            isQrVisible = false
            qrDispatcherCallback.isEnabled = false

            if (limpiarBusqueda && textoBusqueda.isNotEmpty()) {
                etSearchMockup?.setText("")
            }

            if (animar) {
                scrollHome.scrollTo(0, 0)
            }

            llQrAccessSection.visibility = View.GONE
            llTopHeaderAndSearch.visibility = View.VISIBLE

            when (tabIndex) {
                1 -> {
                    llHomeSection.visibility = View.GONE
                    llTiendaSection.visibility = View.GONE
                    llServiciosSection.visibility = View.GONE
                    mostrarSeccionConFade(llMembresiaSection, animar)
                    llSearchBarRow?.visibility = View.GONE
                }
                2 -> {
                    llHomeSection.visibility = View.GONE
                    llMembresiaSection?.visibility = View.GONE
                    llServiciosSection.visibility = View.GONE
                    mostrarSeccionConFade(llTiendaSection, animar)
                    llSearchBarRow?.visibility = View.VISIBLE
                    etSearchMockup.setHint(R.string.tienda_search_hint)
                }
                3 -> {
                    llHomeSection.visibility = View.GONE
                    llMembresiaSection?.visibility = View.GONE
                    llTiendaSection.visibility = View.GONE
                    mostrarSeccionConFade(llServiciosSection, animar)
                    llSearchBarRow?.visibility = View.VISIBLE
                    etSearchMockup.setHint(R.string.servicios_search_hint)
                }
                else -> {
                    llMembresiaSection?.visibility = View.GONE
                    llTiendaSection.visibility = View.GONE
                    llServiciosSection.visibility = View.GONE
                    mostrarSeccionConFade(llHomeSection, animar)
                    llSearchBarRow?.visibility = View.GONE
                }
            }
        }

        cerrarQrSiVisible = { showSectionForTab(selectedTabIndex) }

        aplicarEstilosBottomNav(selectedTabIndex, tabs)
        showSectionForTab(selectedTabIndex, animar = false)

        fun actualizarPildoraSinAnimacion() {
            if (!isAdded || view == null || pillAnimating) return
            val activeTab = tabs[selectedTabIndex].container
            if (activeTab.width > 0) {
                viewActiveNavPill.animate().cancel()
                viewActiveNavPill.pivotX = 0f
                viewActiveNavPill.scaleX = 1f
                viewActiveNavPill.translationX = activeTab.left.toFloat()
                val pillParams = viewActiveNavPill.layoutParams
                if (pillParams.width != activeTab.width) {
                    pillParams.width = activeTab.width
                    viewActiveNavPill.layoutParams = pillParams
                }
            }
        }

        llNavTabsRow.post { actualizarPildoraSinAnimacion() }
        llNavTabsRow.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (!pillAnimating) {
                actualizarPildoraSinAnimacion()
            }
        }

        tabs.forEachIndexed { index, tabItem ->
            tabItem.container.setOnClickListener {
                val mismoTab = index == selectedTabIndex
                if (mismoTab && !isQrVisible) {
                    scrollHome.smoothScrollTo(0, 0)
                    return@setOnClickListener
                }
                if (isQrVisible && mismoTab) {
                    showSectionForTab(index)
                } else {
                    animateBottomNavSelection(
                        targetIndex = index,
                        tabs = tabs,
                        llNavTabsRow = llNavTabsRow,
                        viewActiveNavPill = viewActiveNavPill
                    )
                    showSectionForTab(index, limpiarBusqueda = !mismoTab)
                }

                if (index == 1) {
                    if (!membresiaCargada || miMembresiaData == null || errorCargaMembresia) {
                        cargarMembresiaDesdeBackend(view)
                    }
                } else if (!catalogoCargado || (listaProductos.isEmpty() && listaServicios.isEmpty())) {
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
            val mismoTab = selectedTabIndex == 1
            animateBottomNavSelection(
                targetIndex = 1,
                tabs = tabs,
                llNavTabsRow = llNavTabsRow,
                viewActiveNavPill = viewActiveNavPill
            )
            showSectionForTab(1, limpiarBusqueda = !mismoTab)
            if (!membresiaCargada || miMembresiaData == null || errorCargaMembresia) {
                cargarMembresiaDesdeBackend(view)
            }
        }
        val openHistorialComprasListener = View.OnClickListener {
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
        view.findViewById<View>(R.id.cardPagos)?.setOnClickListener(openHistorialComprasListener)
        view.findViewById<View>(R.id.btnPagarMockup)?.setOnClickListener(openHistorialComprasListener)

        val goToProductosListener = View.OnClickListener {
            val mismoTab = selectedTabIndex == 2
            animateBottomNavSelection(
                targetIndex = 2,
                tabs = tabs,
                llNavTabsRow = llNavTabsRow,
                viewActiveNavPill = viewActiveNavPill
            )
            showSectionForTab(2, limpiarBusqueda = !mismoTab)
            if (!catalogoCargado || listaProductos.isEmpty()) {
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
        val goToServiciosListener = View.OnClickListener {
            val mismoTab = selectedTabIndex == 3
            animateBottomNavSelection(
                targetIndex = 3,
                tabs = tabs,
                llNavTabsRow = llNavTabsRow,
                viewActiveNavPill = viewActiveNavPill
            )
            showSectionForTab(3, limpiarBusqueda = !mismoTab)
            if (!catalogoCargado || listaServicios.isEmpty()) {
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
        view.findViewById<View>(R.id.cardTienda)?.setOnClickListener(goToServiciosListener)
        view.findViewById<View>(R.id.btnComprarMockup)?.setOnClickListener(goToServiciosListener)
        view.findViewById<View>(R.id.btnVerCatalogoMockup)?.setOnClickListener(goToProductosListener)

        val flQrCodeBox = view.findViewById<View>(R.id.flQrCodeBox)
        val btnGenerarNuevoQr = view.findViewById<View>(R.id.btnGenerarNuevoQr)
        val btnReportarProblemaQr = view.findViewById<View>(R.id.btnReportarProblemaQr)
        btnReportarProblemaQr?.visibility = View.GONE

        val openQrAccessListener = View.OnClickListener {
            val transition = AutoTransition().apply {
                duration = 240L
                interpolator = FastOutSlowInInterpolator()
            }
            TransitionManager.beginDelayedTransition(llMainContentRoot, transition)

            isQrVisible = true
            qrDispatcherCallback.isEnabled = true
            llTopHeaderAndSearch.visibility = View.GONE
            llHomeSection.visibility = View.GONE
            llMembresiaSection?.visibility = View.GONE
            llTiendaSection.visibility = View.GONE
            llServiciosSection.visibility = View.GONE
            llQrAccessSection.visibility = View.VISIBLE

            scrollHome.smoothScrollTo(0, 0)
            cargarMiQrDesdeBackend(view, regenerar = false)
        }

        view.findViewById<View>(R.id.fabQrMockup)?.setOnClickListener(openQrAccessListener)
        view.findViewById<View>(R.id.btnMembresiaAbrirQr)?.setOnClickListener(openQrAccessListener)

        val openMembresiaDetailListener = View.OnClickListener {
            val existing = childFragmentManager.findFragmentByTag(MembresiaDetailBottomSheetFragment.TAG)
            if (existing == null) {
                MembresiaDetailBottomSheetFragment.newInstance(miMembresiaData)
                    .show(childFragmentManager, MembresiaDetailBottomSheetFragment.TAG)
            }
        }
        view.findViewById<View>(R.id.cardHeroMembership)?.setOnClickListener(openMembresiaDetailListener)
        view.findViewById<View>(R.id.btnHeroArrow)?.setOnClickListener(openMembresiaDetailListener)

        childFragmentManager.setFragmentResultListener(
            MembresiaDetailBottomSheetFragment.RESULT_KEY_ACCION_MEMBRESIA,
            viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(MembresiaDetailBottomSheetFragment.EXTRA_ACCION)) {
                MembresiaDetailBottomSheetFragment.ACCION_ABRIR_QR -> {
                    openQrAccessListener.onClick(view)
                }
                MembresiaDetailBottomSheetFragment.ACCION_VER_PLANES -> {
                    goToMembresiaListener.onClick(view)
                }
            }
        }

        childFragmentManager.setFragmentResultListener(
            MembresiaDetailBottomSheetFragment.RESULT_KEY_DATOS_ACTUALIZADOS,
            viewLifecycleOwner
        ) { _, bundle ->
            val json = bundle.getString(MembresiaDetailBottomSheetFragment.EXTRA_MEMBRESIA_JSON)
            if (!json.isNullOrBlank()) {
                val actualizado = try {
                    com.google.gson.Gson().fromJson(json, MiMembresiaResponseDto::class.java)
                } catch (_: Exception) {
                    null
                }
                if (actualizado != null) {
                    miMembresiaData = actualizado
                    cargandoMembresia = false
                    errorCargaMembresia = false
                    renderizarSeccionMembresia(view)
                }
            }
        }

        val closeQrListener = View.OnClickListener {
            showSectionForTab(selectedTabIndex)
        }
        view.findViewById<View>(R.id.btnQrBack)?.setOnClickListener(closeQrListener)

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
            cargarMiQrDesdeBackend(view, regenerar = true)
        }

        cargarCatalogoDesdeBackend(
            llHomePopularProductsContainer = llHomePopularProductsContainer,
            llTiendaCategoryChipsContainer = llTiendaCategoryChipsContainer,
            llTiendaProductsGridContainer = llTiendaProductsGridContainer,
            tvTiendaEmptyState = tvTiendaEmptyState,
            llServiciosListContainer = llServiciosListContainer,
            tvServiciosEmptyState = tvServiciosEmptyState
        )
        renderizarHeroMembresia(view)
        cargarMembresiaDesdeBackend(view)
        cargarResumenComprasInicio(view)
    }

    private fun cargarCatalogoDesdeBackend(
        llHomePopularProductsContainer: LinearLayout?,
        llTiendaCategoryChipsContainer: LinearLayout?,
        llTiendaProductsGridContainer: LinearLayout?,
        tvTiendaEmptyState: TextView?,
        llServiciosListContainer: LinearLayout?,
        tvServiciosEmptyState: TextView?
    ) {
        if (listaProductos.isNotEmpty()) {
            construirChipsCategoriasReales(
                llTiendaCategoryChipsContainer,
                llTiendaProductsGridContainer,
                tvTiendaEmptyState
            )
            renderizarProductosPopularesInicio(llHomePopularProductsContainer)
            renderizarGridProductos(llTiendaProductsGridContainer, tvTiendaEmptyState)
        } else if (!catalogoCargado) {
            tvTiendaEmptyState?.visibility = View.GONE
            SkeletonViewHelper.mostrarSkeletonProductosPopular(llHomePopularProductsContainer)
            SkeletonViewHelper.mostrarSkeletonProductosGrid(llTiendaProductsGridContainer)
        }
        if (listaServicios.isNotEmpty()) {
            renderizarListaServicios(llServiciosListContainer, tvServiciosEmptyState)
        } else if (!catalogoCargado) {
            tvServiciosEmptyState?.visibility = View.GONE
            SkeletonViewHelper.mostrarSkeletonServiciosLista(llServiciosListContainer)
        }

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

            catalogoCargado = true
            if (!isAdded) return@launch
            view?.let { root ->
                val tvServSub = root.findViewById<TextView>(R.id.tvHomeServiciosSubtitulo)
                if (listaServicios.isNotEmpty()) {
                    tvServSub?.text = "${listaServicios.size} disponibles\nen sucursal"
                }
                root.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
                    ?.isRefreshing = false
            }
            renderizarListaServicios(llServiciosListContainer, tvServiciosEmptyState)
        }
    }

    private fun cargarResumenComprasInicio(rootView: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.empleadoService.obtenerVentas(limite = 20)
                if (!isAdded || this@HomeFragment.view == null) return@launch
                if (res.isSuccessful && res.body()?.responseFlag == 0) {
                    val data = res.body()?.data
                    val registros = data?.registros.orEmpty()
                    val total = data?.total ?: registros.size
                    val pendientesMp = registros.count {
                        it.estado.equals("pendiente_pago", ignoreCase = true)
                    }
                    val tvLabel = rootView.findViewById<TextView>(R.id.tvHomePagosLabel)
                    val tvValor = rootView.findViewById<TextView>(R.id.tvHomePagosValor)
                    if (pendientesMp > 0) {
                        tvLabel?.text = "Pagos pendientes"
                        tvValor?.text = "$pendientesMp por pagar"
                    } else {
                        tvLabel?.text = "Compras realizadas"
                        tvValor?.text = if (total == 1) "1 compra" else "$total compras"
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun cargarContadorNotificaciones(rootView: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = NotificacionRepository().contarNoLeidas()
            if (!isAdded || resultado !is ResultadoNotificaciones.Exito) return@launch

            val badge = rootView.findViewById<TextView>(R.id.tvNotificationsBadge)
            val noLeidas = resultado.datos
            badge?.text = if (noLeidas > 99) "99+" else noLeidas.toString()
            badge?.visibility = if (noLeidas > 0) View.VISIBLE else View.GONE
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

        val productosDestacados = listaProductos
            .sortedBy { it.stockActual <= 0 }
            .take(4)

        if (productosDestacados.isEmpty()) {
            val tvVacio = TextView(requireContext()).apply {
                text = "No hay productos disponibles por el momento"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                gravity = Gravity.CENTER
                setPadding(dpToPx(4), dpToPx(16), dpToPx(4), dpToPx(16))
            }
            container.addView(tvVacio)
            return
        }

        productosDestacados.chunked(2).forEachIndexed { filaIndex, par ->
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
                isBaselineAligned = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (filaIndex > 0) topMargin = dpToPx(14)
                }
            }
            fila.addView(
                crearTarjetaProductoTiendaGrid(
                    producto = par[0],
                    esColumnaIzquierda = true
                )
            )
            if (par.size > 1) {
                fila.addView(
                    crearTarjetaProductoTiendaGrid(
                        producto = par[1],
                        esColumnaIzquierda = false
                    )
                )
            } else {
                fila.addView(
                    Space(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        ).apply { marginStart = dpToPx(7) }
                    }
                )
            }
            container.addView(fila)
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
                prod.nombre.contains(textoBusqueda, ignoreCase = true)

            coincideCategoria && coincideBusqueda
        }.sortedBy { it.stockActual <= 0 }

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
                serv.nombre.contains(textoBusqueda, ignoreCase = true)
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

    private fun crearTarjetaProductoTiendaGrid(
        producto: ProductoClienteDto,
        esColumnaIzquierda: Boolean
    ): MaterialCardView {
        val context = requireContext()
        val agotado = producto.stockActual <= 0
        val precioNeto = CalculoVentaPos.precioConDescuento(producto.precioVenta, producto.descuentoPorcentaje)

        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(232), 1f).apply {
                if (esColumnaIzquierda) {
                    marginEnd = dpToPx(7)
                } else {
                    marginStart = dpToPx(7)
                }
            }
            setCardBackgroundColor(
                if (agotado) Color.parseColor("#6B7280")
                else ContextCompat.getColor(context, R.color.apex_card_dark)
            )
            alpha = if (agotado) 0.78f else 1f
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
                colorFilter = if (agotado) {
                    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                } else {
                    null
                }
            }
            addView(icon)
            if (agotado) {
                addView(
                    TextView(context).apply {
                        text = "AGOTADO"
                        setTextColor(Color.WHITE)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                        setTypeface(typeface, Typeface.BOLD)
                        setPadding(dpToPx(7), dpToPx(2), dpToPx(7), dpToPx(2))
                        setBackgroundColor(Color.parseColor("#CC334155"))
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP or Gravity.START
                        ).apply {
                            setMargins(dpToPx(6), dpToPx(6), 0, 0)
                        }
                    }
                )
            }
        }
        cargarImagenEnContenedor(producto.imagenUrl, imgBox, desaturar = agotado)
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
            } else {
                append(" • Agotado")
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
            text = "${formatearPrecio(precioNeto)} MXN"
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (agotado) R.color.apex_text_on_dark_secondary else R.color.apex_orange
                )
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        bottomRow.addView(tvPrecio)

        val btnAdd = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            setBackgroundResource(if (agotado) R.drawable.bg_chip_unselected else R.drawable.bg_circle_orange)
            val plusIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(16), dpToPx(16), Gravity.CENTER)
                setImageResource(R.drawable.ic_plus)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            if (agotado) R.color.apex_nav_inactive else R.color.white
                        )
                    )
                )
            }
            addView(plusIcon)
            setOnClickListener {
                if (agotado) {
                    Toast.makeText(context, "${producto.nombre} está agotado temporalmente.", Toast.LENGTH_SHORT).show()
                } else {
                    val res = CartManager.agregarProducto(context, producto, 1)
                    Toast.makeText(context, res.mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }
        bottomRow.addView(btnAdd)

        inner.addView(bottomRow)
        card.addView(inner)

        card.setOnClickListener {
            if (agotado) {
                Toast.makeText(context, "${producto.nombre} está agotado temporalmente.", Toast.LENGTH_SHORT).show()
            } else {
                abrirDetalleProducto(producto)
            }
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
        val llMembresiaPlanesContainer = rootView.findViewById<LinearLayout>(R.id.llMembresiaPlanesContainer)
        if (listaPlanesMembresia.isNotEmpty() || miMembresiaData != null) {
            renderizarSeccionMembresia(rootView)
        } else if (!membresiaCargada) {
            tvMembresiaPlanesEmpty?.visibility = View.GONE
            SkeletonViewHelper.mostrarSkeletonMembresiaPlanes(llMembresiaPlanesContainer)
        }

        cargandoMembresia = true
        errorCargaMembresia = false
        if (miMembresiaData == null) {
            renderizarHeroMembresia(rootView)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            var exitoMembresia = false
            try {
                val resMiMembresia = RetrofitClient.authService.obtenerMiMembresia()
                if (resMiMembresia.isSuccessful && resMiMembresia.body()?.responseFlag == 0) {
                    miMembresiaData = resMiMembresia.body()?.data
                    exitoMembresia = true
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

            cargandoMembresia = false
            membresiaCargada = true
            errorCargaMembresia = !exitoMembresia

            if (!isAdded) return@launch
            renderizarSeccionMembresia(rootView)
        }
    }

    private fun renderizarHeroMembresia(rootView: View) {
        val tvPlanBadge = rootView.findViewById<TextView>(R.id.tvPlanBadge)
        val tvHeroSubtitle = rootView.findViewById<TextView>(R.id.tvHeroSubtitle)

        val resumen = MembresiaVigenciaHelper.construirResumenHero(
            data = miMembresiaData,
            cargando = cargandoMembresia,
            errorCarga = errorCargaMembresia
        )

        tvPlanBadge?.text = resumen.badgeTexto
        tvHeroSubtitle?.text = resumen.subtituloTexto
    }

    private fun renderizarSeccionMembresia(rootView: View) {
        renderizarHeroMembresia(rootView)

        val tvMembresiaEstadoBadge = rootView.findViewById<TextView>(R.id.tvMembresiaEstadoBadge)
        val tvMembresiaRenovacionBadge = rootView.findViewById<TextView>(R.id.tvMembresiaRenovacionBadge)
        val tvMembresiaPlanActualNombre = rootView.findViewById<TextView>(R.id.tvMembresiaPlanActualNombre)
        val tvMembresiaDiasRestantesNumero = rootView.findViewById<TextView>(R.id.tvMembresiaDiasRestantesNumero)
        val tvMembresiaDiasRestantesTexto = rootView.findViewById<TextView>(R.id.tvMembresiaDiasRestantesTexto)
        val tvMembresiaFechaInicio = rootView.findViewById<TextView>(R.id.tvMembresiaFechaInicio)
        val tvMembresiaFechaFin = rootView.findViewById<TextView>(R.id.tvMembresiaFechaFin)
        val tvMembresiaPlanesEmpty = rootView.findViewById<TextView>(R.id.tvMembresiaPlanesEmpty)
        val llMembresiaPlanesContainer = rootView.findViewById<LinearLayout>(R.id.llMembresiaPlanesContainer)

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

        val estaEnCarrito = CartManager.obtenerMembresiaActual(context)?.id == plan.id

        val btnElegir = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(36)
            )
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            setBackgroundResource(
                if (estaEnCarrito || esPlanActual) R.drawable.bg_badge_vip_outline else R.drawable.bg_badge_orange
            )
            text = when {
                estaEnCarrito -> "En tu carrito"
                esPlanActual -> "Renovar"
                else -> "Solicitar plan"
            }
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        bottomRow.addView(btnElegir)

        container.addView(bottomRow)
        card.addView(container)

        val accionAgregarMembresia = View.OnClickListener {
            val resultado = CartManager.agregarMembresia(context, plan)
            Toast.makeText(context, resultado.mensaje, Toast.LENGTH_SHORT).show()
            if (resultado.exito) {
                val fm = try { requireActivity().supportFragmentManager } catch (_: Exception) { parentFragmentManager }
                val existing = fm.findFragmentByTag(CartBottomSheetFragment.TAG)
                if (existing == null) {
                    CartBottomSheetFragment().show(
                        fm,
                        CartBottomSheetFragment.TAG
                    )
                }
            }
        }

        btnElegir.setOnClickListener(accionAgregarMembresia)
        card.setOnClickListener(accionAgregarMembresia)

        return card
    }

    private fun calcularDiasRestantes(fechaFinIso: String?): Long? {
        return MembresiaVigenciaHelper.calcularDiasRestantes(fechaFinIso)
    }

    private fun formatearFechaCorta(fechaIso: String?): String {
        return MembresiaVigenciaHelper.formatearFechaCorta(fechaIso)
    }

    private fun formatearPrecio(monto: Double): String {
        val formato = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (monto % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$${formato.format(monto)}"
    }

    private fun cargarImagenEnContenedor(
        rutaImagen: String?,
        contenedor: FrameLayout,
        desaturar: Boolean = false
    ) {
        val urlCompleta = RetrofitClient.resolverUrlImagen(rutaImagen) ?: return

        val cached = bitmapCache.get(urlCompleta)
        if (cached != null) {
            mostrarBitmapEnContenedor(cached, contenedor, desaturar)
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
                        mostrarBitmapEnContenedor(bitmap, contenedor, desaturar)
                    }
                }
            }
        }
    }

    private fun mostrarBitmapEnContenedor(
        bitmap: Bitmap,
        contenedor: FrameLayout,
        desaturar: Boolean = false
    ) {
        val context = contenedor.context
        val badges = (0 until contenedor.childCount)
            .map { contenedor.getChildAt(it) }
            .filter { it is TextView }
        contenedor.removeAllViews()
        val imageView = ImageView(context).apply {
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
        contenedor.addView(imageView)
        badges.forEach { contenedor.addView(it) }
    }

    private fun animateBottomNavSelection(
        targetIndex: Int,
        tabs: List<NavTabItem>,
        llNavTabsRow: LinearLayout,
        viewActiveNavPill: View
    ) {
        if (targetIndex == selectedTabIndex) return
        selectedTabIndex = targetIndex

        val startX = viewActiveNavPill.translationX
        val startWidth = viewActiveNavPill.width.coerceAtLeast(1)
        val targetContainer = tabs[targetIndex].container
        pillAnimating = true

        llNavTabsRow.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                llNavTabsRow.viewTreeObserver.removeOnPreDrawListener(this)

                val endX = targetContainer.left.toFloat()
                val endWidth = targetContainer.width.coerceAtLeast(1)

                val params = viewActiveNavPill.layoutParams
                if (params.width != endWidth) {
                    params.width = endWidth
                    viewActiveNavPill.layoutParams = params
                }

                viewActiveNavPill.animate().cancel()
                viewActiveNavPill.pivotX = 0f
                viewActiveNavPill.translationX = startX
                viewActiveNavPill.scaleX = startWidth.toFloat() / endWidth.toFloat()
                viewActiveNavPill.animate()
                    .translationX(endX)
                    .scaleX(1f)
                    .setDuration(260L)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .withEndAction { pillAnimating = false }
                    .start()
                return true
            }
        })

        val transition = ChangeBounds().apply {
            duration = 260L
            interpolator = FastOutSlowInInterpolator()
        }
        TransitionManager.beginDelayedTransition(llNavTabsRow, transition)

        aplicarEstilosBottomNav(targetIndex, tabs)
    }

    private fun aplicarEstilosBottomNav(targetIndex: Int, tabs: List<NavTabItem>) {
        val context = requireContext()
        val colorActive = ContextCompat.getColor(context, R.color.apex_text_primary)
        val colorInactive = ContextCompat.getColor(context, R.color.apex_nav_inactive)

        tabs.forEachIndexed { index, item ->
            val isSelected = index == targetIndex

            if (isSelected) {
                item.container.orientation = LinearLayout.HORIZONTAL
                item.container.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(48),
                    0f
                )
                item.container.setPadding(dpToPx(14), 0, dpToPx(14), 0)

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

    private fun cargarMiQrDesdeBackend(rootView: View, regenerar: Boolean) {
        val ivQr = rootView.findViewById<ImageView>(R.id.ivQrCodeMatrix)
        val tvBadge = rootView.findViewWithTag<TextView>("tv_qr_badge")
        val tvNombre = rootView.findViewWithTag<TextView>("tv_qr_member_name")
        val tvCodigo = rootView.findViewWithTag<TextView>("tv_qr_member_code")
        val btnRegenerar = rootView.findViewById<View>(R.id.btnGenerarNuevoQr)

        tvBadge?.text = if (regenerar) "REGENERANDO QR…" else "CARGANDO PASE QR…"
        btnRegenerar?.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = if (regenerar) {
                    RetrofitClient.authService.regenerarMiQr()
                } else {
                    RetrofitClient.authService.obtenerMiQr()
                }
                val body = response.body()
                val data = if (response.isSuccessful && body?.responseFlag == 0) body.data else null
                if (!isAdded) return@launch

                if (data == null || data.qrPayload.isNullOrBlank()) {
                    tvBadge?.text = "NO DISPONIBLE"
                    tvCodigo?.text = body?.message ?: "No se pudo obtener tu código QR"
                    return@launch
                }

                val nombreCompleto = data.cliente?.nombreCompleto
                    ?.takeIf { it.isNotBlank() }
                    ?: com.sprm.apexgym.core.session.SessionManager.obtenerNombre(requireContext())
                    ?: "Socio ApexGym"

                val planNombre = data.membresia?.tipoMembresia?.nombre?.uppercase(Locale.getDefault())
                tvBadge?.text = if (data.vigente) {
                    if (!planNombre.isNullOrBlank()) "MEMBRESÍA VIGENTE • $planNombre" else "MEMBRESÍA VIGENTE"
                } else {
                    "SIN MEMBRESÍA VIGENTE"
                }
                tvNombre?.text = nombreCompleto
                val diasQr = MembresiaVigenciaHelper
                    .calcularDiasRestantes(data.membresia?.fechaFin)
                    ?.coerceAtLeast(0L)
                    ?: data.diasRestantes.toLong().coerceAtLeast(0L)
                val textoDiasQr = MembresiaVigenciaHelper.formatearTextoDiasRestantes(diasQr)
                tvCodigo?.text = if (data.vigente) {
                    "Código: ${data.codigoQr ?: "—"} • $textoDiasQr"
                } else {
                    data.mensaje ?: "Membresía inactiva"
                }

                val bitmap = withContext(Dispatchers.Default) {
                    generarBitmapQr(data.qrPayload, 600)
                }
                if (isAdded && bitmap != null) {
                    ivQr?.setImageBitmap(bitmap)
                    if (regenerar) {
                        Toast.makeText(
                            requireContext(),
                            "Código QR actualizado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                if (isAdded) {
                    tvBadge?.text = "SIN CONEXIÓN"
                    tvCodigo?.text = "No se pudo cargar tu pase QR"
                }
            } finally {
                if (isAdded) {
                    btnRegenerar?.isEnabled = true
                }
            }
        }
    }

    private fun generarBitmapQr(contenido: String, tamano: Int): Bitmap? {
        return try {
            val hints = mapOf(
                com.google.zxing.EncodeHintType.MARGIN to 1,
                com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                contenido,
                com.google.zxing.BarcodeFormat.QR_CODE,
                tamano,
                tamano,
                hints
            )
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            val negro = android.graphics.Color.parseColor("#121214")
            val blanco = android.graphics.Color.WHITE
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix.get(x, y)) negro else blanco
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onStart() {
        super.onStart()
        if (!notificacionReceiverRegistrado) {
            ContextCompat.registerReceiver(
                requireContext(),
                notificacionReceiver,
                IntentFilter(ApexGymMessagingService.ACTION_NOTIFICACION_RECIBIDA),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            notificacionReceiverRegistrado = true
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { cargarContadorNotificaciones(it) }
    }

    override fun onStop() {
        if (notificacionReceiverRegistrado) {
            try {
                requireContext().unregisterReceiver(notificacionReceiver)
            } catch (_: IllegalArgumentException) {
            }
            notificacionReceiverRegistrado = false
        }
        super.onStop()
    }

    override fun onDestroyView() {
        cartListener?.let { CartManager.removeListener(it) }
        cartListener = null
        view?.findViewById<View>(R.id.viewActiveNavPill)?.animate()?.cancel()
        pillAnimating = false
        super.onDestroyView()
    }
}

package com.sprm.apexgym.feature.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.NotificacionDto
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.repository.NotificacionRepository
import com.sprm.apexgym.core.network.repository.ResultadoNotificaciones
import com.sprm.apexgym.core.notifications.ApexGymMessagingService
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentNotificacionesListBinding
import kotlinx.coroutines.launch

class NotificacionesListFragment : BaseFragment<FragmentNotificacionesListBinding>() {

    private data class TabFiltro(
        val titulo: String,
        val estado: String? = null,
        val tipo: String? = null
    )

    private val repositorio = NotificacionRepository()

    private var notificaciones: List<NotificacionDto> = emptyList()
    private var estadoSeleccionado: String? = null
    private var tipoSeleccionado: String? = null
    private var textoBusqueda: String = ""
    private var cargando: Boolean = false
    private var recargaPendiente: Boolean = false
    private var primerResume: Boolean = true
    private var receiverRegistrado: Boolean = false

    private val notificacionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isAdded || view == null) return
            cargarNotificaciones(silencioso = true)
        }
    }

    override fun getLayoutRes(): Int = R.layout.fragment_notificaciones_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        // La API todavía no expone preferencias de notificaciones: no se muestra un acceso sin función.
        binding.flConfig.visibility = View.INVISIBLE
        binding.vpNotificacionesTabs.visibility = View.GONE
        binding.cvUnreadBadge.visibility = View.GONE

        configurarTabs()
        configurarBusqueda()
        binding.btnMarcarTodasLeidas.setOnClickListener { marcarTodasLeidas() }

        primerResume = true
        cargarNotificaciones()
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistrado) {
            ContextCompat.registerReceiver(
                requireContext(),
                notificacionReceiver,
                IntentFilter(ApexGymMessagingService.ACTION_NOTIFICACION_RECIBIDA),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistrado = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (primerResume) {
            primerResume = false
        } else if (view != null) {
            cargarNotificaciones(silencioso = true)
        }
    }

    override fun onStop() {
        if (receiverRegistrado) {
            try {
                requireContext().unregisterReceiver(notificacionReceiver)
            } catch (_: IllegalArgumentException) {
            }
            receiverRegistrado = false
        }
        super.onStop()
    }

    private fun configurarTabs() {
        val tabs = buildList {
            add(TabFiltro("Todas"))
            add(TabFiltro("Pendientes", estado = NotificacionDto.ESTADO_PENDIENTE))
            add(TabFiltro("Leídas", estado = NotificacionDto.ESTADO_LEIDA))
            add(TabFiltro("Ignoradas", estado = NotificacionDto.ESTADO_IGNORADA))
            if (SessionManager.esEmpleado(requireContext())) {
                add(TabFiltro("Actividad", tipo = NotificacionDto.TIPO_ACTIVIDAD_EMPLEADO))
            }
        }

        binding.tabLayoutNotificaciones.removeAllTabs()
        tabs.forEach { filtro ->
            binding.tabLayoutNotificaciones.addTab(
                binding.tabLayoutNotificaciones.newTab()
                    .setText(filtro.titulo)
                    .setTag(filtro)
            )
        }

        binding.tabLayoutNotificaciones.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val filtro = tab.tag as? TabFiltro
                    estadoSeleccionado = filtro?.estado
                    tipoSeleccionado = filtro?.tipo
                    renderizar()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )
        binding.tabLayoutNotificaciones.getTabAt(0)?.select()
    }

    private fun configurarBusqueda() {
        binding.etSearchNotificaciones.isEnabled = true
        binding.etSearchNotificaciones.doAfterTextChanged { editable ->
            textoBusqueda = editable?.toString().orEmpty()
            renderizar()
        }
    }

    private fun cargarNotificaciones(silencioso: Boolean = false) {
        if (cargando) {
            recargaPendiente = true
            return
        }
        cargando = true
        if (!silencioso || notificaciones.isEmpty()) {
            mostrarEstado(
                titulo = "Cargando notificaciones…",
                mensaje = "Consultando tus notificaciones.",
                accionTexto = null,
                accion = null
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repositorio.listar()
            cargando = false
            if (!isAdded || view == null) return@launch

            when (resultado) {
                is ResultadoNotificaciones.Exito -> {
                    notificaciones = resultado.datos
                    actualizarContador(notificaciones.count { it.estaPendiente })
                    renderizar()
                }

                is ResultadoNotificaciones.Error -> {
                    if (!silencioso || notificaciones.isEmpty()) {
                        notificaciones = emptyList()
                        actualizarContador(0)
                        mostrarEstado(
                            titulo = "No se pudieron cargar las notificaciones",
                            mensaje = resultado.mensaje,
                            accionTexto = "Reintentar",
                            accion = { cargarNotificaciones() }
                        )
                    }
                }
            }

            if (recargaPendiente) {
                recargaPendiente = false
                cargarNotificaciones(silencioso = true)
            }
        }
    }

    private fun renderizar() {
        if (!isAdded) return

        val filtradas = filtrarNotificaciones(
            notificaciones = notificaciones,
            estado = estadoSeleccionado,
            texto = textoBusqueda,
            tipo = tipoSeleccionado
        )
        binding.llNotificacionesContainer.removeAllViews()

        if (filtradas.isEmpty()) {
            val hayNotificaciones = notificaciones.isNotEmpty()
            mostrarEstado(
                titulo = if (hayNotificaciones) "Sin resultados" else "No tienes notificaciones",
                mensaje = if (hayNotificaciones) {
                    "No hay notificaciones que coincidan con el filtro o la búsqueda."
                } else {
                    "Cuando recibas un aviso aparecerá aquí."
                },
                accionTexto = null,
                accion = null
            )
            return
        }

        filtradas.forEach { notificacion ->
            binding.llNotificacionesContainer.addView(crearTarjeta(notificacion))
        }
    }

    private fun actualizarContador(noLeidas: Int) {
        binding.cvUnreadBadge.visibility = if (noLeidas > 0) View.VISIBLE else View.GONE
        binding.tvUnreadCount.text = when (noLeidas) {
            1 -> "Tienes 1 notificación sin leer"
            else -> "Tienes $noLeidas notificaciones sin leer"
        }
        binding.btnMarcarTodasLeidas.visibility = if (noLeidas > 0) View.VISIBLE else View.GONE
    }

    private fun crearTarjeta(notificacion: NotificacionDto): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                navegar(NotificacionDetalleFragment.newInstance(notificacion.id))
            }
        }

        val contenido = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(14))
        }

        val filaSuperior = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        filaSuperior.addView(
            FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                setBackgroundResource(R.drawable.bg_circle_orange_light)
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
                        setImageResource(R.drawable.ic_notification_bell)
                        setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                    }
                )
            }
        )

        val columnaTexto = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(12) }
        }

        columnaTexto.addView(
            TextView(context).apply {
                text = notificacion.titulo.ifBlank { "Notificación" }
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
        )

        val filaEtiquetas = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(5) }
        }

        filaEtiquetas.addView(
            TextView(context).apply {
                val norm = notificacion.tipo.trim().uppercase()
                text = when (norm) {
                    "ACCESO_QR" -> "Acceso QR"
                    "ACTIVIDAD_EMPLEADO" -> "Actividad"
                    "MEMBRESIA" -> "Membresía"
                    "PAGO", "VENTA" -> "Pagos"
                    "GENERAL", "" -> "General"
                    else -> norm.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                }
                setBackgroundResource(R.drawable.bg_chip_orange)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        val (estadoTexto, estadoFondo) = when (notificacion.estado) {
            NotificacionDto.ESTADO_LEIDA -> "Leída" to R.drawable.bg_badge_green
            NotificacionDto.ESTADO_IGNORADA -> "Ignorada" to R.drawable.bg_badge_blue
            else -> "Pendiente" to R.drawable.bg_badge_orange
        }

        filaEtiquetas.addView(
            TextView(context).apply {
                text = estadoTexto
                setBackgroundResource(estadoFondo)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(6) }
            }
        )

        columnaTexto.addView(filaEtiquetas)
        filaSuperior.addView(columnaTexto)

        filaSuperior.addView(
            TextView(context).apply {
                text = formatearFecha(
                    notificacion.createdAt,
                    "dd/MM/yy HH:mm"
                )
                setTextColor(ContextCompat.getColor(context, R.color.apex_nav_inactive))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            }
        )

        contenido.addView(filaSuperior)

        contenido.addView(
            TextView(context).apply {
                text = notificacion.mensaje
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setLineSpacing(0f, 1.1f)
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            }
        )

        if (notificacion.estaPendiente) {
            val acciones = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
            }

            acciones.addView(
                crearAccion("Ignorar", ContextCompat.getColor(context, R.color.apex_text_secondary)) {
                    ejecutarAccion(
                        id = notificacion.id,
                        exito = "Notificación ignorada",
                        accion = { repositorio.marcarIgnorada(notificacion.id) }
                    )
                }
            )
            acciones.addView(
                crearAccion("Marcar leída", ContextCompat.getColor(context, R.color.apex_orange)) {
                    ejecutarAccion(
                        id = notificacion.id,
                        exito = "Notificación marcada como leída",
                        accion = { repositorio.marcarLeida(notificacion.id) }
                    )
                }
            )
            contenido.addView(acciones)
        }

        card.addView(contenido)
        return card
    }

    private fun crearAccion(texto: String, color: Int, alTocar: () -> Unit): TextView =
        TextView(requireContext()).apply {
            text = texto
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { alTocar() }
        }

    private fun ejecutarAccion(
        id: Int,
        exito: String,
        accion: suspend () -> ResultadoNotificaciones<NotificacionDto>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val resultado = accion()) {
                is ResultadoNotificaciones.Exito -> {
                    if (!isAdded) return@launch
                    notificaciones = notificaciones.map { actual ->
                        if (actual.id == id) resultado.datos else actual
                    }
                    actualizarContador(notificaciones.count { it.estaPendiente })
                    renderizar()
                    Toast.makeText(requireContext(), exito, Toast.LENGTH_SHORT).show()
                }

                is ResultadoNotificaciones.Error -> {
                    if (!isAdded) return@launch
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun marcarTodasLeidas() {
        val pendientes = notificaciones.filter { it.estaPendiente }
        if (pendientes.isEmpty()) {
            Toast.makeText(requireContext(), "No hay notificaciones pendientes", Toast.LENGTH_SHORT)
                .show()
            return
        }

        binding.btnMarcarTodasLeidas.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            var actualizadas = 0
            var error: String? = null

            for (notificacion in pendientes) {
                when (val resultado = repositorio.marcarLeida(notificacion.id)) {
                    is ResultadoNotificaciones.Exito -> {
                        notificaciones = notificaciones.map { actual ->
                            if (actual.id == notificacion.id) resultado.datos else actual
                        }
                        actualizadas++
                    }

                    is ResultadoNotificaciones.Error -> {
                        error = resultado.mensaje
                        break
                    }
                }
            }

            binding.btnMarcarTodasLeidas.isEnabled = true
            if (!isAdded) return@launch
            actualizarContador(notificaciones.count { it.estaPendiente })
            renderizar()

            val mensaje = when {
                actualizadas > 0 && error == null ->
                    "Se marcaron $actualizadas notificaciones como leídas"
                actualizadas > 0 && error != null ->
                    "Se marcaron $actualizadas notificaciones. $error"
                else -> error ?: "No se pudo actualizar la notificación"
            }
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarEstado(
        titulo: String,
        mensaje: String,
        accionTexto: String?,
        accion: (() -> Unit)?
    ) {
        val context = requireContext()
        binding.llNotificacionesContainer.removeAllViews()

        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
        }

        val contenido = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }

        contenido.addView(
            FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
                setBackgroundResource(R.drawable.bg_icon_circle_orange_outline)
                addView(
                    ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER)
                        setImageResource(R.drawable.ic_bell_outline)
                        setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                    }
                )
            }
        )

        contenido.addView(
            TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(16) }
                text = titulo
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            }
        )

        contenido.addView(
            TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
                gravity = Gravity.CENTER
                text = mensaje
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setLineSpacing(0f, 1.15f)
            }
        )

        if (accionTexto != null && accion != null) {
            contenido.addView(
                TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(14) }
                    text = accionTexto
                    setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    setTypeface(typeface, Typeface.BOLD)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { accion() }
                }
            )
        }

        card.addView(contenido)
        binding.llNotificacionesContainer.addView(card)
    }

    private fun navegar(fragmento: androidx.fragment.app.Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragmento)
            .addToBackStack(null)
            .commit()
    }

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()
}

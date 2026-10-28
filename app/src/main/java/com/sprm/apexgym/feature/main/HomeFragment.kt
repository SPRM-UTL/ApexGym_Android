package com.sprm.apexgym.feature.main

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.sprm.apexgym.R

/**
 * Plantilla / Mockup de la pantalla principal (Inicio) de ApexGym.
 * Incluye:
 * - Animación fluida de deslizamiento e intercambio en el menú inferior (Inicio, Membresía, Tienda, Perfil).
 * - Apertura de la pantalla de Notificaciones de abajo hacia arriba (BottomSheet).
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var selectedTabIndex = 0
    private var pillAnimator: ValueAnimator? = null

    private data class NavTabItem(
        val container: LinearLayout,
        val icon: ImageView,
        val label: TextView
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Botón de Notificaciones -> Abre pantalla de abajo hacia arriba
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

        // Avatar permite volver al Login durante las pruebas del prototipo
        val flAvatar = view.findViewById<View>(R.id.flAvatar)
        flAvatar?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        // 2. Configuración y animación del Menú Inferior
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

        // Posicionar inicialmente la píldora blanca exactamente bajo el tab activo (INICIO)
        llNavTabsRow.post {
            val activeTab = tabs[selectedTabIndex].container
            viewActiveNavPill.translationX = activeTab.left.toFloat()
            val pillParams = viewActiveNavPill.layoutParams
            pillParams.width = activeTab.width
            viewActiveNavPill.layoutParams = pillParams
        }

        tabs.forEachIndexed { index, tabItem ->
            tabItem.container.setOnClickListener {
                animateBottomNavSelection(
                    targetIndex = index,
                    tabs = tabs,
                    llNavTabsRow = llNavTabsRow,
                    viewActiveNavPill = viewActiveNavPill
                )
            }
        }
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

        // Listener registrado ANTES de TransitionManager para leer las coordenadas finales exactas tras onLayout
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

        // Transición fluida de expansión/contracción de las pestañas
        val transition = ChangeBounds().apply {
            duration = 320L
            interpolator = FastOutSlowInInterpolator()
        }
        TransitionManager.beginDelayedTransition(llNavTabsRow, transition)

        // Actualizar el estado visual de cada opción
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

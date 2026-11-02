package com.sprm.apexgym.feature.main

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.sprm.apexgym.R
import com.sprm.apexgym.feature.auth.LoginFragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var selectedTabIndex = 0
    private var isQrVisible = false
    private var pillAnimator: ValueAnimator? = null

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
        val llHomeSection = view.findViewById<View>(R.id.llHomeSection)
        val llTiendaSection = view.findViewById<View>(R.id.llTiendaSection)
        val llQrAccessSection = view.findViewById<View>(R.id.llQrAccessSection)
        val etSearchMockup = view.findViewById<EditText>(R.id.etSearchMockup)

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

        val openProfileSheet = {
            val existing = childFragmentManager.findFragmentByTag(ProfileBottomSheetFragment.TAG)
            if (existing == null) {
                ProfileBottomSheetFragment().show(
                    childFragmentManager,
                    ProfileBottomSheetFragment.TAG
                )
            }
        }

        val flAvatar = view.findViewById<View>(R.id.flAvatar)
        flAvatar?.setOnClickListener {
            openProfileSheet()
        }

        val storeCategoryChips = listOf<TextView>(
            view.findViewById(R.id.chipTiendaTodos),
            view.findViewById(R.id.chipTiendaProteinas),
            view.findViewById(R.id.chipTiendaCreatinas),
            view.findViewById(R.id.chipTiendaBebidas),
            view.findViewById(R.id.chipTiendaAccesorios),
            view.findViewById(R.id.chipTiendaRopa)
        )
        storeCategoryChips.forEach { chip ->
            chip.setOnClickListener {
                updateStoreCategoryChips(storeCategoryChips, chip)
            }
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

            if (tabIndex == 2) {
                llHomeSection.visibility = View.GONE
                llTiendaSection.visibility = View.VISIBLE
                etSearchMockup.setHint(R.string.tienda_search_hint)
            } else {
                llTiendaSection.visibility = View.GONE
                llHomeSection.visibility = View.VISIBLE
                etSearchMockup.setHint(R.string.home_search_hint)
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
                if (index == 3) {
                    openProfileSheet()
                    return@setOnClickListener
                }

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
            }
        }

        val goToTiendaListener = View.OnClickListener {
            animateBottomNavSelection(
                targetIndex = 2,
                tabs = tabs,
                llNavTabsRow = llNavTabsRow,
                viewActiveNavPill = viewActiveNavPill
            )
            showSectionForTab(2)
        }
        view.findViewById<View>(R.id.cardTienda)?.setOnClickListener(goToTiendaListener)
        view.findViewById<View>(R.id.btnComprarMockup)?.setOnClickListener(goToTiendaListener)
        view.findViewById<View>(R.id.btnVerCatalogoMockup)?.setOnClickListener(goToTiendaListener)
        view.findViewById<View>(R.id.chipHomeInsumos)?.setOnClickListener(goToTiendaListener)

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
            llTiendaSection.visibility = View.GONE
            llQrAccessSection.visibility = View.VISIBLE

            scrollHome.smoothScrollTo(0, 0)
        }

        view.findViewById<View>(R.id.fabQrMockup)?.setOnClickListener(openQrAccessListener)
        view.findViewById<View>(R.id.chipHomeAccesoQr)?.setOnClickListener(openQrAccessListener)

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
    }

    private fun updateStoreCategoryChips(chips: List<TextView>, selected: TextView) {
        val context = requireContext()
        val colorWhite = ContextCompat.getColor(context, R.color.white)
        val colorPrimary = ContextCompat.getColor(context, R.color.apex_text_primary)

        chips.forEach { chip ->
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(colorWhite)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(colorPrimary)
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

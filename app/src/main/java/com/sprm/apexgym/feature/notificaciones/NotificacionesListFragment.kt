package com.sprm.apexgym.feature.notificaciones

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentNotificacionesListBinding

/**
 * La API actual no expone notificaciones ni su configuración.
 * Se muestra un estado explícito en lugar de datos de ejemplo.
 */
class NotificacionesListFragment : BaseFragment<FragmentNotificacionesListBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_notificaciones_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.cvUnreadBadge.visibility = View.GONE
        binding.tabLayoutNotificaciones.visibility = View.GONE
        binding.vpNotificacionesTabs.visibility = View.GONE
        binding.etSearchNotificaciones.isEnabled = false
        binding.etSearchNotificaciones.hint = "No disponible"

        binding.flConfig.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Configuración no disponible")
                .setMessage(
                    "La API actual no permite consultar ni guardar preferencias de notificaciones."
                )
                .setPositiveButton("Entendido", null)
                .show()
        }

        binding.llNotificacionesContainer.removeAllViews()
        binding.llNotificacionesContainer.addView(crearEstadoNoDisponible())
    }

    private fun crearEstadoNoDisponible(): View {
        val context = requireContext()
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

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }

        content.addView(
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

        content.addView(
            TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(16) }
                text = "Notificaciones no disponibles"
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        )

        content.addView(
            TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
                gravity = Gravity.CENTER
                text = "La API actual no expone un endpoint de notificaciones. " +
                    "Este módulo se activará cuando el backend lo incorpore."
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setLineSpacing(0f, 1.15f)
            }
        )

        card.addView(content)
        return card
    }

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()
}

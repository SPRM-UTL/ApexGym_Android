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
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentNotificacionesListBinding

class NotificacionesListFragment : BaseFragment<FragmentNotificacionesListBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_notificaciones_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupSampleNotifications()
    }

    private fun setupViews() {
        binding.cvUnreadBadge.visibility = View.VISIBLE
        binding.tvUnreadCount.text = "Tienes 3 notificaciones importantes"

        binding.tabLayoutNotificaciones.addTab(binding.tabLayoutNotificaciones.newTab().setText("Todas"))
        binding.tabLayoutNotificaciones.addTab(binding.tabLayoutNotificaciones.newTab().setText("No Leídas"))
        binding.tabLayoutNotificaciones.addTab(binding.tabLayoutNotificaciones.newTab().setText("Sistema"))
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flConfig.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, ConfiguracionNotificacionesFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.btnMarcarTodasLeidas.setOnClickListener {
            Toast.makeText(requireContext(), "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show()
            binding.cvUnreadBadge.visibility = View.GONE
        }
        binding.cvUnreadBadge.setOnClickListener {
            abrirDetalleNotificacion()
        }
        binding.etSearchNotificaciones.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                Toast.makeText(requireContext(), "Búsqueda de notificaciones realizada", Toast.LENGTH_SHORT).show()
                true
            } else false
        }
    }

    private fun abrirDetalleNotificacion() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, NotificacionDetalleFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun setupSampleNotifications() {
        binding.llNotificacionesContainer.removeAllViews()

        val notificaciones = listOf(
            Triple("¡Tu membresía vence pronto!", "Tu plan Premium vence en 15 días. Renuévalo a tiempo para mantener tus beneficios.", "Hace 2 horas"),
            Triple("Compra confirmada", "Tu pedido de Proteína Whey 2kg ha sido procesado exitosamente.", "Ayer"),
            Triple("Mantenimiento programado", "El área de alberca y sauna estará en servicio este domingo.", "Hace 3 días")
        )

        notificaciones.forEach { (titulo, mensaje, hora) ->
            val card = crearTarjetaNotificacion(titulo, mensaje, hora)
            binding.llNotificacionesContainer.addView(card)
        }
    }

    private fun crearTarjetaNotificacion(titulo: String, mensaje: String, hora: String): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(2).toFloat()
            strokeWidth = 0
            isClickable = true
            isFocusable = true
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val iconBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42))
            setBackgroundResource(R.drawable.bg_icon_circle_orange_outline)

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(20), dpToPx(20), Gravity.CENTER)
                setImageResource(R.drawable.ic_notification_bell)
                setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
            }
            addView(icon)
        }
        row.addView(iconBox)

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
                marginEnd = dpToPx(8)
            }
        }

        val tvTitulo = TextView(context).apply {
            text = titulo
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        textCol.addView(tvTitulo)

        val tvMsg = TextView(context).apply {
            text = mensaje
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 2
            setLineSpacing(0f, 1.15f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
        }
        textCol.addView(tvMsg)

        val tvHora = TextView(context).apply {
            text = hora
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }
        textCol.addView(tvHora)

        row.addView(textCol)

        val chevron = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18))
            setImageResource(R.drawable.ic_chevron_right)
            setColorFilter(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
        }
        row.addView(chevron)

        card.addView(row)
        card.setOnClickListener {
            abrirDetalleNotificacion()
        }

        return card
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}

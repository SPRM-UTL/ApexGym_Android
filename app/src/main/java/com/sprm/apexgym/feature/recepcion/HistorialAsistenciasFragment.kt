package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentHistorialAsistenciasBinding

class HistorialAsistenciasFragment : BaseFragment<FragmentHistorialAsistenciasBinding>() {

    data class AsistenciaMock(
        val fecha: String,
        val tipo: String,
        val plan: String
    )

    private val listaMock = listOf(
        AsistenciaMock("Hoy, 08:15 AM", "Entrada QR", "Plan Premium"),
        AsistenciaMock("Ayer, 07:50 PM", "Entrada QR", "Plan Premium"),
        AsistenciaMock("09/11/2026, 06:30 AM", "Entrada manual", "Plan Premium"),
        AsistenciaMock("08/11/2026, 07:10 PM", "Salida", "Plan Premium")
    )

    override fun getLayoutRes(): Int = R.layout.fragment_historial_asistencias

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvTotalAccesos.text = "${listaMock.size}"
        binding.tvEsteMes.text = "${listaMock.size}"
        binding.tvRachaActual.text = "5"

        setupRecyclerView()
    }

    private fun abrirDetalle() {
        val sheet = AsistenciaDetalleSheetFragment()
        sheet.show(parentFragmentManager, AsistenciaDetalleSheetFragment.TAG)
    }

    private inner class AsistenciaViewHolder(
        itemView: View,
        val tvFecha: TextView,
        val tvTipo: TextView,
        val tvPlan: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: AsistenciaMock) {
            tvFecha.text = item.fecha
            tvTipo.text = item.tipo
            tvPlan.text = item.plan
            itemView.setOnClickListener { abrirDetalle() }
        }
    }

    private fun setupRecyclerView() {
        binding.rvHistorialAsistencias.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorialAsistencias.adapter = object : RecyclerView.Adapter<AsistenciaViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsistenciaViewHolder {
                val card = MaterialCardView(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dpToPx(12)
                    }
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
                    radius = dpToPx(18).toFloat()
                    cardElevation = dpToPx(3).toFloat()
                    strokeWidth = 0
                    isClickable = true
                    isFocusable = true
                }

                val row = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                }

                val iconBox = FrameLayout(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44))
                    setBackgroundResource(R.drawable.bg_circle_blue_light)
                    val icon = ImageView(parent.context).apply {
                        layoutParams = FrameLayout.LayoutParams(dpToPx(22), dpToPx(22), Gravity.CENTER)
                        setImageResource(R.drawable.ic_calendar_clock)
                        setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                    }
                    addView(icon)
                }
                row.addView(iconBox)

                val col = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = dpToPx(14)
                    }
                }

                val tvFecha = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                col.addView(tvFecha)

                val tvTipo = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                }
                col.addView(tvTipo)

                val tvPlan = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                col.addView(tvPlan)

                row.addView(col)

                val chevron = ImageView(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20))
                    setImageResource(R.drawable.ic_chevron_right)
                    setColorFilter(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
                }
                row.addView(chevron)

                card.addView(row)

                return AsistenciaViewHolder(card, tvFecha, tvTipo, tvPlan)
            }

            override fun onBindViewHolder(holder: AsistenciaViewHolder, position: Int) {
                holder.bind(listaMock[position])
            }

            override fun getItemCount(): Int = listaMock.size
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}

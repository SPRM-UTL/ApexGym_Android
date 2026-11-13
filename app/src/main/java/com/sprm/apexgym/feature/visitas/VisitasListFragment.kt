package com.sprm.apexgym.feature.visitas

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentVisitasListBinding

class VisitasListFragment : BaseFragment<FragmentVisitasListBinding>() {

    data class VisitaMock(
        val prospecto: String,
        val estado: String,
        val telefono: String,
        val asesor: String,
        val interes: String
    )

    private val listaVisitasMock = listOf(
        VisitaMock("Ana Martínez", "NUEVO", "4771234567", "Carlos Rodríguez", "Plan Anual VIP"),
        VisitaMock("Roberto López", "CONTACTADO", "4779876543", "María Gómez", "Plan Mensual"),
        VisitaMock("Sofía Herrera", "PROPUESTA", "4775551234", "Carlos Rodríguez", "Entrenamiento Personalizado"),
        VisitaMock("Daniel Morales", "CALIFICADO", "4773339988", "María Gómez", "Pase Diario / Semanal")
    )

    override fun getLayoutRes(): Int = R.layout.fragment_visitas_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerView()
    }

    private fun setupViews() {
        binding.tvTotalVisitas.text = "${listaVisitasMock.size}"
        binding.tvVisitasPendientes.text = "${listaVisitasMock.count { it.estado == "NUEVO" || it.estado == "CONTACTADO" }}"
        binding.tvVisitasConvertidos.text = "12"
        binding.tvVisitasPerdidos.text = "2"
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddVisita.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, CrearEditarVisitaFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.ivFilterVisitas.setOnClickListener {
            Toast.makeText(requireContext(), "Filtrar prospectos", Toast.LENGTH_SHORT).show()
        }
        binding.etSearchVisitas.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                Toast.makeText(requireContext(), "Búsqueda de prospectos", Toast.LENGTH_SHORT).show()
                true
            } else false
        }

        val abrirDetalle = View.OnClickListener {
            abrirDetalleVisita()
        }
        binding.tvTotalVisitas.setOnClickListener(abrirDetalle)
        binding.tvVisitasPendientes.setOnClickListener(abrirDetalle)
        binding.tvVisitasConvertidos.setOnClickListener(abrirDetalle)
        binding.tvVisitasPerdidos.setOnClickListener(abrirDetalle)
        binding.llEmptyVisitas.setOnClickListener(abrirDetalle)
    }

    private fun abrirDetalleVisita() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, VisitaDetalleFragment())
            .addToBackStack(null)
            .commit()
    }

    private inner class VisitaViewHolder(
        itemView: View,
        val tvNombre: TextView,
        val tvEstado: TextView,
        val tvInteres: TextView,
        val tvAsesor: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: VisitaMock) {
            tvNombre.text = item.prospecto
            tvEstado.text = item.estado
            tvInteres.text = "Interés: ${item.interes}"
            tvAsesor.text = "Asesor: ${item.asesor} • Tel: ${item.telefono}"
            itemView.setOnClickListener {
                abrirDetalleVisita()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvVisitas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVisitas.adapter = object : RecyclerView.Adapter<VisitaViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitaViewHolder {
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
                    setBackgroundResource(R.drawable.bg_circle_purple_light)
                    val icon = ImageView(parent.context).apply {
                        layoutParams = FrameLayout.LayoutParams(dpToPx(22), dpToPx(22), Gravity.CENTER)
                        setImageResource(R.drawable.ic_calendar_event)
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

                val tvNombre = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                col.addView(tvNombre)

                val tvInteres = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                }
                col.addView(tvInteres)

                val tvAsesor = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                col.addView(tvAsesor)

                row.addView(col)

                val tvEstado = TextView(parent.context).apply {
                    setBackgroundResource(R.drawable.bg_badge_orange)
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                row.addView(tvEstado)

                card.addView(row)

                return VisitaViewHolder(card, tvNombre, tvEstado, tvInteres, tvAsesor)
            }

            override fun onBindViewHolder(holder: VisitaViewHolder, position: Int) {
                holder.bind(listaVisitasMock[position])
            }

            override fun getItemCount(): Int = listaVisitasMock.size
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

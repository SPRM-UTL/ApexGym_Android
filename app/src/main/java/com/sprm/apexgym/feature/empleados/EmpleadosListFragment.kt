package com.sprm.apexgym.feature.empleados

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
import com.sprm.apexgym.databinding.FragmentEmpleadosListBinding

class EmpleadosListFragment : BaseFragment<FragmentEmpleadosListBinding>() {

    data class EmpleadoMock(
        val nombre: String,
        val rol: String,
        val email: String,
        val estado: String
    )

    private val listaMock = listOf(
        EmpleadoMock("Carlos Rodríguez", "Entrenador Personal", "carlos@apexgym.com", "ACTIVO"),
        EmpleadoMock("María Gómez", "Recepcionista", "maria@apexgym.com", "ACTIVO"),
        EmpleadoMock("Jorge Sánchez", "Nutriólogo Deportivo", "jorge@apexgym.com", "EN TURNO"),
        EmpleadoMock("Valeria Soto", "Instructora de Spinning", "valeria@apexgym.com", "ACTIVO")
    )

    override fun getLayoutRes(): Int = R.layout.fragment_empleados_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        setupRecyclerView()
    }

    private fun setupViews() {
        binding.tvTotalEmpleados.text = "${listaMock.size}"
        binding.tvEmpleadosActivos.text = "${listaMock.count { it.estado == "ACTIVO" || it.estado == "EN TURNO" }}"
        binding.tvEmpleadosEnTurno.text = "${listaMock.count { it.estado == "EN TURNO" }}"
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddEmpleado.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, CrearEditarEmpleadoFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.ivFilter.setOnClickListener {
            Toast.makeText(requireContext(), "Filtro de empleados", Toast.LENGTH_SHORT).show()
        }
        binding.etSearchEmpleados.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                Toast.makeText(requireContext(), "Búsqueda realizada", Toast.LENGTH_SHORT).show()
                true
            } else false
        }

        val abrirDetalle = View.OnClickListener {
            abrirDetalleEmpleado()
        }
        binding.tvTotalEmpleados.setOnClickListener(abrirDetalle)
        binding.tvEmpleadosActivos.setOnClickListener(abrirDetalle)
        binding.tvEmpleadosEnTurno.setOnClickListener(abrirDetalle)
        binding.llEmptyEmpleados.setOnClickListener(abrirDetalle)
    }

    private fun abrirDetalleEmpleado() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, EmpleadoDetalleFragment())
            .addToBackStack(null)
            .commit()
    }

    private inner class EmpleadoViewHolder(
        itemView: View,
        val tvNombre: TextView,
        val tvRol: TextView,
        val tvEmail: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: EmpleadoMock) {
            tvNombre.text = item.nombre
            tvRol.text = item.rol
            tvEmail.text = item.email
            itemView.setOnClickListener {
                abrirDetalleEmpleado()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvEmpleados.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEmpleados.adapter = object : RecyclerView.Adapter<EmpleadoViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpleadoViewHolder {
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

                val avatar = FrameLayout(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(46), dpToPx(46))
                    setBackgroundResource(R.drawable.bg_icon_circle_orange_outline)
                    val icon = ImageView(parent.context).apply {
                        layoutParams = FrameLayout.LayoutParams(dpToPx(24), dpToPx(24), Gravity.CENTER)
                        setImageResource(R.drawable.ic_user_profile)
                        setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                    }
                    addView(icon)
                }
                row.addView(avatar)

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

                val tvRol = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                }
                col.addView(tvRol)

                val tvEmail = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                col.addView(tvEmail)

                row.addView(col)

                val chevron = ImageView(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20))
                    setImageResource(R.drawable.ic_chevron_right)
                    setColorFilter(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
                }
                row.addView(chevron)

                card.addView(row)

                return EmpleadoViewHolder(card, tvNombre, tvRol, tvEmail)
            }

            override fun onBindViewHolder(holder: EmpleadoViewHolder, position: Int) {
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

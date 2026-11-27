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
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.VisitaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.fechaHoyIso
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.inicioDeMesIso
import com.sprm.apexgym.databinding.FragmentVisitasListBinding
import kotlinx.coroutines.launch

class VisitasListFragment : BaseFragment<FragmentVisitasListBinding>() {

    private var visitas: List<VisitaDto> = emptyList()
    private var filtroSeleccionado = "Todos"
    private var textoBusqueda = ""
    private val chips = mutableListOf<TextView>()

    override fun getLayoutRes(): Int = R.layout.fragment_visitas_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvVisitas.layoutManager = LinearLayoutManager(requireContext())
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarVisitas() }
        binding.flAddVisita.setOnClickListener {
            navegar(CrearEditarVisitaFragment.newInstance())
        }
        binding.ivFilterVisitas.setOnClickListener {
            binding.etSearchVisitas.setText("")
        }

        binding.etSearchVisitas.doAfterTextChanged { texto ->
            textoBusqueda = texto?.toString()?.trim().orEmpty()
            binding.ivFilterVisitas.visibility = if (textoBusqueda.isNotEmpty()) View.VISIBLE else View.GONE
            aplicarFiltros()
        }

        construirChips()
    }

    override fun onResume() {
        super.onResume()
        if (view != null) {
            cargarVisitas()
        }
    }

    private fun cargarVisitas() {
        binding.rvVisitas.visibility = View.GONE
        binding.llEmptyVisitas.visibility = View.VISIBLE
        binding.llEmptyVisitas.getChildAt(1)?.let { (it as? TextView)?.text = "Cargando visitas…" }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerVisitas(
                    fechaInicio = inicioDeMesIso(),
                    fechaFin = fechaHoyIso()
                )
                if (!isAdded || view == null) return@launch
                visitas = response.dataExitosa().orEmpty()
                actualizarConteosChips()
                aplicarFiltros()
            } catch (_: Exception) {
                if (!isAdded || view == null) return@launch
                visitas = emptyList()
                actualizarConteosChips()
                aplicarFiltros()
                binding.llEmptyVisitas.getChildAt(1)?.let {
                    (it as? TextView)?.text = "No se pudo conectar con el servidor"
                }
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun actualizarConteosChips() {
        val hoyIso = fechaHoyIso()
        val conteos = mapOf(
            "Todos" to visitas.size,
            "Hoy" to visitas.count { formatearFecha(it.fechaHora, "yyyy-MM-dd") == hoyIso },
            "Con costo" to visitas.count { it.costo > 0 },
            "Gratuitas" to visitas.count { it.costo <= 0 }
        )
        chips.forEach { chip ->
            val key = chip.tag as? String ?: return@forEach
            val total = conteos[key] ?: 0
            chip.text = "$key ($total)"
        }
    }

    private fun construirChips() {
        val container = binding.llFilterChips
        container.removeAllViews()
        chips.clear()

        listOf("Todos", "Hoy", "Con costo", "Gratuitas").forEachIndexed { index, nombre ->
            val chip = TextView(requireContext()).apply {
                tag = nombre
                text = "$nombre (0)"
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(18), 0, dpToPx(18), 0)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(38)
                ).apply {
                    if (index > 0) marginStart = dpToPx(8)
                }
            }
            chips.add(chip)
            container.addView(chip)
            chip.setOnClickListener {
                filtroSeleccionado = chip.tag as? String ?: "Todos"
                actualizarEstilos()
                aplicarFiltros()
            }
        }

        actualizarEstilos()
    }

    private fun actualizarEstilos() {
        val blanco = ContextCompat.getColor(requireContext(), R.color.white)
        val primario = ContextCompat.getColor(requireContext(), R.color.apex_text_primary)
        chips.forEach { chip ->
            if ((chip.tag as? String) == filtroSeleccionado) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(blanco)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(primario)
            }
        }
    }

    private fun aplicarFiltros() {
        val filtradas = visitas.filter { visita ->
            val cumpleFiltro = when (filtroSeleccionado) {
                "Hoy" -> formatearFecha(visita.fechaHora, "yyyy-MM-dd") == fechaHoyIso()
                "Con costo" -> visita.costo > 0
                "Gratuitas" -> visita.costo <= 0
                else -> true
            }
            if (!cumpleFiltro) return@filter false
            if (textoBusqueda.isBlank()) return@filter true

            listOf(
                visita.nombreVisitante.orEmpty(),
                visita.tipoVisita?.nombre.orEmpty(),
                visita.observaciones.orEmpty()
            ).any { it.contains(textoBusqueda, ignoreCase = true) }
        }.sortedByDescending { it.fechaHora.orEmpty() }

        if (filtradas.isEmpty()) {
            binding.llEmptyVisitas.visibility = View.VISIBLE
            binding.rvVisitas.visibility = View.GONE
            val mensaje = if (visitas.isEmpty()) {
                "No hay visitas registradas este mes"
            } else {
                "No hay visitas que coincidan con el filtro"
            }
            binding.llEmptyVisitas.getChildAt(1)?.let { (it as? TextView)?.text = mensaje }
            return
        }

        binding.llEmptyVisitas.visibility = View.GONE
        binding.rvVisitas.visibility = View.VISIBLE

        binding.rvVisitas.adapter = object : RecyclerView.Adapter<VisitaViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitaViewHolder {
                return crearTarjeta(parent.context)
            }

            override fun onBindViewHolder(holder: VisitaViewHolder, position: Int) {
                val visita = filtradas[position]
                holder.tvNombre.text = visita.nombreVisitante ?: "Visitante #${visita.id}"
                holder.tvTipo.text = visita.tipoVisita?.nombre ?: "Tipo #${visita.tipoVisitaId}"
                holder.tvDetalle.text = buildString {
                    append(formatearFecha(visita.fechaHora, "dd/MM/yyyy HH:mm"))
                    visita.observaciones
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            append(" • ")
                            append(if (it.length > 40) it.take(40) + "…" else it)
                        }
                }
                holder.tvCosto.text = if (visita.costo > 0) {
                    visita.costo.formatearMonedaSinSufijo()
                } else {
                    "Gratis"
                }
                holder.tvCosto.setBackgroundResource(
                    if (visita.costo > 0) R.drawable.bg_badge_orange else R.drawable.bg_chip_unselected
                )
                holder.tvCosto.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (visita.costo > 0) R.color.white else R.color.apex_text_secondary
                    )
                )
                holder.itemView.setOnClickListener {
                    navegar(VisitaDetalleFragment.newInstance(visita.id))
                }
            }

            override fun getItemCount(): Int = filtradas.size
        }
    }

    private class VisitaViewHolder(
        itemView: View,
        val tvNombre: TextView,
        val tvTipo: TextView,
        val tvDetalle: TextView,
        val tvCosto: TextView
    ) : RecyclerView.ViewHolder(itemView)

    private fun crearTarjeta(context: android.content.Context): VisitaViewHolder {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            radius = dpToPx(16).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
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
            setBackgroundResource(R.drawable.bg_chip_unselected)
            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(20), dpToPx(20), Gravity.CENTER)
                setImageResource(R.drawable.ic_calendar_event)
                setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
            }
            addView(icon)
        }
        row.addView(iconBox)

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(13)
                marginEnd = dpToPx(8)
            }
        }

        val tvNombre = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        col.addView(tvNombre)

        val tvTipo = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        }
        col.addView(tvTipo)

        val tvDetalle = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
        }
        col.addView(tvDetalle)
        row.addView(col)

        val tvCosto = TextView(context).apply {
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        row.addView(tvCosto)

        card.addView(row)
        return VisitaViewHolder(card, tvNombre, tvTipo, tvDetalle, tvCosto)
    }

    private fun navegar(fragment: androidx.fragment.app.Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}

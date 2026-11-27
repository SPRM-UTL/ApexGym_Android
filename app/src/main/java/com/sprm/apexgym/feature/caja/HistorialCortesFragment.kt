package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.AperturaCajaDto
import com.sprm.apexgym.core.network.api.CorteCajaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.databinding.FragmentHistorialCortesBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HistorialCortesFragment : BaseFragment<FragmentHistorialCortesBinding>() {

    private var cortes: List<CorteCajaDto> = emptyList()
    private var aperturasPorId: Map<Int, AperturaCajaDto> = emptyMap()
    private var filtroSeleccionado = "Todos"
    private var textoBusqueda = ""
    private val chips = mutableListOf<TextView>()

    override fun getLayoutRes(): Int = R.layout.fragment_historial_cortes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHistorialCortes.layoutManager = LinearLayoutManager(requireContext())
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarCortes() }

        parentFragmentManager.setFragmentResultListener(
            CorteDetalleFragment.RESULT_KEY_CORTE_VALIDADO,
            viewLifecycleOwner
        ) { _, _ ->
            cargarCortes()
        }

        binding.etSearchCortes.doAfterTextChanged { editable ->
            textoBusqueda = editable?.toString()?.trim().orEmpty()
            aplicarFiltros()
        }

        construirChips()
        cargarCortes()
    }

    private fun cargarCortes() {
        binding.llEmptyCortes.visibility = View.GONE
        binding.rvHistorialCortes.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val cortesDef = async { RetrofitClient.empleadoService.obtenerCortes() }
                val aperturasDef = async { RetrofitClient.empleadoService.obtenerAperturas() }

                val respuestaCortes = cortesDef.await()
                val aperturas = aperturasDef.await().dataExitosa().orEmpty()
                if (!isAdded || view == null) return@launch

                cortes = respuestaCortes.dataExitosa().orEmpty()
                aperturasPorId = aperturas.associateBy { it.id }

                aplicarFiltros()
            } catch (_: Exception) {
                if (!isAdded || view == null) return@launch
                cortes = emptyList()
                aperturasPorId = emptyMap()
                aplicarFiltros()
                binding.llEmptyCortes.getChildAt(1)?.let {
                    (it as? TextView)?.text = "No se pudo conectar con el servidor"
                }
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun construirChips() {
        val container = binding.llFilterChips
        container.removeAllViews()
        chips.clear()

        listOf("Todos", "Validados", "Pendientes", "Este mes").forEachIndexed { index, nombre ->
            val chip = TextView(requireContext()).apply {
                text = nombre
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
                filtroSeleccionado = chip.text.toString()
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
            if (chip.text.toString() == filtroSeleccionado) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(blanco)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(primario)
            }
        }
    }

    private fun aplicarFiltros() {
        val mesActual = SimpleDateFormat("yyyy-MM", Locale.US).format(System.currentTimeMillis())

        val filtrados = cortes.filter { corte ->
            val estado = corte.estado?.uppercase().orEmpty()
            val cumpleFiltro = when (filtroSeleccionado) {
                "Validados" -> estado == "VALIDADO"
                "Pendientes" -> estado == "PENDIENTE"
                "Este mes" -> formatearFecha(corte.fechaCorte, "yyyy-MM") == mesActual
                else -> true
            }

            if (!cumpleFiltro) return@filter false
            if (textoBusqueda.isBlank()) return@filter true

            val aperturaRel = corte.aperturaCaja ?: aperturasPorId[corte.aperturaCajaId]
            val caja = aperturaRel?.caja?.nombre.orEmpty()
            val responsable = corte.empleado?.nombreCompleto
                ?: aperturaRel?.empleado?.nombreCompleto.orEmpty()
            val fecha = formatearFecha(corte.fechaCorte, "dd/MM/yyyy HH:mm")
            listOf(
                caja,
                responsable,
                fecha,
                corte.estado.orEmpty(),
                corte.id.toString(),
                corte.aperturaCajaId.toString()
            ).any { it.contains(textoBusqueda, ignoreCase = true) }
        }.sortedByDescending { it.id }

        if (filtrados.isEmpty()) {
            binding.llEmptyCortes.visibility = View.VISIBLE
            binding.rvHistorialCortes.visibility = View.GONE
            val mensaje = if (cortes.isEmpty()) {
                "No hay cortes registrados"
            } else {
                "No hay cortes que coincidan con el filtro"
            }
            binding.llEmptyCortes.getChildAt(1)?.let { (it as? TextView)?.text = mensaje }
            return
        }

        binding.llEmptyCortes.visibility = View.GONE
        binding.rvHistorialCortes.visibility = View.VISIBLE

        binding.rvHistorialCortes.adapter = object : RecyclerView.Adapter<CorteHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CorteHolder {
                return crearFila(parent.context)
            }

            override fun onBindViewHolder(holder: CorteHolder, position: Int) {
                val corte = filtrados[position]
                val aperturaRel = corte.aperturaCaja ?: aperturasPorId[corte.aperturaCajaId]
                val caja = aperturaRel?.caja?.nombre ?: "Apertura #${corte.aperturaCajaId}"
                holder.tvTitulo.text = "Corte #${corte.id} • $caja"
                holder.tvDetalle.text = buildString {
                    append(formatearFecha(corte.fechaCorte, "dd/MM/yyyy HH:mm"))
                    append(" • ")
                    append(corte.estado ?: "—")
                    append(" • Dif.: ")
                    append(corte.diferencia.formatearMonedaSinSufijo())
                }
                holder.tvMonto.text = corte.efectivoContado.formatearMonedaSinSufijo()
                holder.itemView.setOnClickListener { mostrarDetalleCorte(corte, caja, aperturaRel) }
            }

            override fun getItemCount(): Int = filtrados.size
        }
    }

    private fun mostrarDetalleCorte(
        corte: CorteCajaDto,
        cajaNombre: String,
        aperturaRel: AperturaCajaDto?
    ) {
        val responsable = corte.empleado?.nombreCompleto?.takeIf { it.isNotBlank() }
            ?: aperturaRel?.empleado?.nombreCompleto?.takeIf { it.isNotBlank() }
            ?: "Empleado"

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                CorteDetalleFragment.newInstance(
                    corte = corte,
                    cajaNombre = cajaNombre,
                    responsable = responsable
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private class CorteHolder(
        itemView: View,
        val tvTitulo: TextView,
        val tvDetalle: TextView,
        val tvMonto: TextView
    ) : RecyclerView.ViewHolder(itemView)

    private fun crearFila(context: android.content.Context): CorteHolder {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(12) }
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
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
        }

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitulo = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        col.addView(tvTitulo)
        val tvDetalle = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
        }
        col.addView(tvDetalle)
        row.addView(col)

        val tvMonto = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        row.addView(tvMonto)
        card.addView(row)
        return CorteHolder(card, tvTitulo, tvDetalle, tvMonto)
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

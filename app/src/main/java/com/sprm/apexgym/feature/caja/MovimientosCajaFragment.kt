package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.AperturaCajaDto
import com.sprm.apexgym.core.network.api.MovimientoCajaDto
import com.sprm.apexgym.core.network.api.ResumenAperturaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentMovimientosCajaBinding
import kotlinx.coroutines.launch

class MovimientosCajaFragment : BaseFragment<FragmentMovimientosCajaBinding>() {

    private var aperturaId: Int = 0
    private var apertura: AperturaCajaDto? = null
    private var movimientos: List<MovimientoCajaDto> = emptyList()
    private var filtroSeleccionado = "Todos"
    private val chips = mutableListOf<TextView>()

    override fun getLayoutRes(): Int = R.layout.fragment_movimientos_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aperturaId = arguments?.getInt(ARG_APERTURA_ID, 0) ?: 0

        binding.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddMovement.setOnClickListener {
            if (apertura == null) {
                Toast.makeText(requireContext(), "No hay una caja abierta", Toast.LENGTH_SHORT).show()
            } else {
                NuevoMovimientoSheetFragment.newInstance(apertura!!.id)
                    .show(parentFragmentManager, NuevoMovimientoSheetFragment.TAG)
            }
        }

        parentFragmentManager.setFragmentResultListener(
            NuevoMovimientoSheetFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, _ -> cargarMovimientos() }

        construirChips()
        cargarMovimientos()
    }

    private fun cargarMovimientos() {
        binding.tvCajaNombre.text = "Cargando caja…"
        binding.tvAperturaInfo.text = ""
        binding.tvSaldoActual.text = "…"
        ocultarListaVacia()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val aperturas = RetrofitClient.empleadoService.obtenerAperturas().dataExitosa().orEmpty()
                val aperturaEncontrada = if (aperturaId > 0) {
                    aperturas.firstOrNull { it.id == aperturaId }
                } else {
                    aperturas.filter { it.estado?.uppercase() != "CERRADA" }.maxByOrNull { it.id }
                }

                apertura = aperturaEncontrada ?: if (aperturaId > 0) {
                    AperturaCajaDto(id = aperturaId)
                } else {
                    null
                }

                val actual = apertura
                if (actual == null) {
                    binding.tvCajaNombre.text = "No hay caja abierta"
                    binding.tvAperturaInfo.text = "Abre una caja para registrar movimientos"
                    binding.tvSaldoActual.text = 0.0.formatearMonedaSinSufijo()
                    movimientos = emptyList()
                    aplicarFiltro()
                    return@launch
                }

                val resumen = RetrofitClient.empleadoService
                    .obtenerResumenApertura(actual.id)
                    .dataExitosa()

                movimientos = resumen?.movimientos.orEmpty()
                mostrarCabecera(actual, resumen)
                aplicarFiltro()
            } catch (_: Exception) {
                binding.tvCajaNombre.text = "Sin conexión"
                binding.tvAperturaInfo.text = "No se pudo conectar con el servidor"
                binding.tvSaldoActual.text = "—"
                movimientos = emptyList()
                aplicarFiltro()
            }
        }
    }

    private fun mostrarCabecera(actual: AperturaCajaDto, resumen: ResumenAperturaDto?) {
        binding.tvCajaNombre.text = actual.caja?.nombre
            ?.let { "Caja: $it" }
            ?: "Caja abierta #${actual.id}"
        binding.tvAperturaInfo.text = buildString {
            append("Abierta: ")
            append(formatearFecha(actual.fechaApertura, "dd/MM/yyyy HH:mm"))
            append(" • Fondo: ")
            append((resumen?.montoInicial ?: actual.montoInicial).formatearMonedaSinSufijo())
            append(" • Esperado: ")
            append((resumen?.saldoDisponible ?: actual.montoInicial).formatearMonedaSinSufijo())
        }
        binding.tvSaldoActual.text =
            (resumen?.saldoDisponible ?: actual.montoInicial).formatearMonedaSinSufijo()
    }

    private fun construirChips() {
        val container = binding.llFilterChips
        container.removeAllViews()
        chips.clear()

        listOf("Todos", "Entradas", "Salidas", "Pendientes", "Validados").forEachIndexed { index, nombre ->
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
                aplicarFiltro()
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

    private fun aplicarFiltro() {
        val filtrados = movimientos.filter { movimiento ->
            when (filtroSeleccionado) {
                "Entradas" -> movimiento.tipo?.uppercase() == "ENTRADA"
                "Salidas" -> movimiento.tipo?.uppercase() == "SALIDA"
                "Pendientes" -> movimiento.estado?.uppercase() == "PENDIENTE"
                "Validados" -> movimiento.estado?.uppercase() == "VALIDADO"
                else -> true
            }
        }.sortedByDescending { it.id }

        if (filtrados.isEmpty()) {
            binding.llEmptyMovimientos.visibility = View.VISIBLE
            binding.rvMovimientos.visibility = View.GONE
            val mensaje = if (movimientos.isEmpty()) {
                "No hay movimientos registrados"
            } else {
                "No hay movimientos con este filtro"
            }
            binding.llEmptyMovimientos.getChildAt(1)?.let { (it as? TextView)?.text = mensaje }
        } else {
            binding.llEmptyMovimientos.visibility = View.GONE
            binding.rvMovimientos.visibility = View.VISIBLE
        }

        binding.rvMovimientos.adapter = object : RecyclerView.Adapter<MovimientoHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoHolder {
                return crearFila(parent.context)
            }

            override fun onBindViewHolder(holder: MovimientoHolder, position: Int) {
                val movimiento = filtrados[position]
                val esSalida = movimiento.tipo?.uppercase() == "SALIDA"
                holder.tvTitulo.text = movimiento.concepto ?: "Movimiento #${movimiento.id}"
                holder.tvDetalle.text = buildString {
                    append(movimiento.tipo ?: "—")
                    append(" • ")
                    append(formatearFecha(movimiento.createdAt, "dd/MM/yyyy HH:mm"))
                    append(" • ")
                    append(movimiento.estado ?: "—")
                }
                holder.tvMonto.text =
                    (if (esSalida) "-" else "+") + movimiento.monto.formatearMonedaSinSufijo()
                holder.tvAccion.text = if (movimiento.estado?.uppercase() == "PENDIENTE") {
                    "Validar"
                } else {
                    ""
                }

                holder.itemView.setOnClickListener {
                    if (movimiento.estado?.uppercase() == "PENDIENTE") {
                        confirmarValidacion(movimiento)
                    }
                }
            }

            override fun getItemCount(): Int = filtrados.size
        }
    }

    private fun confirmarValidacion(movimiento: MovimientoCajaDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Validar movimiento")
            .setMessage(
                "Se validará ${movimiento.tipo?.lowercase() ?: "el movimiento"} de " +
                    movimiento.monto.formatearMonedaSinSufijo() +
                    ".\n\nConcepto: ${movimiento.concepto ?: "—"}"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Validar") { _, _ -> validarMovimiento(movimiento) }
            .show()
    }

    private fun validarMovimiento(movimiento: MovimientoCajaDto) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.validarMovimiento(movimiento.id)
                if (response.dataExitosa() == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo validar el movimiento",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Movimiento validado", Toast.LENGTH_SHORT).show()
                    cargarMovimientos()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun ocultarListaVacia() {
        binding.llEmptyMovimientos.visibility = View.GONE
        binding.rvMovimientos.visibility = View.VISIBLE
    }

    private class MovimientoHolder(
        itemView: View,
        val tvTitulo: TextView,
        val tvDetalle: TextView,
        val tvMonto: TextView,
        val tvAccion: TextView
    ) : RecyclerView.ViewHolder(itemView)

    private fun crearFila(context: android.content.Context): MovimientoHolder {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            radius = dpToPx(14).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
            isClickable = true
            isFocusable = true
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
        }

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitulo = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        col.addView(tvTitulo)
        val tvDetalle = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
        }
        col.addView(tvDetalle)
        row.addView(col)

        val rightCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        val tvMonto = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        rightCol.addView(tvMonto)
        val tvAccion = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        rightCol.addView(tvAccion)
        row.addView(rightCol)

        card.addView(row)
        return MovimientoHolder(card, tvTitulo, tvDetalle, tvMonto, tvAccion)
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()

    companion object {
        private const val ARG_APERTURA_ID = "apertura_id"

        fun newInstance(aperturaId: Int): MovimientosCajaFragment {
            return MovimientosCajaFragment().apply {
                arguments = Bundle().apply { putInt(ARG_APERTURA_ID, aperturaId) }
            }
        }
    }
}

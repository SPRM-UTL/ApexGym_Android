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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.AsistenciaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.fechaHoyIso
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentHistorialAsistenciasBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class HistorialAsistenciasFragment : BaseFragment<FragmentHistorialAsistenciasBinding>() {

    private var asistencias: List<AsistenciaDto> = emptyList()
    private var paginaActual: Int = 1

    override fun getLayoutRes(): Int = R.layout.fragment_historial_asistencias

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.rvHistorialAsistencias.layoutManager = LinearLayoutManager(requireContext())
        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarHistorial() }

        binding.btnPaginaAnterior.setOnClickListener {
            if (paginaActual > 1) {
                paginaActual--
                renderizarPagina()
            }
        }
        binding.btnPaginaSiguiente.setOnClickListener {
            val totalPaginas = calcularTotalPaginas()
            if (paginaActual < totalPaginas) {
                paginaActual++
                renderizarPagina()
            }
        }

        cargarHistorial()
    }

    private fun cargarHistorial() {
        binding.tvRachaActual.text = "…"
        binding.tvEsteMes.text = "…"
        binding.tvTotalAccesos.text = "…"
        binding.llPaginacion.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerAsistencias()
                val data = response.dataExitosa()
                if (!isAdded) return@launch

                if (data == null) {
                    binding.tvRachaActual.text = "0"
                    binding.tvEsteMes.text = "0"
                    binding.tvTotalAccesos.text = "0"
                    binding.llEmptyHistorial.visibility = View.VISIBLE
                    binding.rvHistorialAsistencias.visibility = View.GONE
                    mostrarMensajeVacio(
                        response.mensajeDeError() ?: "No se pudo cargar el historial"
                    )
                    return@launch
                }

                asistencias = data.sortedByDescending { it.fechaHora.orEmpty() }
                paginaActual = 1

                val hoyIso = fechaHoyIso()
                val mesActual = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

                val accesosHoy = asistencias.count {
                    formatearFecha(it.fechaHora, "yyyy-MM-dd") == hoyIso
                }
                val accesosMes = asistencias.count {
                    formatearFecha(it.fechaHora, "yyyy-MM") == mesActual
                }

                binding.tvRachaActual.text = accesosHoy.toString()
                binding.tvEsteMes.text = accesosMes.toString()
                binding.tvTotalAccesos.text = asistencias.size.toString()

                renderizarPagina()
            } catch (_: Exception) {
                if (isAdded) {
                    binding.tvRachaActual.text = "0"
                    binding.tvEsteMes.text = "0"
                    binding.tvTotalAccesos.text = "0"
                    binding.llEmptyHistorial.visibility = View.VISIBLE
                    binding.rvHistorialAsistencias.visibility = View.GONE
                    mostrarMensajeVacio("No se pudo conectar con el servidor")
                }
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun calcularTotalPaginas(): Int {
        if (asistencias.isEmpty()) return 1
        return ceil(asistencias.size.toDouble() / POR_PAGINA).toInt().coerceAtLeast(1)
    }

    private fun mostrarMensajeVacio(mensaje: String) {
        val grupo = binding.llEmptyHistorial
        for (i in 0 until grupo.childCount) {
            val hijo = grupo.getChildAt(i)
            if (hijo is TextView && hijo.textSize >= 14f) {
                hijo.text = mensaje
                break
            }
        }
    }

    private fun renderizarPagina() {
        if (asistencias.isEmpty()) {
            binding.llEmptyHistorial.visibility = View.VISIBLE
            binding.rvHistorialAsistencias.visibility = View.GONE
            binding.llPaginacion.visibility = View.GONE
            mostrarMensajeVacio("Todavía no hay asistencias registradas")
            return
        }

        val totalPaginas = calcularTotalPaginas()
        if (paginaActual > totalPaginas) paginaActual = totalPaginas
        val inicio = (paginaActual - 1) * POR_PAGINA
        val fin = (inicio + POR_PAGINA).coerceAtMost(asistencias.size)
        val visibles = asistencias.subList(inicio, fin)

        binding.llEmptyHistorial.visibility = View.GONE
        binding.rvHistorialAsistencias.visibility = View.VISIBLE

        if (totalPaginas > 1) {
            binding.llPaginacion.visibility = View.VISIBLE
            binding.tvPaginaActual.text = "Página $paginaActual de $totalPaginas"
            binding.btnPaginaAnterior.isEnabled = paginaActual > 1
            binding.btnPaginaAnterior.alpha = if (paginaActual > 1) 1f else 0.45f
            binding.btnPaginaSiguiente.isEnabled = paginaActual < totalPaginas
            binding.btnPaginaSiguiente.alpha = if (paginaActual < totalPaginas) 1f else 0.45f
        } else {
            binding.llPaginacion.visibility = View.GONE
        }

        binding.rvHistorialAsistencias.adapter = object :
            RecyclerView.Adapter<AsistenciaViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsistenciaViewHolder {
                val card = MaterialCardView(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(10) }
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    radius = dpToPx(16).toFloat()
                    cardElevation = 0f
                    strokeWidth = dpToPx(1)
                    strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
                    isClickable = true
                    isFocusable = true
                }

                val row = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
                }

                val iconBox = FrameLayout(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42))
                    setBackgroundResource(R.drawable.bg_chip_unselected)
                    addView(ImageView(parent.context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            dpToPx(20),
                            dpToPx(20),
                            Gravity.CENTER
                        )
                        setImageResource(R.drawable.ic_calendar_clock)
                        setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                    })
                }
                row.addView(iconBox)

                val col = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply { marginStart = dpToPx(13) }
                }
                val tvNombre = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                col.addView(tvNombre)
                val tvFecha = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                }
                col.addView(tvFecha)
                val tvPlan = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                col.addView(tvPlan)
                row.addView(col)

                val chevron = ImageView(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20))
                    setImageResource(R.drawable.ic_chevron_right)
                    setColorFilter(ContextCompat.getColor(context, R.color.apex_nav_inactive))
                }
                row.addView(chevron)
                card.addView(row)

                return AsistenciaViewHolder(card, tvNombre, tvFecha, tvPlan)
            }

            override fun onBindViewHolder(holder: AsistenciaViewHolder, position: Int) {
                holder.bind(visibles[position])
            }

            override fun getItemCount(): Int = visibles.size
        }
    }

    private inner class AsistenciaViewHolder(
        itemView: View,
        private val tvNombre: TextView,
        private val tvFecha: TextView,
        private val tvPlan: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(asistencia: AsistenciaDto) {
            tvNombre.text = asistencia.cliente?.nombreCompleto
                ?.takeIf { it.isNotBlank() }
                ?: "Socio"
            tvFecha.text = formatearFecha(asistencia.fechaHora)
            tvPlan.text = asistencia.clienteMembresia?.tipoMembresia?.nombre
                ?.let { "Plan: $it" }
                ?: "Sin membresía registrada"

            itemView.setOnClickListener {
                AsistenciaDetalleSheetFragment.newInstance(asistencia)
                    .show(parentFragmentManager, AsistenciaDetalleSheetFragment.TAG)
            }
        }
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()

    companion object {
        private const val POR_PAGINA = 10
    }
}

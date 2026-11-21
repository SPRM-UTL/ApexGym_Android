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
import com.sprm.apexgym.core.network.formatearHora
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentRecepcionDashboardBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecepcionDashboardFragment : BaseFragment<FragmentRecepcionDashboardBinding>() {

    private var asistencias: List<AsistenciaDto> = emptyList()

    override fun getLayoutRes(): Int = R.layout.fragment_recepcion_dashboard

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewWithTag<View>("fl_back_recepcion")?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvTodayDate.text = SimpleDateFormat(
            "EEEE, dd MMMM yyyy",
            Locale("es", "MX")
        ).format(Date())

        val abrirBuscador = View.OnClickListener { abrirBusquedaCliente("") }
        view.findViewWithTag<View>("ll_search_bar_recepcion")?.setOnClickListener(abrirBuscador)
        binding.etSearchClient.setOnClickListener(abrirBuscador)

        binding.cvScanQR.setOnClickListener { abrirEscanerQr() }
        binding.ivScanQR.setOnClickListener { abrirEscanerQr() }
        binding.cvManualAccess.setOnClickListener { abrirAccesoManual() }
        binding.tvVerTodosAccesos.setOnClickListener { abrirHistorialAsistencias() }
        binding.cvAlerts.setOnClickListener { abrirHistorialAsistencias() }

        binding.rvRecentAccess.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        if (view != null) {
            cargarResumenRecepcion()
        }
    }

    private fun abrirBusquedaCliente(termino: String) {
        val sheet = ClienteBusquedaSheetFragment.newInstance(termino)
        sheet.show(parentFragmentManager, ClienteBusquedaSheetFragment.TAG)
    }

    private fun abrirEscanerQr() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, QrScannerFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun abrirAccesoManual() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, AccesoManualFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun abrirHistorialAsistencias() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, HistorialAsistenciasFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun cargarResumenRecepcion() {
        binding.tvEntradasHoy.text = "…"
        binding.tvEntradasDiff.text = "Cargando…"
        binding.tvEnGimnasio.text = "…"
        binding.tvCapacidad.text = "Visitantes del día"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerAsistenciasHoy()
                val data = response.dataExitosa()

                val hoy = fechaHoyIso()
                val visitasHoy = try {
                    RetrofitClient.empleadoService.obtenerVisitas(
                        fechaInicio = hoy,
                        fechaFin = hoy
                    ).dataExitosa().orEmpty()
                } catch (_: Exception) {
                    emptyList()
                }

                if (!isAdded) return@launch

                binding.tvEnGimnasio.text = visitasHoy.size.toString()
                binding.tvCapacidad.text = if (visitasHoy.size == 1) "1 visitante hoy" else "${visitasHoy.size} visitantes hoy"

                if (data == null) {
                    binding.tvEntradasHoy.text = "0"
                    binding.tvEntradasDiff.text = response.mensajeDeError() ?: "Sin accesos"
                    renderizarAsistencias(emptyList())
                    return@launch
                }

                asistencias = data.asistencias
                binding.tvEntradasHoy.text = data.total.toString()
                binding.tvEntradasDiff.text = if (data.total == 1) "1 acceso hoy" else "${data.total} accesos hoy"

                renderizarAsistencias(asistencias.take(MAX_ULTIMOS_ACCESOS))
            } catch (_: Exception) {
                if (isAdded) {
                    binding.tvEntradasHoy.text = "0"
                    binding.tvEntradasDiff.text = "Sin conexión"
                    binding.tvEnGimnasio.text = "0"
                    renderizarAsistencias(emptyList())
                }
            }
        }
    }

    private fun renderizarAsistencias(lista: List<AsistenciaDto>) {
        binding.rvRecentAccess.adapter = object : RecyclerView.Adapter<AsistenciaViewHolder>() {
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
                        setImageResource(R.drawable.ic_user_profile)
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
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                col.addView(tvNombre)

                val tvPlan = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                }
                col.addView(tvPlan)

                val tvHora = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                col.addView(tvHora)
                row.addView(col)

                val chevron = ImageView(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18))
                    setImageResource(R.drawable.ic_chevron_right)
                    setColorFilter(ContextCompat.getColor(context, R.color.apex_nav_inactive))
                }
                row.addView(chevron)

                card.addView(row)
                return AsistenciaViewHolder(card, tvNombre, tvPlan, tvHora)
            }

            override fun onBindViewHolder(holder: AsistenciaViewHolder, position: Int) {
                holder.bind(lista[position])
            }

            override fun getItemCount(): Int = lista.size
        }
    }

    private inner class AsistenciaViewHolder(
        itemView: View,
        private val tvNombre: TextView,
        private val tvPlan: TextView,
        private val tvHora: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(asistencia: AsistenciaDto) {
            val cliente = asistencia.cliente
            tvNombre.text = cliente?.nombreCompleto?.takeIf { it.isNotBlank() } ?: "Socio"
            tvPlan.text = asistencia.clienteMembresia?.tipoMembresia?.nombre
                ?.let { "Plan: $it" }
                ?: "Sin membresía registrada"
            tvHora.text = "Entrada: ${formatearHora(asistencia.fechaHora)}"

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
        private const val MAX_ULTIMOS_ACCESOS = 5
    }
}

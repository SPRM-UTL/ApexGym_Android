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
import android.widget.Toast
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
import com.sprm.apexgym.core.network.formatearHora
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentRecepcionDashboardBinding
import com.sprm.apexgym.feature.notificaciones.NotificacionesListFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecepcionDashboardFragment : BaseFragment<FragmentRecepcionDashboardBinding>() {

    private var asistencias: List<AsistenciaDto> = emptyList()

    override fun getLayoutRes(): Int = R.layout.fragment_recepcion_dashboard

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTodayDate.text = SimpleDateFormat(
            "EEEE, dd MMMM yyyy",
            Locale("es", "MX")
        ).format(Date())

        binding.flNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, NotificacionesListFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cvScanQR.setOnClickListener { mostrarQrNoDisponible() }
        binding.ivScanQR.setOnClickListener { mostrarQrNoDisponible() }

        binding.cvManualAccess.setOnClickListener {
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

        binding.tvVerTodosAccesos.setOnClickListener {
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

        binding.cvAlerts.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Las alertas de membresías por vencer no tienen endpoint disponible.",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.etSearchClient.setOnEditorActionListener { _, _, _ ->
            abrirBusquedaCliente(binding.etSearchClient.text?.toString()?.trim().orEmpty())
            true
        }

        binding.rvRecentAccess.layoutManager = LinearLayoutManager(requireContext())
        cargarAsistencias()
    }

    private fun abrirBusquedaCliente(termino: String) {
        val sheet = ClienteBusquedaSheetFragment.newInstance(termino)
        sheet.show(parentFragmentManager, ClienteBusquedaSheetFragment.TAG)
    }

    private fun mostrarQrNoDisponible() {
        Toast.makeText(
            requireContext(),
            "El escaneo QR no está disponible: la API no expone el payload ni el endpoint de lectura.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun cargarAsistencias() {
        binding.tvEntradasHoy.text = "…"
        binding.tvEnGimnasio.text = "—"
        binding.tvCapacidad.text = "Dato no disponible"
        binding.tvAlertas.text = "—"
        binding.tvAlertasLabel.text = "Sin endpoint de alertas"
        binding.tvEntradasDiff.text = "Asistencias registradas hoy"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerAsistenciasHoy()
                val data = response.dataExitosa()

                if (data == null) {
                    binding.tvEntradasHoy.text = "—"
                    binding.tvEntradasDiff.text =
                        response.mensajeDeError() ?: "No se pudieron cargar las asistencias"
                    renderizarAsistencias(emptyList())
                    return@launch
                }

                asistencias = data.asistencias
                binding.tvEntradasHoy.text = data.total.toString()
                binding.tvEntradasDiff.text = if (data.total == 1) {
                    "1 acceso registrado hoy"
                } else {
                    "${data.total} accesos registrados hoy"
                }
                renderizarAsistencias(asistencias.take(6))
            } catch (e: Exception) {
                binding.tvEntradasHoy.text = "—"
                binding.tvEntradasDiff.text = "Sin conexión con el servidor"
                renderizarAsistencias(emptyList())
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
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
                    radius = dpToPx(18).toFloat()
                    cardElevation = dpToPx(2).toFloat()
                    strokeWidth = 0
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
                    setBackgroundResource(R.drawable.bg_circle_blue_light)
                    addView(ImageView(parent.context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            dpToPx(22),
                            dpToPx(22),
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
                    setTextColor(ContextCompat.getColor(context, R.color.white))
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
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                col.addView(tvHora)
                row.addView(col)

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
            tvNombre.text = cliente?.nombreCompleto?.takeIf { it.isNotBlank() } ?: "Cliente"
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
}

package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.ClienteMembresiaDto
import com.sprm.apexgym.core.network.client.MiMembresiaResponseDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Locale

class MembresiaDetailBottomSheetFragment : BottomSheetDialogFragment() {

    private val gson = Gson()
    private var datosActuales: MiMembresiaResponseDto? = null
    private var consultando = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_membresia_detail_sheet, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val jsonInicial = arguments?.getString(ARG_MI_MEMBRESIA_JSON)
        if (!jsonInicial.isNullOrBlank()) {
            datosActuales = try {
                gson.fromJson(jsonInicial, MiMembresiaResponseDto::class.java)
            } catch (_: Exception) {
                null
            }
        }

        view.findViewById<View>(R.id.btnCerrarMembresiaDetail)?.setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btnRefrescarMembresiaDetail)?.setOnClickListener {
            recargarDesdeBackend(view, mostrarFeedbackManual = true)
        }

        view.findViewById<View>(R.id.btnReintentarMembresiaDetail)?.setOnClickListener {
            recargarDesdeBackend(view, mostrarFeedbackManual = true)
        }

        if (datosActuales != null) {
            renderizarDetalle(view, datosActuales)
            recargarDesdeBackend(view, mostrarFeedbackManual = false)
        } else {
            mostrarBannerCarga(
                rootView = view,
                mensaje = "Consultando vigencia de tu membresía...",
                mostrarReintentar = false
            )
            renderizarDetalle(view, null)
            recargarDesdeBackend(view, mostrarFeedbackManual = false)
        }
    }

    private fun recargarDesdeBackend(rootView: View, mostrarFeedbackManual: Boolean) {
        if (consultando) return
        consultando = true

        val btnRefresh = rootView.findViewById<View>(R.id.btnRefrescarMembresiaDetail)
        btnRefresh?.isEnabled = false
        btnRefresh?.alpha = 0.6f

        if (datosActuales == null) {
            mostrarBannerCarga(
                rootView = rootView,
                mensaje = "Consultando vigencia de tu membresía...",
                mostrarReintentar = false
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.obtenerMiMembresia()
                if (!isAdded) return@launch

                if (response.isSuccessful && response.body()?.responseFlag == 0) {
                    val nuevoDato = response.body()?.data
                    datosActuales = nuevoDato
                    ocultarBannerCarga(rootView)
                    renderizarDetalle(rootView, datosActuales)

                    if (nuevoDato != null) {
                        parentFragmentManager.setFragmentResult(
                            RESULT_KEY_DATOS_ACTUALIZADOS,
                            Bundle().apply {
                                putString(EXTRA_MEMBRESIA_JSON, gson.toJson(nuevoDato))
                            }
                        )
                    }

                    if (mostrarFeedbackManual) {
                        Toast.makeText(
                            requireContext(),
                            "Vigencia de membresía actualizada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else if (datosActuales == null) {
                    mostrarBannerCarga(
                        rootView = rootView,
                        mensaje = "No se pudo consultar tu vigencia de membresía. Verifica tu conexión.",
                        mostrarReintentar = true
                    )
                } else if (mostrarFeedbackManual) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo actualizar la información en este momento",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                if (!isAdded) return@launch
                if (datosActuales == null) {
                    mostrarBannerCarga(
                        rootView = rootView,
                        mensaje = "No se pudo consultar tu vigencia de membresía. Verifica tu conexión.",
                        mostrarReintentar = true
                    )
                } else if (mostrarFeedbackManual) {
                    Toast.makeText(
                        requireContext(),
                        "Sin conexión para actualizar tu membresía",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                consultando = false
                if (isAdded) {
                    btnRefresh?.isEnabled = true
                    btnRefresh?.alpha = 1f
                }
            }
        }
    }

    private fun mostrarBannerCarga(
        rootView: View,
        mensaje: String,
        mostrarReintentar: Boolean
    ) {
        val llEstadoCarga = rootView.findViewById<View>(R.id.llDetailMembresiaEstadoCarga)
        val tvEstadoTexto = rootView.findViewById<TextView>(R.id.tvDetailMembresiaEstadoCargaTexto)
        val btnReintentar = rootView.findViewById<View>(R.id.btnReintentarMembresiaDetail)

        llEstadoCarga?.visibility = View.VISIBLE
        tvEstadoTexto?.text = mensaje
        btnReintentar?.visibility = if (mostrarReintentar) View.VISIBLE else View.GONE
    }

    private fun ocultarBannerCarga(rootView: View) {
        rootView.findViewById<View>(R.id.llDetailMembresiaEstadoCarga)?.visibility = View.GONE
    }

    private fun renderizarDetalle(rootView: View, data: MiMembresiaResponseDto?) {
        val tvEstadoBadge = rootView.findViewById<TextView>(R.id.tvDetailMembresiaEstadoBadge)
        val tvPlanNombre = rootView.findViewById<TextView>(R.id.tvDetailMembresiaPlanNombre)
        val tvDiasNumero = rootView.findViewById<TextView>(R.id.tvDetailMembresiaDiasNumero)
        val tvDiasEtiqueta = rootView.findViewById<TextView>(R.id.tvDetailMembresiaDiasEtiqueta)
        val pbProgreso = rootView.findViewById<ProgressBar>(R.id.pbDetailMembresiaProgreso)
        val tvFechaInicio = rootView.findViewById<TextView>(R.id.tvDetailMembresiaFechaInicio)
        val tvFechaFin = rootView.findViewById<TextView>(R.id.tvDetailMembresiaFechaFin)
        val tvDuracion = rootView.findViewById<TextView>(R.id.tvDetailMembresiaDuracion)

        val tvPrecio = rootView.findViewById<TextView>(R.id.tvDetailMembresiaPrecio)
        val tvRenovacionValor = rootView.findViewById<TextView>(R.id.tvDetailMembresiaRenovacionValor)
        val tvDescripcion = rootView.findViewById<TextView>(R.id.tvDetailMembresiaDescripcion)
        val tvEstadoDetalle = rootView.findViewById<TextView>(R.id.tvDetailMembresiaEstadoDetalle)

        val llTitularCard = rootView.findViewById<View>(R.id.llDetailMembresiaTitularCard)
        val tvTitularNombre = rootView.findViewById<TextView>(R.id.tvDetailMembresiaTitularNombre)
        val tvTitularContacto = rootView.findViewById<TextView>(R.id.tvDetailMembresiaTitularContacto)

        val llHistorialSection = rootView.findViewById<View>(R.id.llDetailMembresiaHistorialSection)
        val llHistorialContainer = rootView.findViewById<LinearLayout>(R.id.llDetailMembresiaHistorialContainer)

        val btnAccionPrincipal = rootView.findViewById<MaterialButton>(R.id.btnDetailMembresiaAccionPrincipal)
        val btnVerPlanes = rootView.findViewById<MaterialButton>(R.id.btnDetailMembresiaVerPlanes)

        val activa = data?.membresiaActiva
        if (activa != null) {
            val estadoNombre = (activa.estadoMembresia?.nombre ?: "Vigente").trim()
            tvEstadoBadge?.text = "MEMBRESÍA ${estadoNombre.uppercase(Locale.getDefault())}"
            tvPlanNombre?.text = activa.tipoMembresia?.nombre ?: "Plan Activo ApexGym"

            val progreso = MembresiaVigenciaHelper.calcularProgreso(
                fechaInicioIso = activa.fechaInicio,
                fechaFinIso = activa.fechaFin,
                duracionDiasPlan = activa.tipoMembresia?.duracionDias
            )
            val diasRestantes = MembresiaVigenciaHelper.calcularDiasRestantes(activa.fechaFin)

            if (progreso != null) {
                tvDiasNumero?.text = progreso.diasRestantes.toString()
                tvDiasEtiqueta?.text = when (progreso.diasRestantes) {
                    1L -> "día restante de vigencia"
                    0L -> "días restantes • Vence hoy"
                    else -> "días restantes de vigencia"
                }
                pbProgreso?.progress = progreso.porcentajeCompletado
                tvDuracion?.text = "${progreso.diasTotales} días"
            } else {
                val duracionPlan = activa.tipoMembresia?.duracionDias ?: 30
                tvDiasNumero?.text = (diasRestantes?.coerceAtLeast(0L) ?: duracionPlan.toLong()).toString()
                tvDiasEtiqueta?.text = "días de duración del plan"
                pbProgreso?.progress = 0
                tvDuracion?.text = "$duracionPlan días"
            }

            tvFechaInicio?.text = MembresiaVigenciaHelper.formatearFechaCorta(activa.fechaInicio)
            tvFechaFin?.text = MembresiaVigenciaHelper.formatearFechaCorta(activa.fechaFin)

            tvPrecio?.text = activa.tipoMembresia?.let {
                MembresiaVigenciaHelper.formatearPrecioMxn(it.precio)
            } ?: "—"

            tvRenovacionValor?.text = if (activa.renovacionAutomatica) "Automática" else "Manual"

            tvDescripcion?.text = activa.tipoMembresia?.descripcion?.takeIf { it.isNotBlank() }
                ?: "Acceso completo a las instalaciones y áreas de entrenamiento de ApexGym durante el periodo vigente."

            val descripcionEstado = activa.estadoMembresia?.descripcion?.takeIf { it.isNotBlank() }
            val alertaPorVencer = diasRestantes != null &&
                diasRestantes in 0L..MembresiaVigenciaHelper.DIAS_ALERTA_POR_VENCER

            tvEstadoDetalle?.text = buildString {
                append(estadoNombre)
                if (!descripcionEstado.isNullOrBlank()) {
                    append(" • ")
                    append(descripcionEstado)
                }
                if (alertaPorVencer) {
                    append(". Tu membresía está próxima a vencer; puedes renovarla en recepción.")
                } else if (activa.renovacionAutomatica) {
                    append(". Se renovará automáticamente al concluir el periodo actual.")
                }
            }

            btnAccionPrincipal?.text = "Mostrar pase QR de acceso"
            btnAccionPrincipal?.setIconResource(R.drawable.ic_qr_scanner)
            btnAccionPrincipal?.setOnClickListener {
                emitirAccionYCerrar(ACCION_ABRIR_QR)
            }

            btnVerPlanes?.visibility = View.VISIBLE
            btnVerPlanes?.setOnClickListener {
                emitirAccionYCerrar(ACCION_VER_PLANES)
            }
        } else {
            val ultima = data?.historial?.firstOrNull()
            tvEstadoBadge?.text = if (data == null && consultando) {
                "CONSULTANDO ESTADO"
            } else {
                "SIN MEMBRESÍA ACTIVA"
            }
            tvPlanNombre?.text = when {
                data == null && consultando -> "Consultando membresía..."
                ultima?.tipoMembresia?.nombre != null -> "Último plan: ${ultima.tipoMembresia.nombre}"
                else -> "Sin membresía activa"
            }

            tvDiasNumero?.text = "0"
            tvDiasEtiqueta?.text = "días vigentes"
            pbProgreso?.progress = 0

            if (ultima != null) {
                tvFechaInicio?.text = MembresiaVigenciaHelper.formatearFechaCorta(ultima.fechaInicio)
                tvFechaFin?.text = MembresiaVigenciaHelper.formatearFechaCorta(ultima.fechaFin)
                val duracionUltima = ultima.tipoMembresia?.duracionDias
                tvDuracion?.text = if (duracionUltima != null) "$duracionUltima días" else "—"
                tvPrecio?.text = ultima.tipoMembresia?.let {
                    MembresiaVigenciaHelper.formatearPrecioMxn(it.precio)
                } ?: "—"
            } else {
                tvFechaInicio?.text = "Sin registro"
                tvFechaFin?.text = "Sin vigencia"
                tvDuracion?.text = "—"
                tvPrecio?.text = "—"
            }

            tvRenovacionValor?.text = "Sin renovación"
            tvDescripcion?.text = ultima?.tipoMembresia?.descripcion?.takeIf { it.isNotBlank() }
                ?: "Aún no cuentas con una membresía activa. Explora los planes disponibles para activar tu acceso a ApexGym."

            tvEstadoDetalle?.text = if (ultima != null) {
                val ultimoEstado = ultima.estadoMembresia?.nombre ?: "Vencida"
                "Último estado: $ultimoEstado • Puedes renovar o contratar un nuevo plan en recepción."
            } else {
                "Sin suscripción vigente • Elige un plan para comenzar a entrenar en ApexGym."
            }

            btnAccionPrincipal?.text = "Ver planes disponibles"
            btnAccionPrincipal?.setIconResource(R.drawable.ic_membership_card)
            btnAccionPrincipal?.setOnClickListener {
                emitirAccionYCerrar(ACCION_VER_PLANES)
            }
            btnVerPlanes?.visibility = View.GONE
        }

        val cliente = data?.cliente
        val nombreCompleto = MembresiaVigenciaHelper.construirNombreCompleto(cliente)
        val datosContacto = listOfNotNull(
            cliente?.correo?.trim()?.takeIf { it.isNotEmpty() },
            cliente?.telefono?.trim()?.takeIf { it.isNotEmpty() }
        ).joinToString(" • ")

        if (!nombreCompleto.isNullOrBlank() || datosContacto.isNotBlank()) {
            llTitularCard?.visibility = View.VISIBLE
            tvTitularNombre?.text = nombreCompleto ?: "Socio ApexGym"
            if (datosContacto.isNotBlank()) {
                tvTitularContacto?.visibility = View.VISIBLE
                tvTitularContacto?.text = datosContacto
            } else {
                tvTitularContacto?.visibility = View.GONE
            }
        } else {
            llTitularCard?.visibility = View.GONE
        }

        val historial = data?.historial.orEmpty()
        llHistorialContainer?.removeAllViews()
        if (historial.isEmpty()) {
            llHistorialSection?.visibility = View.GONE
        } else {
            llHistorialSection?.visibility = View.VISIBLE
            historial.take(3).forEach { item ->
                llHistorialContainer?.addView(crearFilaHistorialMembresia(item))
            }
        }
    }

    private fun crearFilaHistorialMembresia(item: ClienteMembresiaDto): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
            setBackgroundResource(R.drawable.bg_profile_option_card)
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
        }

        val iconCircle = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
            setBackgroundResource(R.drawable.bg_icon_circle_orange_outline)

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(16), dpToPx(16), Gravity.CENTER)
                setImageResource(R.drawable.ic_membership_card)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_orange))
                )
            }
            addView(icon)
        }
        row.addView(iconCircle)

        val infoCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(10)
                marginEnd = dpToPx(8)
            }
        }

        val tvPlan = TextView(context).apply {
            text = item.tipoMembresia?.nombre ?: "Membresía ApexGym"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, Typeface.BOLD)
        }
        infoCol.addView(tvPlan)

        val inicioStr = MembresiaVigenciaHelper.formatearFechaCorta(item.fechaInicio)
        val finStr = MembresiaVigenciaHelper.formatearFechaCorta(item.fechaFin)
        val tvFechas = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            text = "$inicioStr al $finStr"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
        }
        infoCol.addView(tvFechas)
        row.addView(infoCol)

        val estadoStr = (item.estadoMembresia?.nombre ?: "Registrada").uppercase(Locale.getDefault())
        val tvEstado = TextView(context).apply {
            text = estadoStr
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(
                if (estadoStr.contains("VIGENTE")) {
                    ContextCompat.getColor(context, R.color.apex_orange)
                } else {
                    ContextCompat.getColor(context, R.color.apex_text_secondary)
                }
            )
        }
        row.addView(tvEstado)

        return row
    }

    private fun emitirAccionYCerrar(accion: String) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY_ACCION_MEMBRESIA,
            Bundle().apply {
                putString(EXTRA_ACCION, accion)
            }
        )
        dismiss()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val TAG = "MembresiaDetailBottomSheet"
        const val RESULT_KEY_ACCION_MEMBRESIA = "result_membresia_detail_accion"
        const val EXTRA_ACCION = "extra_accion"
        const val ACCION_ABRIR_QR = "ABRIR_QR"
        const val ACCION_VER_PLANES = "VER_PLANES"
        const val RESULT_KEY_DATOS_ACTUALIZADOS = "result_membresia_detail_actualizada"
        const val EXTRA_MEMBRESIA_JSON = "extra_membresia_json"
        private const val ARG_MI_MEMBRESIA_JSON = "arg_mi_membresia_json"

        fun newInstance(data: MiMembresiaResponseDto?): MembresiaDetailBottomSheetFragment {
            return MembresiaDetailBottomSheetFragment().apply {
                if (data != null) {
                    arguments = Bundle().apply {
                        putString(ARG_MI_MEMBRESIA_JSON, Gson().toJson(data))
                    }
                }
            }
        }
    }
}

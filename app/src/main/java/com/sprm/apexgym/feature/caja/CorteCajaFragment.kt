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
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.AperturaCajaDto
import com.sprm.apexgym.core.network.api.CrearCorteRequest
import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.api.ResumenAperturaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearMoneda
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.core.ui.InputFilters
import com.sprm.apexgym.databinding.FragmentCorteCajaBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CorteCajaFragment : BaseFragment<FragmentCorteCajaBinding>() {

    private var aperturaId: Int = 0
    private var apertura: AperturaCajaDto? = null
    private var resumen: ResumenAperturaDto? = null
    private var efectivoEsperado: Double = 0.0
    private var empleadosResponsables: List<EmpleadoDto> = emptyList()

    private val conteoDenominaciones = linkedMapOf<String, Int>().apply {
        DesgloseEfectivo.DENOMINACIONES.forEach { put(it.clave, 0) }
    }
    private lateinit var denominacionAdapter: DenominacionEfectivoAdapter
    private var efectivoEditadoManualmente = false
    private var actualizandoDesdeDesglose = false

    override fun getLayoutRes(): Int = R.layout.fragment_corte_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aperturaId = arguments?.getInt(ARG_APERTURA_ID, 0) ?: 0

        binding.rvCorteVentasMetodo.layoutManager = LinearLayoutManager(requireContext())
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        val ahora = Date()
        binding.tvCorteFecha.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora)
        binding.tvCorteHora.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora)
        binding.tvCorteResponsable.text = SessionManager.obtenerNombre(requireContext()) ?: "—"
        binding.tvCorteEstado.text = "EN PROCESO"

        denominacionAdapter = DenominacionEfectivoAdapter(
            items = DesgloseEfectivo.DENOMINACIONES,
            conteo = conteoDenominaciones,
            onCambioCantidad = { actualizarResumenDesglose(sincronizarContado = true) }
        )
        binding.rvCorteDenominaciones.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            adapter = denominacionAdapter
        }

        binding.tvToggleDesgloseCorte.setOnClickListener {
            val visible = binding.llCorteDesgloseContainer.visibility == View.VISIBLE
            binding.llCorteDesgloseContainer.visibility = if (visible) View.GONE else View.VISIBLE
            binding.tvToggleDesgloseCorte.text = if (visible) {
                "Contar por denominaciones (opcional)"
            } else {
                "Ocultar conteo por denominaciones"
            }
        }

        binding.tvCorteLimpiarDesglose.setOnClickListener {
            conteoDenominaciones.keys.forEach { conteoDenominaciones[it] = 0 }
            efectivoEditadoManualmente = false
            denominacionAdapter.notifyDataSetChanged()
            actualizarResumenDesglose(sincronizarContado = true)
        }

        binding.etCorteEfectivoContado.filters = InputFilters.decimalAmount(maxIntegers = 7, maxDecimals = 2)
        binding.etCorteObservaciones.filters = InputFilters.plainText(500, allowNewLines = true)

        binding.etCorteEfectivoContado.doAfterTextChanged { editable ->
            if (!actualizandoDesdeDesglose) {
                efectivoEditadoManualmente = !editable.isNullOrBlank()
            }
            actualizarDiferencia()
        }

        binding.btnFinalizarCorte.setOnClickListener { confirmarCorte() }

        cargarCorte()
    }

    private fun cargarCorte() {
        binding.tvCorteCajaNombre.text = "Cargando caja…"
        binding.tvCorteVentasTotales.text = "…"
        binding.tvCorteNumTransacciones.text = "…"
        binding.tvCorteEfectivoCaja.text = "…"
        binding.tvCorteDiferencia.text = "Diferencia: —"
        binding.cvCorteDiferencias.visibility = View.GONE
        binding.btnFinalizarCorte.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ctx = requireContext()
                val aperturasDef = async { RetrofitClient.empleadoService.obtenerAperturas() }
                val empleadosDef = async {
                    EmpleadoSelectorHelper.prepararSelector(
                        ctx,
                        binding.llResponsableCorte,
                        binding.spResponsableCorte
                    )
                }

                val aperturas = aperturasDef.await().dataExitosa().orEmpty()
                val empRes = empleadosDef.await()
                if (!isAdded || view == null) return@launch

                empleadosResponsables = empRes.empleados

                val activa = if (aperturaId > 0) {
                    aperturas.firstOrNull { it.id == aperturaId }
                } else {
                    aperturas.filter { CajaValidacionLogic.esAperturaAbierta(it.estado) }.maxByOrNull { it.id }
                }

                if (activa == null) {
                    binding.tvCorteCajaNombre.text = "No hay caja abierta"
                    binding.tvCorteVentasTotales.text = "—"
                    binding.tvCorteNumTransacciones.text = "—"
                    binding.tvCorteEfectivoCaja.text = "—"
                    Toast.makeText(requireContext(), "No hay una caja abierta", Toast.LENGTH_LONG).show()
                    return@launch
                }

                apertura = activa
                aperturaId = activa.id
                binding.tvCorteCajaNombre.text = activa.caja?.nombre ?: "Caja #${activa.id}"
                binding.tvCorteEstado.text = activa.estado?.uppercase() ?: "ABIERTA"

                if (!CajaValidacionLogic.esAperturaAbierta(activa.estado)) {
                    binding.tvCorteVentasTotales.text = "—"
                    binding.tvCorteNumTransacciones.text = "Apertura cerrada"
                    binding.tvCorteEfectivoCaja.text = "—"
                    binding.tvCorteDiferencia.text = "Esta apertura ya fue cerrada"
                    Toast.makeText(requireContext(), "Esta apertura de caja ya está cerrada", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val resumenResp = RetrofitClient.empleadoService.obtenerResumenApertura(activa.id)
                if (!isAdded || view == null) return@launch
                val resumenCargado = resumenResp.dataExitosa()

                if (resumenCargado == null) {
                    binding.tvCorteVentasTotales.text = "—"
                    binding.tvCorteNumTransacciones.text = "Error de resumen"
                    binding.tvCorteEfectivoCaja.text = "—"
                    binding.tvCorteDiferencia.text =
                        resumenResp.mensajeDeError() ?: "No se pudo cargar el arqueo"
                    return@launch
                }

                resumen = resumenCargado
                efectivoEsperado = resumenCargado.saldoDisponible

                binding.tvCorteVentasTotales.text = (resumenCargado.totales?.totalVentas ?: 0.0)
                    .formatearMonedaSinSufijo()
                binding.tvCorteNumTransacciones.text =
                    "${resumenCargado.totales?.transaccionesVentas ?: 0} transacciones"
                binding.tvCorteEfectivoCaja.text = efectivoEsperado.formatearMonedaSinSufijo()
                binding.tvCorteDiferencia.text = "Esperado: ${efectivoEsperado.formatearMonedaSinSufijo()}"
                binding.tvCorteArqueoInfo.text =
                    "Fondo: ${resumenCargado.montoInicial.formatearMonedaSinSufijo()} • " +
                        "Entradas: +${resumenCargado.totalEntradas.formatearMonedaSinSufijo()} • " +
                        "Salidas: -${resumenCargado.totalSalidas.formatearMonedaSinSufijo()} • " +
                        "Ventas ef.: ${resumenCargado.totalVentasEfectivo.formatearMonedaSinSufijo()}"
                binding.cvCorteDiferencias.visibility = View.GONE
                binding.btnFinalizarCorte.isEnabled = true

                renderizarVentasMetodo(resumenCargado)
            } catch (_: Exception) {
                if (!isAdded || view == null) return@launch
                binding.tvCorteCajaNombre.text = "Sin conexión"
                binding.tvCorteVentasTotales.text = "—"
                binding.tvCorteNumTransacciones.text = "—"
                binding.tvCorteEfectivoCaja.text = "—"
                binding.tvCorteDiferencia.text = "No se pudo conectar con el servidor"
                Toast.makeText(requireContext(), "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun actualizarResumenDesglose(sincronizarContado: Boolean) {
        val calc = DesgloseEfectivo.calcularResumen(conteoDenominaciones)
        binding.tvCorteResumenDesglose.text =
            "Billetes: ${calc.totalBilletes.formatearMonedaSinSufijo()} • Monedas: ${calc.totalMonedas.formatearMonedaSinSufijo()}"
        binding.tvCorteLimpiarDesglose.visibility =
            if (calc.piezasTotales > 0) View.VISIBLE else View.GONE

        if (sincronizarContado && !efectivoEditadoManualmente) {
            actualizandoDesdeDesglose = true
            try {
                val texto = if (calc.piezasTotales > 0) {
                    String.format(Locale.US, "%.2f", calc.total)
                } else {
                    ""
                }
                binding.etCorteEfectivoContado.setText(texto)
                binding.etCorteEfectivoContado.setSelection(texto.length)
            } finally {
                actualizandoDesdeDesglose = false
            }
        }
        actualizarDiferencia()
    }

    private fun renderizarVentasMetodo(resumenCargado: ResumenAperturaDto?) {
        val metodos = resumenCargado?.totales?.ventasPorMetodoPago.orEmpty()
        binding.rvCorteVentasMetodo.adapter = object : RecyclerView.Adapter<SimpleRowHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleRowHolder {
                return crearFila(parent.context)
            }

            override fun onBindViewHolder(holder: SimpleRowHolder, position: Int) {
                if (metodos.isEmpty()) {
                    holder.tvTitulo.text = "Sin ventas registradas en esta apertura"
                    holder.tvDetalle.text = ""
                    holder.tvMonto.text = ""
                } else {
                    val metodo = metodos[position]
                    holder.tvTitulo.text = metodo.nombre ?: "Método ${metodo.metodoPagoId}"
                    holder.tvDetalle.text = "${metodo.transacciones} transacciones"
                    holder.tvMonto.text = metodo.total.formatearMonedaSinSufijo()
                }
            }

            override fun getItemCount(): Int = metodos.size.coerceAtLeast(1)
        }
    }

    private fun actualizarDiferencia() {
        val texto = binding.etCorteEfectivoContado.text?.toString()?.trim()?.replace(',', '.')
        val contado = texto?.toDoubleOrNull()
        binding.tvCorteEfectivoCaja.text = efectivoEsperado.formatearMonedaSinSufijo()

        if (contado == null) {
            binding.tvCorteDiferencia.text = "Esperado: ${efectivoEsperado.formatearMonedaSinSufijo()}"
            binding.cvCorteDiferencias.visibility = View.GONE
            binding.tilCorteObservaciones.hint = "Observaciones del corte (opcional)"
            return
        }

        val diferencia = contado - efectivoEsperado
        binding.tvCorteDiferencia.text = "Diferencia: ${diferencia.formatearMonedaSinSufijo()}"
        if (CajaValidacionLogic.esCorteCuadrado(diferencia)) {
            binding.cvCorteDiferencias.visibility = View.GONE
            binding.tilCorteObservaciones.hint = "Observaciones del corte (opcional)"
        } else {
            binding.cvCorteDiferencias.visibility = View.VISIBLE
            binding.tvCorteSobranteFaltante.text = if (diferencia > 0) {
                "Sobrante ${diferencia.formatearMonedaSinSufijo()}"
            } else {
                "Faltante ${(-diferencia).formatearMonedaSinSufijo()}"
            }
            binding.tilCorteObservaciones.hint = "Observaciones del corte (obligatorio por diferencia)"
        }
    }

    private fun confirmarCorte() {
        val actual = apertura
        val resumenActual = resumen
        if (actual == null || resumenActual == null || !CajaValidacionLogic.esAperturaAbierta(actual.estado)) {
            Toast.makeText(requireContext(), "No hay una apertura activa válida para cerrar", Toast.LENGTH_SHORT).show()
            return
        }

        val pendientes = resumenActual.movimientosPendientes
        if (pendientes > 0) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Movimientos pendientes")
                .setMessage(
                    "Hay $pendientes movimiento(s) pendiente(s) de validar. " +
                        "El corte requiere que todos estén validados."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Ver movimientos") { _, _ ->
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                        .replace(R.id.fragment_container, MovimientosCajaFragment.newInstance(actual.id))
                        .addToBackStack(null)
                        .commit()
                }
                .show()
            return
        }

        val empleadoId = EmpleadoSelectorHelper.resolverEmpleadoId(
            requireContext(),
            empleadosResponsables,
            binding.spResponsableCorte
        )
        if (empleadoId == null) {
            Toast.makeText(
                requireContext(),
                "Selecciona un empleado responsable para realizar el corte.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val calc = DesgloseEfectivo.calcularResumen(conteoDenominaciones)
        val desgloseFinJson = DesgloseEfectivo.serializarJson(conteoDenominaciones)
        val contado = binding.etCorteEfectivoContado.text
            ?.toString()
            ?.trim()
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?: if (calc.piezasTotales > 0) calc.total else null

        if (contado == null || contado < 0) {
            Toast.makeText(requireContext(), "Ingresa el efectivo contado", Toast.LENGTH_SHORT).show()
            return
        }

        val observaciones = binding.etCorteObservaciones.text
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val diferencia = contado - efectivoEsperado
        if (CajaValidacionLogic.requiereJustificacionCorte(diferencia) && observaciones.isNullOrBlank()) {
            binding.tilCorteObservaciones.error = "Explica el motivo de la diferencia en arqueo"
            Toast.makeText(
                requireContext(),
                "Ingresa observaciones para justificar la diferencia",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        binding.tilCorteObservaciones.error = null

        if ((observaciones?.length ?: 0) > CajaValidacionLogic.MAX_CARACTERES_OBSERVACIONES) {
            binding.tilCorteObservaciones.error =
                "Máximo ${CajaValidacionLogic.MAX_CARACTERES_OBSERVACIONES} caracteres"
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar corte de caja")
            .setMessage(
                "Esta acción cerrará el turno actual de ${actual.caja?.nombre ?: "la caja"}.\n\n" +
                    "Efectivo esperado: ${efectivoEsperado.formatearMoneda()}\n" +
                    "Efectivo contado: ${contado.formatearMoneda()}\n" +
                    "Diferencia: ${diferencia.formatearMoneda()}"
            )
            .setNegativeButton("Revisar", null)
            .setPositiveButton("Confirmar corte") { _, _ ->
                ejecutarCorte(actual, empleadoId, contado, observaciones, desgloseFinJson)
            }
            .show()
    }

    private fun ejecutarCorte(
        actual: AperturaCajaDto,
        empleadoId: Int,
        contado: Double,
        observaciones: String?,
        desgloseFinJson: String?
    ) {
        binding.btnFinalizarCorte.isEnabled = false
        binding.btnFinalizarCorte.text = "Procesando..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearCorte(
                    CrearCorteRequest(
                        aperturaCajaId = actual.id,
                        empleadoId = empleadoId,
                        efectivoContado = contado,
                        observaciones = observaciones,
                        desgloseInicio = actual.desgloseInicio,
                        desgloseFin = desgloseFinJson
                    )
                )
                if (!isAdded || view == null) return@launch

                val corte = response.dataExitosa()
                if (corte == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo registrar el corte",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Corte registrado")
                        .setMessage(
                            "La caja se cerró correctamente.\n\n" +
                                "Efectivo contado: ${corte.efectivoContado.formatearMonedaSinSufijo()}\n" +
                                "Ventas: ${corte.totalVentas.formatearMonedaSinSufijo()}\n" +
                                "Diferencia: ${corte.diferencia.formatearMonedaSinSufijo()}\n" +
                                "Fecha: ${formatearFecha(corte.fechaCorte, "dd/MM/yyyy HH:mm")}"
                        )
                        .setCancelable(false)
                        .setPositiveButton("Aceptar") { _, _ ->
                            parentFragmentManager.popBackStack()
                        }
                        .show()
                }
            } catch (_: Exception) {
                if (isAdded && view != null) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                if (isAdded && view != null) {
                    binding.btnFinalizarCorte.isEnabled = true
                    binding.btnFinalizarCorte.text = "Finalizar Corte"
                }
            }
        }
    }

    private class SimpleRowHolder(
        itemView: View,
        val tvTitulo: TextView,
        val tvDetalle: TextView,
        val tvMonto: TextView
    ) : RecyclerView.ViewHolder(itemView)

    private fun crearFila(context: android.content.Context): SimpleRowHolder {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_bg_screen))
            radius = dpToPx(14).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
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

        val tvMonto = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        row.addView(tvMonto)
        card.addView(row)
        return SimpleRowHolder(card, tvTitulo, tvDetalle, tvMonto)
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()

    companion object {
        private const val ARG_APERTURA_ID = "apertura_id"

        fun newInstance(aperturaId: Int): CorteCajaFragment {
            return CorteCajaFragment().apply {
                arguments = Bundle().apply { putInt(ARG_APERTURA_ID, aperturaId) }
            }
        }
    }
}

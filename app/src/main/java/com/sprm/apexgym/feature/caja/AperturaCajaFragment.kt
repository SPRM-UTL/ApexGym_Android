package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.CajaDto
import com.sprm.apexgym.core.network.api.CrearAperturaRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearMoneda
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentAperturaCajaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AperturaCajaFragment : BaseFragment<FragmentAperturaCajaBinding>() {

    private var cajas: List<CajaDto> = emptyList()
    private val conteoDenominaciones = linkedMapOf<String, Int>().apply {
        DesgloseEfectivo.DENOMINACIONES.forEach { put(it.clave, 0) }
    }
    private lateinit var denominacionAdapter: DenominacionAdapter
    private var fondoEditadoManualmente = false
    private var actualizandoFondoDesdeDesglose = false

    override fun getLayoutRes(): Int = R.layout.fragment_apertura_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        denominacionAdapter = DenominacionAdapter(
            items = DesgloseEfectivo.DENOMINACIONES,
            conteo = conteoDenominaciones,
            onCambioCantidad = { actualizarResumenDesglose(sincronizarFondo = true) }
        )

        binding.rvDenominaciones.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            visibility = View.VISIBLE
            adapter = denominacionAdapter
        }

        binding.tvLimpiarDesglose.setOnClickListener { limpiarDesglose() }

        binding.etFondoInicial.doAfterTextChanged { editable ->
            if (!actualizandoFondoDesdeDesglose) {
                fondoEditadoManualmente = !editable.isNullOrBlank()
                actualizarResumenDesglose(sincronizarFondo = false)
            }
        }
        binding.etFondoInicial.setOnEditorActionListener { _, _, _ ->
            actualizarResumenDesglose(sincronizarFondo = false)
            false
        }

        binding.btnAbrirCaja.setOnClickListener { abrirCaja() }

        actualizarResumenDesglose(sincronizarFondo = false)
        cargarCajas()
    }

    private fun cargarCajas() {
        binding.btnAbrirCaja.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerCajas()
                if (!isAdded || view == null) return@launch
                cajas = response.dataExitosa().orEmpty()

                if (cajas.isEmpty()) {
                    binding.spCaja.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("Sin cajas registradas")
                    )
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError()
                            ?: "No hay cajas registradas. Créalas en Configuración de caja.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                binding.spCaja.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    cajas.map { it.nombre ?: "Caja #${it.id}" }
                )
                binding.btnAbrirCaja.isEnabled = true
                actualizarResumenDesglose(sincronizarFondo = false)
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun actualizarResumenDesglose(sincronizarFondo: Boolean) {
        val resumen = DesgloseEfectivo.calcularResumen(conteoDenominaciones)
        binding.tvResumenBilletesMonedas.text =
            "Billetes: ${resumen.totalBilletes.formatearMonedaSinSufijo()} • Monedas: ${resumen.totalMonedas.formatearMonedaSinSufijo()}"
        binding.tvLimpiarDesglose.visibility =
            if (resumen.piezasTotales > 0) View.VISIBLE else View.GONE

        if (sincronizarFondo && !fondoEditadoManualmente) {
            actualizandoFondoDesdeDesglose = true
            try {
                val textoFondo = if (resumen.piezasTotales > 0) {
                    String.format(Locale.US, "%.2f", resumen.total)
                } else {
                    ""
                }
                binding.etFondoInicial.setText(textoFondo)
                binding.etFondoInicial.setSelection(textoFondo.length)
            } finally {
                actualizandoFondoDesdeDesglose = false
            }
        }

        binding.tvTotalCalculado.text = resumen.total.formatearMoneda()
        val fondoInicial = leerFondoInicial()

        binding.tvDiferenciaFondo.text = when {
            resumen.piezasTotales == 0 && fondoInicial == null ->
                "Ingresa el fondo inicial o cuenta las denominaciones"
            resumen.piezasTotales == 0 && fondoInicial != null ->
                "Fondo inicial sin desglose: ${fondoInicial.formatearMoneda()}"
            else -> {
                val diff = DesgloseEfectivo.calcularDiferencia(resumen.total, fondoInicial)
                when {
                    diff == null || abs(diff) < 0.005 -> "Cuadra con el fondo inicial"
                    diff > 0 -> "Diferencia con fondo inicial: +${diff.formatearMoneda()}"
                    else -> "Diferencia con fondo inicial: -${(-diff).formatearMoneda()}"
                }
            }
        }
    }

    private fun limpiarDesglose() {
        for (denominacion in DesgloseEfectivo.DENOMINACIONES) {
            conteoDenominaciones[denominacion.clave] = 0
        }
        denominacionAdapter.notifyDataSetChanged()
        actualizarResumenDesglose(sincronizarFondo = true)
    }

    private fun leerFondoInicial(): Double? =
        binding.etFondoInicial.text
            ?.toString()
            ?.trim()
            ?.replace(',', '.')
            ?.toDoubleOrNull()

    private fun abrirCaja() {
        val empleadoId = SessionManager.obtenerEmpleadoId(requireContext())
        if (empleadoId == null) {
            Toast.makeText(
                requireContext(),
                "Tu usuario no tiene un empleado asociado. No es posible abrir caja.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val caja = cajas.getOrNull(binding.spCaja.selectedItemPosition)
        if (caja == null) {
            Toast.makeText(requireContext(), "Selecciona una caja", Toast.LENGTH_SHORT).show()
            return
        }

        val resumen = DesgloseEfectivo.calcularResumen(conteoDenominaciones)
        val desgloseJson = DesgloseEfectivo.serializarJson(conteoDenominaciones)
        val montoTexto = leerFondoInicial()
        val monto = montoTexto ?: if (resumen.piezasTotales > 0) resumen.total else null

        if (monto == null || monto < 0) {
            Toast.makeText(requireContext(), "Ingresa un fondo inicial válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (desgloseJson != null && abs(resumen.total - monto) >= 0.01) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("El desglose no coincide")
                .setMessage(
                    "El desglose de efectivo suma ${resumen.total.formatearMoneda()}, " +
                        "pero el fondo inicial ingresado es ${monto.formatearMoneda()}."
                )
                .setNegativeButton("Revisar", null)
                .setPositiveButton("Usar ${resumen.total.formatearMoneda()}") { _, _ ->
                    fondoEditadoManualmente = false
                    actualizarResumenDesglose(sincronizarFondo = true)
                    enviarApertura(caja.id, empleadoId, resumen.total, desgloseJson)
                }
                .show()
            return
        }

        enviarApertura(caja.id, empleadoId, monto, desgloseJson)
    }

    private fun enviarApertura(
        cajaId: Int,
        empleadoId: Int,
        montoInicial: Double,
        desgloseJson: String?
    ) {
        binding.btnAbrirCaja.isEnabled = false
        binding.btnAbrirCaja.text = "Abriendo..."

        val fechaApertura = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearApertura(
                    CrearAperturaRequest(
                        cajaId = cajaId,
                        empleadoId = empleadoId,
                        montoInicial = montoInicial,
                        fechaApertura = fechaApertura,
                        observaciones = binding.etObservacionesApertura.text
                            ?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() },
                        desgloseInicio = desgloseJson
                    )
                )
                if (!isAdded || view == null) return@launch

                if (response.dataExitosa() == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo abrir la caja",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Caja abierta correctamente", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                if (isAdded && view != null) {
                    binding.btnAbrirCaja.isEnabled = true
                    binding.btnAbrirCaja.text = "Abrir Caja"
                }
            }
        }
    }

    private class DenominacionAdapter(
        private val items: List<DenominacionEfectivo>,
        private val conteo: MutableMap<String, Int>,
        private val onCambioCantidad: () -> Unit
    ) : RecyclerView.Adapter<DenominacionAdapter.DenominacionHolder>() {

        class DenominacionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDenominacion: TextView = itemView.findViewById(R.id.tvDenominacion)
            val tvTipoDenominacion: TextView = itemView.findViewById(R.id.tvTipoDenominacion)
            val btnMenos: TextView = itemView.findViewById(R.id.btnMenos)
            val etCantidad: EditText = itemView.findViewById(R.id.etCantidad)
            val btnMas: TextView = itemView.findViewById(R.id.btnMas)
            val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
            val vDivider: View = itemView.findViewById(R.id.vDivider)
            var watcher: TextWatcher? = null
            var actualizandoTexto = false
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DenominacionHolder {
            val vista = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_denominacion_efectivo, parent, false)
            return DenominacionHolder(vista)
        }

        override fun onBindViewHolder(holder: DenominacionHolder, position: Int) {
            val item = items[position]
            val cantidadInicial = DesgloseEfectivo.normalizarCantidad(conteo[item.clave] ?: 0)

            holder.tvDenominacion.text = item.etiqueta
            holder.tvTipoDenominacion.text = item.tipo.etiqueta
            holder.vDivider.visibility = if (position == items.lastIndex) View.GONE else View.VISIBLE

            holder.watcher?.let { holder.etCantidad.removeTextChangedListener(it) }
            holder.actualizandoTexto = true
            holder.etCantidad.setText(if (cantidadInicial > 0) cantidadInicial.toString() else "")
            holder.actualizandoTexto = false

            actualizarSubtotalFila(holder, item, cantidadInicial)

            val nuevoWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (holder.actualizandoTexto) return
                    val texto = s?.toString()?.trim().orEmpty()
                    val crudo = texto.toIntOrNull() ?: 0
                    val normalizada = DesgloseEfectivo.normalizarCantidad(crudo)
                    if (crudo > DesgloseEfectivo.MAX_CANTIDAD_POR_DENOMINACION) {
                        holder.actualizandoTexto = true
                        val textoLimite = normalizada.toString()
                        holder.etCantidad.setText(textoLimite)
                        holder.etCantidad.setSelection(textoLimite.length)
                        holder.actualizandoTexto = false
                    }
                    conteo[item.clave] = normalizada
                    actualizarSubtotalFila(holder, item, normalizada)
                    onCambioCantidad()
                }
            }
            holder.watcher = nuevoWatcher
            holder.etCantidad.addTextChangedListener(nuevoWatcher)

            holder.btnMenos.setOnClickListener {
                val actual = conteo[item.clave] ?: 0
                aplicarCantidadDesdeBoton(holder, item, actual - 1)
            }

            holder.btnMas.setOnClickListener {
                val actual = conteo[item.clave] ?: 0
                aplicarCantidadDesdeBoton(holder, item, actual + 1)
            }
        }

        override fun getItemCount(): Int = items.size

        private fun aplicarCantidadDesdeBoton(
            holder: DenominacionHolder,
            item: DenominacionEfectivo,
            candidato: Int
        ) {
            val nuevaCantidad = DesgloseEfectivo.normalizarCantidad(candidato)
            conteo[item.clave] = nuevaCantidad
            holder.actualizandoTexto = true
            val texto = if (nuevaCantidad > 0) nuevaCantidad.toString() else ""
            holder.etCantidad.setText(texto)
            holder.etCantidad.setSelection(texto.length)
            holder.actualizandoTexto = false
            actualizarSubtotalFila(holder, item, nuevaCantidad)
            onCambioCantidad()
        }

        private fun actualizarSubtotalFila(
            holder: DenominacionHolder,
            item: DenominacionEfectivo,
            cantidad: Int
        ) {
            val subtotal = item.valor * cantidad
            holder.tvSubtotal.text = subtotal.formatearMonedaSinSufijo()
            holder.tvSubtotal.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (cantidad > 0) R.color.apex_orange else R.color.apex_text_secondary
                )
            )
        }
    }
}

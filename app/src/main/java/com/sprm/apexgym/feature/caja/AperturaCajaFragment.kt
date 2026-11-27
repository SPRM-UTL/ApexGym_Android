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
import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearMoneda
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.core.ui.InputFilters
import com.sprm.apexgym.databinding.FragmentAperturaCajaBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

class AperturaCajaFragment : BaseFragment<FragmentAperturaCajaBinding>() {

    private var cajas: List<CajaDto> = emptyList()
    private var empleadosResponsables: List<EmpleadoDto> = emptyList()
    private val conteoDenominaciones = linkedMapOf<String, Int>().apply {
        DesgloseEfectivo.DENOMINACIONES.forEach { put(it.clave, 0) }
    }
    private lateinit var denominacionAdapter: DenominacionEfectivoAdapter
    private var fondoEditadoManualmente = false
    private var actualizandoFondoDesdeDesglose = false

    override fun getLayoutRes(): Int = R.layout.fragment_apertura_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }

        denominacionAdapter = DenominacionEfectivoAdapter(
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

        binding.etFondoInicial.filters = InputFilters.decimalAmount(maxIntegers = 6, maxDecimals = 2)
        binding.etObservacionesApertura.filters = InputFilters.plainText(500, allowNewLines = true)

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
                val ctx = requireContext()
                val cajasDef = async { RetrofitClient.empleadoService.obtenerCajas() }
                val aperturasDef = async { RetrofitClient.empleadoService.obtenerAperturas() }
                val empleadosDef = async {
                    EmpleadoSelectorHelper.prepararSelector(
                        ctx,
                        binding.llResponsableApertura,
                        binding.spResponsableApertura
                    )
                }

                val response = cajasDef.await()
                val aperturasResp = aperturasDef.await()
                val empleadosRes = empleadosDef.await()
                if (!isAdded || view == null) return@launch

                empleadosResponsables = empleadosRes.empleados
                val todasCajas = response.dataExitosa().orEmpty()
                val aperturasAbiertasIds = aperturasResp.dataExitosa()
                    .orEmpty()
                    .filter { CajaValidacionLogic.esAperturaAbierta(it.estado) }
                    .map { it.cajaId }
                    .toSet()

                cajas = todasCajas.filter { caja ->
                    CajaValidacionLogic.esEstadoCajaActivo(caja.estado) &&
                        caja.id !in aperturasAbiertasIds
                }

                if (cajas.isEmpty()) {
                    val mensaje = when {
                        todasCajas.isEmpty() ->
                            response.mensajeDeError()
                                ?: "No hay cajas registradas. Créalas en Configuración de caja."
                        todasCajas.none { CajaValidacionLogic.esEstadoCajaActivo(it.estado) } ->
                            "No hay cajas activas disponibles para abrir."
                        else ->
                            "Todas las cajas activas ya tienen una apertura abierta."
                    }
                    binding.spCaja.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("Sin cajas disponibles")
                    )
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    return@launch
                }

                binding.spCaja.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    cajas.map { caja ->
                        val ubic = caja.ubicacion?.takeIf { it.isNotBlank() }
                        if (ubic != null) "${caja.nombre ?: "Caja #${caja.id}"} ($ubic)"
                        else caja.nombre ?: "Caja #${caja.id}"
                    }
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
        val empleadoId = EmpleadoSelectorHelper.resolverEmpleadoId(
            requireContext(),
            empleadosResponsables,
            binding.spResponsableApertura
        )
        if (empleadoId == null) {
            Toast.makeText(
                requireContext(),
                "Selecciona un empleado responsable para abrir la caja.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val caja = cajas.getOrNull(binding.spCaja.selectedItemPosition)
        if (caja == null) {
            Toast.makeText(requireContext(), "Selecciona una caja disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val resumen = DesgloseEfectivo.calcularResumen(conteoDenominaciones)
        val desgloseJson = DesgloseEfectivo.serializarJson(conteoDenominaciones)
        val montoTexto = leerFondoInicial()
        val monto = montoTexto ?: if (resumen.piezasTotales > 0) resumen.total else null
        val obs = binding.etObservacionesApertura.text?.toString()

        val errorValidacion = CajaValidacionLogic.validarMontoApertura(monto, obs)
        if (errorValidacion != null || monto == null) {
            Toast.makeText(
                requireContext(),
                errorValidacion ?: "Ingresa un fondo inicial válido",
                Toast.LENGTH_SHORT
            ).show()
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

        val fechaApertura = CajaValidacionLogic.formatearFechaAperturaUtc()

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
}

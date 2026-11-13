package com.sprm.apexgym.feature.tienda

import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
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
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.CrearCobroRequest
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.network.mensajeUsuario
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentCheckoutPosBinding
import com.sprm.apexgym.feature.main.CartItem
import com.sprm.apexgym.feature.main.CartManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Cobro de la tienda POS para empleados.
 * La API solo soporta un proveedor por cobro: efectivo o terminal manual.
 */
class CheckoutFragment : BaseFragment<FragmentCheckoutPosBinding>() {

    private enum class MetodoCobro {
        EFECTIVO,
        TERMINAL_MANUAL
    }

    private var metodoSeleccionado = MetodoCobro.EFECTIVO
    private var total: Double = 0.0
    private var aperturaCajaId: Int? = null
    private var cobrando = false
    private val idempotenciaKey: String = UUID.randomUUID().toString()

    override fun getLayoutRes(): Int = R.layout.fragment_checkout_pos

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val items = CartManager.obtenerItems(requireContext())
        if (items.isEmpty()) {
            parentFragmentManager.popBackStack()
            return
        }

        total = CartManager.obtenerTotalPrecio(requireContext())

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.cardEfectivo.setOnClickListener { seleccionarMetodo(MetodoCobro.EFECTIVO) }
        binding.cardTerminal.setOnClickListener { seleccionarMetodo(MetodoCobro.TERMINAL_MANUAL) }
        binding.etEfectivoRecibido.doAfterTextChanged { actualizarCambio() }
        binding.btnCobrar.setOnClickListener { validarYCobrar() }

        binding.btnCobrar.text = "Cobrar ${total.formatearMonedaSinSufijo()} MXN"
        binding.tvTotalPagar.text = "${total.formatearMonedaSinSufijo()} MXN"

        renderizarResumen(items)
        seleccionarMetodo(MetodoCobro.EFECTIVO)
        cargarAperturaActiva()

        if (!SessionManager.esEmpleado(requireContext())) {
            mostrarError("El cobro POS está disponible solo para empleados.")
            binding.btnCobrar.isEnabled = false
        } else if (SessionManager.obtenerEmpleadoId(requireContext()) == null) {
            mostrarError(
                "No se pudo identificar al empleado en la sesión. Vuelve a iniciar sesión."
            )
            binding.btnCobrar.isEnabled = false
        }
    }

    private fun renderizarResumen(items: List<CartItem>) {
        binding.llResumenItems.removeAllViews()

        items.forEach { item ->
            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
            }

            val tvNombre = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = buildString {
                    append(item.nombre)
                    if (item.tipo == com.sprm.apexgym.feature.main.CartItemType.PRODUCTO &&
                        item.cantidad > 1
                    ) {
                        append(" × ${item.cantidad}")
                    }
                }
                setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, 0, dp(8), 0)
            }
            fila.addView(tvNombre)

            val tvSubtotal = TextView(requireContext()).apply {
                text = "${item.subtotal.formatearMonedaSinSufijo()} MXN"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setTypeface(typeface, Typeface.BOLD)
            }
            fila.addView(tvSubtotal)

            binding.llResumenItems.addView(fila)
        }
    }

    private fun seleccionarMetodo(metodo: MetodoCobro) {
        metodoSeleccionado = metodo
        val context = requireContext()
        val naranja = ContextCompat.getColor(context, R.color.apex_orange)
        val gris = ContextCompat.getColor(context, R.color.apex_stroke_light)
        val secundario = ContextCompat.getColor(context, R.color.apex_text_secondary)

        val esEfectivo = metodo == MetodoCobro.EFECTIVO
        binding.cardEfectivo.strokeColor = if (esEfectivo) naranja else gris
        binding.cardEfectivo.strokeWidth = if (esEfectivo) dp(2) else dp(1)
        binding.cardTerminal.strokeColor = if (esEfectivo) gris else naranja
        binding.cardTerminal.strokeWidth = if (esEfectivo) dp(1) else dp(2)

        binding.tvEfectivoCheck.text = if (esEfectivo) "●" else "○"
        binding.tvEfectivoCheck.setTextColor(if (esEfectivo) naranja else secundario)
        binding.tvTerminalCheck.text = if (esEfectivo) "○" else "●"
        binding.tvTerminalCheck.setTextColor(if (esEfectivo) secundario else naranja)

        binding.llEfectivoFields.visibility = if (esEfectivo) View.VISIBLE else View.GONE
        binding.llTerminalFields.visibility = if (esEfectivo) View.GONE else View.VISIBLE
        binding.tvCheckoutError.visibility = View.GONE
    }

    private fun actualizarCambio() {
        val recibido = leerMonto(binding.etEfectivoRecibido.text?.toString())
        val cambio = if (recibido != null) recibido - total else 0.0
        binding.tvCambio.text = "Cambio: ${cambio.coerceAtLeast(0.0).formatearMonedaSinSufijo()} MXN"
        binding.tvCambio.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (recibido != null && recibido >= total) R.color.apex_text_secondary else R.color.apex_orange
            )
        )
    }

    private fun cargarAperturaActiva() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val aperturas = RetrofitClient.empleadoService.obtenerAperturas().dataExitosa()
                    .orEmpty()
                if (!isAdded) return@launch

                val empleadoId = SessionManager.obtenerEmpleadoId(requireContext())
                val activas = aperturas.filter { it.estado.equals("ABIERTA", ignoreCase = true) }
                val activa = activas.firstOrNull {
                    empleadoId != null && it.empleadoId == empleadoId
                } ?: activas.firstOrNull()

                aperturaCajaId = activa?.id
                binding.tvCajaActiva.text = if (activa != null) {
                    val caja = activa.caja?.nombre?.takeIf { it.isNotBlank() }
                        ?: "Caja #${activa.cajaId}"
                    "Caja activa: $caja • Apertura #${activa.id}"
                } else {
                    "Sin apertura de caja activa. El efectivo no se registrará en caja."
                }
            } catch (_: Exception) {
                if (isAdded) {
                    binding.tvCajaActiva.text = "No se pudo verificar la caja activa."
                }
            }
        }
    }

    private fun validarYCobrar() {
        if (cobrando) return
        binding.tvCheckoutError.visibility = View.GONE

        val items = CartManager.obtenerItems(requireContext())
        if (items.isEmpty()) {
            mostrarError("El carrito está vacío.")
            return
        }

        val empleadoId = SessionManager.obtenerEmpleadoId(requireContext())
        if (empleadoId == null) {
            mostrarError(
                "No se pudo identificar al empleado en la sesión. Vuelve a iniciar sesión."
            )
            return
        }

        var efectivoRecibido: Double? = null
        var folioVoucher: String? = null

        when (metodoSeleccionado) {
            MetodoCobro.EFECTIVO -> {
                efectivoRecibido = leerMonto(binding.etEfectivoRecibido.text?.toString())
                if (efectivoRecibido == null) {
                    mostrarError("Ingresa el efectivo recibido.")
                    return
                }
                if (efectivoRecibido < total) {
                    mostrarError("El efectivo recibido no cubre el total a cobrar.")
                    return
                }
            }

            MetodoCobro.TERMINAL_MANUAL -> {
                folioVoucher = binding.etFolioVoucher.text?.toString()?.trim()
                if (folioVoucher.isNullOrBlank()) {
                    mostrarError("Ingresa el folio o autorización del voucher.")
                    return
                }
            }
        }

        realizarCobro(items, empleadoId, efectivoRecibido, folioVoucher)
    }

    private fun realizarCobro(
        items: List<CartItem>,
        empleadoId: Int,
        efectivoRecibido: Double?,
        folioVoucher: String?
    ) {
        cobrando = true
        binding.btnCobrar.isEnabled = false
        binding.btnCobrar.text = "Procesando cobro…"

        val concepto = if (items.size == 1) {
            items.first().nombre
        } else {
            "Venta POS (${items.sumOf { it.cantidad }} artículos)"
        }.take(240)

        val metadatos = mutableMapOf<String, Any>(
            "origen" to "POS_ANDROID",
            "empleadoId" to empleadoId,
            "items" to items.map { item ->
                mapOf(
                    "tipo" to item.tipo.name,
                    "id" to item.id,
                    "nombre" to item.nombre,
                    "cantidad" to item.cantidad,
                    "precioUnitario" to item.precioUnitario,
                    "subtotal" to item.subtotal
                )
            }
        )
        aperturaCajaId?.let { metadatos["aperturaCajaId"] = it }
        efectivoRecibido?.let {
            metadatos["efectivoRecibido"] = it
            metadatos["cambio"] = it - total
        }

        val request = CrearCobroRequest(
            concepto = concepto,
            monto = total,
            proveedor = metodoSeleccionado.name,
            folioVoucher = folioVoucher,
            uuidIdempotencia = idempotenciaKey,
            metadatos = metadatos
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearCobro(request)
                val resultado = response.dataExitosa()

                if (!isAdded) return@launch

                if (resultado == null) {
                    mostrarError(
                        response.mensajeDeError()
                            ?: response.body()?.message
                            ?: "No se pudo registrar el cobro."
                    )
                    return@launch
                }

                val transaccion = resultado.transaccion
                if (transaccion == null) {
                    mostrarError("El servidor no devolvió la transacción del cobro.")
                    return@launch
                }

                if (!transaccion.estado.equals("APROBADO", ignoreCase = true)) {
                    mostrarError(
                        "El cobro quedó en estado ${transaccion.estado ?: "desconocido"}. " +
                            "El carrito no se vació."
                    )
                    return@launch
                }

                val itemsJson = construirItemsJson(items)
                CartManager.vaciarCarrito(requireContext())

                Toast.makeText(
                    requireContext(),
                    resultado.mensaje ?: "Cobro registrado correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(
                        R.id.fragment_container,
                        TicketCompraFragment.newInstance(
                            transaccion = transaccion,
                            itemsJson = itemsJson,
                            metodoPago = metodoSeleccionado.name
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            } catch (e: Exception) {
                if (isAdded) mostrarError(e.mensajeUsuario())
            } finally {
                cobrando = false
                if (isAdded) {
                    binding.btnCobrar.isEnabled = true
                    binding.btnCobrar.text = "Cobrar ${total.formatearMonedaSinSufijo()} MXN"
                }
            }
        }
    }

    private fun construirItemsJson(items: List<CartItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("nombre", item.nombre)
                    put("cantidad", item.cantidad)
                    put("precioUnitario", item.precioUnitario)
                    put("subtotal", item.subtotal)
                }
            )
        }
        return array.toString()
    }

    private fun leerMonto(texto: String?): Double? {
        val limpio = texto?.trim()?.replace(",", ".").orEmpty()
        if (limpio.isBlank()) return null
        return limpio.toDoubleOrNull()?.takeIf { it >= 0.0 }
    }

    private fun mostrarError(mensaje: String) {
        binding.tvCheckoutError.text = mensaje
        binding.tvCheckoutError.visibility = View.VISIBLE
    }

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()
}

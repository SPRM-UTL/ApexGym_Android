package com.sprm.apexgym.feature.tienda

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.sprm.apexgym.core.network.api.MetodoPagoDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearMonedaSinSufijo
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.network.mensajeUsuario
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentCheckoutPosBinding
import com.sprm.apexgym.feature.main.CartItem
import com.sprm.apexgym.feature.main.CartItemType
import com.sprm.apexgym.feature.main.CartManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Cobro de la tienda POS para empleados.
 * Carga dinámicamente las formas de pago configuradas en el catálogo del sistema
 * (`GET /api/metodos-pago`) y registra el cobro en `/api/pagos/crear-cobro`.
 */
class CheckoutFragment : BaseFragment<FragmentCheckoutPosBinding>() {

    private var metodosSistema: List<MetodoPagoDto> = emptyList()
    private var metodoSeleccionado: MetodoPagoDto? = null
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
        binding.etEfectivoRecibido.doAfterTextChanged { actualizarCambio() }
        binding.btnCobrar.setOnClickListener { validarYCobrar() }

        binding.btnCobrar.text = "Cobrar ${total.formatearMonedaSinSufijo()} MXN"
        binding.tvTotalPagar.text = "${total.formatearMonedaSinSufijo()} MXN"

        // Ocultar las 2 tarjetas estáticas heredadas para mostrar las dinámicas del sistema
        binding.cardEfectivo.visibility = View.GONE
        binding.cardTerminal.visibility = View.GONE

        renderizarResumen(items)
        cargarMetodosPagoDelSistema(view)
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

    private fun cargarMetodosPagoDelSistema(rootView: View) {
        val contenedor = rootView.findViewWithTag<LinearLayout>("ll_metodos_pago_dinamicos")
        viewLifecycleOwner.lifecycleScope.launch {
            val cargados = try {
                RetrofitClient.empleadoService.obtenerMetodosPago()
                    .dataExitosa()
                    .orEmpty()
                    .filter { it.estado.isNullOrBlank() || it.estado.equals("activo", ignoreCase = true) }
            } catch (_: Exception) {
                emptyList()
            }

            if (!isAdded) return@launch

            metodosSistema = if (cargados.isNotEmpty()) {
                cargados
            } else {
                listOf(
                    MetodoPagoDto(id = 1, nombre = "Efectivo", descripcion = "Pago en caja con efectivo", estado = "activo"),
                    MetodoPagoDto(id = 2, nombre = "Tarjeta", descripcion = "Pago con tarjeta en terminal", estado = "activo"),
                    MetodoPagoDto(id = 3, nombre = "Transferencia", descripcion = "Pago por transferencia bancaria", estado = "activo")
                )
            }

            if (metodoSeleccionado == null) {
                metodoSeleccionado = metodosSistema.firstOrNull()
            }
            renderizarMetodosDinamicos(contenedor)
            actualizarCamposSegunMetodo()
        }
    }

    private fun renderizarMetodosDinamicos(contenedor: LinearLayout?) {
        if (contenedor == null) return
        val context = requireContext()
        val naranja = ContextCompat.getColor(context, R.color.apex_orange)
        val grisBorde = ContextCompat.getColor(context, R.color.apex_stroke_light)
        val primario = ContextCompat.getColor(context, R.color.apex_text_primary)
        val secundario = ContextCompat.getColor(context, R.color.apex_text_secondary)

        contenedor.removeAllViews()

        metodosSistema.forEach { metodo ->
            val seleccionado = metodoSeleccionado?.id == metodo.id && metodoSeleccionado?.nombre == metodo.nombre

            val card = MaterialCardView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                radius = dp(16).toFloat()
                cardElevation = 0f
                strokeWidth = dp(if (seleccionado) 2 else 1)
                strokeColor = if (seleccionado) naranja else grisBorde
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    metodoSeleccionado = metodo
                    renderizarMetodosDinamicos(contenedor)
                    actualizarCamposSegunMetodo()
                }
            }

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            val radioOuter = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(context, R.color.white))
                    setStroke(dp(if (seleccionado) 2 else 1), if (seleccionado) naranja else grisBorde)
                }
                if (seleccionado) {
                    addView(View(context).apply {
                        layoutParams = FrameLayout.LayoutParams(dp(12), dp(12), Gravity.CENTER)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(naranja)
                        }
                    })
                }
            }
            row.addView(radioOuter)

            val col = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(12)
                }
            }

            col.addView(TextView(context).apply {
                text = metodo.nombre?.takeIf { it.isNotBlank() } ?: "Método #${metodo.id}"
                setTextColor(primario)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
            })

            metodo.descripcion?.takeIf { it.isNotBlank() }?.let { desc ->
                col.addView(TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(2) }
                    text = desc
                    setTextColor(secundario)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                })
            }

            row.addView(col)
            card.addView(row)
            contenedor.addView(card)
        }
    }

    private fun esMetodoEfectivo(metodo: MetodoPagoDto?): Boolean {
        val nombre = metodo?.nombre?.lowercase().orEmpty()
        return nombre.contains("efectivo")
    }

    private fun actualizarCamposSegunMetodo() {
        val esEfectivo = esMetodoEfectivo(metodoSeleccionado)
        binding.llEfectivoFields.visibility = if (esEfectivo) View.VISIBLE else View.GONE
        binding.llTerminalFields.visibility = if (esEfectivo) View.GONE else View.VISIBLE
        if (!esEfectivo) {
            val nombre = metodoSeleccionado?.nombre ?: "pago"
            binding.etFolioVoucher.hint = "Folio / referencia de $nombre"
        }
        binding.tvCheckoutError.visibility = View.GONE
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
                    "Sin apertura de caja activa en esta sucursal."
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

        val metodo = metodoSeleccionado
        if (metodo == null) {
            mostrarError("Selecciona una forma de pago.")
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
        val esEfectivo = esMetodoEfectivo(metodo)

        if (esEfectivo) {
            efectivoRecibido = leerMonto(binding.etEfectivoRecibido.text?.toString())
            if (efectivoRecibido == null) {
                mostrarError("Ingresa el efectivo recibido.")
                return
            }
            if (efectivoRecibido < total) {
                mostrarError("El efectivo recibido no cubre el total a cobrar.")
                return
            }
        } else {
            folioVoucher = binding.etFolioVoucher.text?.toString()?.trim()
            if (folioVoucher.isNullOrBlank()) {
                mostrarError("Ingresa el folio o referencia de ${metodo.nombre ?: "pago"}.")
                return
            }
        }

        realizarCobro(items, metodo, empleadoId, efectivoRecibido, folioVoucher)
    }

    private fun realizarCobro(
        items: List<CartItem>,
        metodo: MetodoPagoDto,
        empleadoId: Int,
        efectivoRecibido: Double?,
        folioVoucher: String?
    ) {
        cobrando = true
        binding.btnCobrar.isEnabled = false
        binding.btnCobrar.text = "Procesando cobro…"

        val esEfectivo = esMetodoEfectivo(metodo)
        val proveedorApi = if (esEfectivo) "EFECTIVO" else "TERMINAL_MANUAL"
        val nombreMetodo = metodo.nombre?.takeIf { it.isNotBlank() } ?: proveedorApi

        val concepto = if (items.size == 1) {
            items.first().nombre
        } else {
            "Venta POS (${items.sumOf { it.cantidad }} artículos)"
        }.take(240)

        val detalleVenta = items.map { item ->
            com.sprm.apexgym.core.network.api.DetalleVentaRequest(
                tipoItem = item.tipo.name,
                cantidad = item.cantidad,
                productoId = if (item.tipo == CartItemType.PRODUCTO) item.id else null,
                servicioId = if (item.tipo == CartItemType.SERVICIO) item.id else null,
                tipoMembresiaId = if (item.tipo == CartItemType.MEMBRESIA) item.id else null,
                descripcion = item.nombre
            )
        }

        val request = com.sprm.apexgym.core.network.api.CrearVentaRequest(
            empleadoId = empleadoId,
            aperturaCajaId = aperturaCajaId,
            metodoPagoId = metodo.id,
            subtotal = total,
            total = total,
            montoRecibido = efectivoRecibido,
            detalleVenta = detalleVenta,
            uuidIdempotencia = idempotenciaKey,
            pago = com.sprm.apexgym.core.network.api.PagoVentaRequest(
                folioVoucher = folioVoucher,
                referencia = folioVoucher
            )
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearVenta(request, idempotencyKey = idempotenciaKey)
                val resultado = response.dataExitosa()

                if (!isAdded) return@launch

                if (resultado == null) {
                    mostrarError(
                        response.mensajeDeError()
                            ?: response.body()?.message
                            ?: "No se pudo registrar la venta."
                    )
                    return@launch
                }

                val transaccion = com.sprm.apexgym.core.network.api.TransaccionPagoDto(
                    id = resultado.id,
                    uuidIdempotencia = idempotenciaKey,
                    monto = resultado.total,
                    estado = resultado.estado?.uppercase() ?: "APROBADO",
                    concepto = concepto,
                    folioVoucher = folioVoucher ?: resultado.folioVenta?.id?.let { "F-$it" },
                    createdAt = resultado.createdAt
                )

                val itemsJson = construirItemsJson(items)
                CartManager.vaciarCarrito(requireContext())

                Toast.makeText(
                    requireContext(),
                    "Venta registrada correctamente.",
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
                            metodoPago = nombreMetodo
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

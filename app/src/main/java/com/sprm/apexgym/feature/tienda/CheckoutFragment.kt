package com.sprm.apexgym.feature.tienda

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import com.sprm.apexgym.core.network.api.ClienteAsistenciaDto
import com.sprm.apexgym.core.network.api.CrearVentaPosRequest
import com.sprm.apexgym.core.network.api.DetalleVentaPosRequest
import com.sprm.apexgym.core.network.api.MetodoPagoDto
import com.sprm.apexgym.core.network.api.PagoVentaPosRequest
import com.sprm.apexgym.core.network.api.TransaccionPagoDto
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
 * Cobro de la tienda POS para empleados conectado a `POST /api/ventas`
 * para registrar la venta en caja y descontar el stock del inventario en el servidor.
 */
class CheckoutFragment : BaseFragment<FragmentCheckoutPosBinding>() {

    private var metodosSistema: List<MetodoPagoDto> = emptyList()
    private var metodoSeleccionado: MetodoPagoDto? = null
    private var clientesDisponibles: List<ClienteAsistenciaDto> = emptyList()
    private var totales: TotalesVentaPos = TotalesVentaPos(0.0, 0.0, 0.0)
    private var aperturaCajaId: Int? = null
    private var cobrando = false
    private var idempotenciaKey: String = UUID.randomUUID().toString()

    override fun getLayoutRes(): Int = R.layout.fragment_checkout_pos

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val items = CartManager.obtenerItems(requireContext())
        if (items.isEmpty()) {
            parentFragmentManager.popBackStack()
            return
        }

        totales = CalculoVentaPos.calcularTotales(items.map { it.precioUnitario to it.cantidad })

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.etEfectivoRecibido.doAfterTextChanged { actualizarCambio() }
        binding.btnCobrar.setOnClickListener { validarYCobrar() }

        binding.tvSubtotalPagar.text = "${totales.subtotal.formatearMonedaSinSufijo()} MXN"
        binding.tvIvaPagar.text = "${totales.iva.formatearMonedaSinSufijo()} MXN"
        binding.tvTotalPagar.text = "${totales.total.formatearMonedaSinSufijo()} MXN"
        binding.btnCobrar.text = "Cobrar ${totales.total.formatearMonedaSinSufijo()} MXN"

        // Ocultar las 2 tarjetas estáticas heredadas para mostrar las dinámicas del sistema
        binding.cardEfectivo.visibility = View.GONE
        binding.cardTerminal.visibility = View.GONE

        renderizarResumen(items)
        cargarClientes()
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

    private fun cargarClientes() {
        binding.spClienteVenta.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Cargando clientes…")
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val lista = RetrofitClient.empleadoService.obtenerClientes()
                    .dataExitosa()
                    .orEmpty()
                if (!isAdded || view == null) return@launch

                clientesDisponibles = lista
                val etiquetas = if (lista.isEmpty()) {
                    listOf("Sin clientes registrados en la sucursal")
                } else {
                    lista.map { cliente ->
                        cliente.nombreCompleto.ifBlank { "Cliente #${cliente.id}" }
                    }
                }
                binding.spClienteVenta.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    etiquetas
                )
            } catch (_: Exception) {
                if (!isAdded || view == null) return@launch
                clientesDisponibles = emptyList()
                binding.spClienteVenta.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf("Error al cargar clientes")
                )
            }
        }
    }

    private fun cargarMetodosPagoDelSistema(rootView: View) {
        val contenedor = rootView.findViewWithTag<LinearLayout>("ll_metodos_pago_dinamicos")
        viewLifecycleOwner.lifecycleScope.launch {
            val cargados = try {
                RetrofitClient.empleadoService.obtenerMetodosPago()
                    .dataExitosa()
                    .orEmpty()
                    .filter {
                        (it.estado.isNullOrBlank() || it.estado.equals("activo", ignoreCase = true)) &&
                            !it.codigo.equals("MERCADO_PAGO", ignoreCase = true)
                    }
            } catch (_: Exception) {
                emptyList()
            }

            if (!isAdded || view == null) return@launch

            metodosSistema = if (cargados.isNotEmpty()) {
                cargados
            } else {
                listOf(
                    MetodoPagoDto(id = 2, nombre = "Efectivo", codigo = "EFECTIVO", descripcion = "Pago en caja con efectivo", estado = "activo"),
                    MetodoPagoDto(id = 1, nombre = "Tarjeta", codigo = "TARJETA", descripcion = "Pago con tarjeta en terminal", estado = "activo"),
                    MetodoPagoDto(id = 3, nombre = "Transferencia", codigo = "TRANSFERENCIA", descripcion = "Pago por transferencia bancaria", estado = "activo")
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
        val codigo = metodo?.codigo?.uppercase().orEmpty()
        val nombre = metodo?.nombre?.lowercase().orEmpty()
        return codigo == "EFECTIVO" || nombre.contains("efectivo")
    }

    private fun esMetodoTransferencia(metodo: MetodoPagoDto?): Boolean {
        val codigo = metodo?.codigo?.uppercase().orEmpty()
        val nombre = metodo?.nombre?.lowercase().orEmpty()
        return codigo == "TRANSFERENCIA" || nombre.contains("transferencia")
    }

    private fun actualizarCamposSegunMetodo() {
        val esEfectivo = esMetodoEfectivo(metodoSeleccionado)
        binding.llEfectivoFields.visibility = if (esEfectivo) View.VISIBLE else View.GONE
        binding.llTerminalFields.visibility = if (esEfectivo) View.GONE else View.VISIBLE
        if (!esEfectivo) {
            val nombre = metodoSeleccionado?.nombre ?: "pago"
            binding.etFolioVoucher.hint = "Folio de $nombre (opcional: AUT-xxx)"
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
                    if (item.tipo == CartItemType.PRODUCTO && item.cantidad > 1) {
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
        val cambio = ValidadorPagoAndroid.calcularCambio(recibido, totales.total)
        binding.tvCambio.text = "Cambio: ${cambio.formatearMonedaSinSufijo()} MXN"
        binding.tvCambio.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (recibido != null && ValidadorPagoAndroid.validarEfectivo(recibido, totales.total).valido) {
                    R.color.apex_text_secondary
                } else {
                    R.color.apex_orange
                }
            )
        )
    }

    private fun cargarAperturaActiva() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val aperturas = RetrofitClient.empleadoService.obtenerAperturas().dataExitosa()
                    .orEmpty()
                if (!isAdded || view == null) return@launch

                val empleadoId = SessionManager.obtenerEmpleadoId(requireContext())
                val activas = aperturas.filter { it.estado.equals("ABIERTA", ignoreCase = true) }
                // VentaDao requiere que la apertura pertenezca al empleado que registra la venta.
                val activaPropia = activas.firstOrNull {
                    empleadoId != null && it.empleadoId == empleadoId
                }

                aperturaCajaId = activaPropia?.id
                binding.tvCajaActiva.text = when {
                    activaPropia != null -> {
                        val caja = activaPropia.caja?.nombre?.takeIf { it.isNotBlank() }
                            ?: "Caja #${activaPropia.cajaId}"
                        "Caja activa: $caja • Apertura #${activaPropia.id}"
                    }
                    activas.isNotEmpty() ->
                        "No tienes una caja abierta asignada a tu empleado. Abre una caja para cobrar."
                    else ->
                        "Sin apertura de caja activa en esta sucursal. Abre una caja primero."
                }
            } catch (_: Exception) {
                if (isAdded && view != null) {
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

        val aperturaId = aperturaCajaId
        if (aperturaId == null) {
            mostrarError("Necesitas abrir una caja a tu nombre en esta sucursal para registrar la venta.")
            return
        }

        val cliente = clientesDisponibles.getOrNull(binding.spClienteVenta.selectedItemPosition)
        if (cliente == null) {
            mostrarError("Selecciona el cliente para registrar la venta.")
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
            val textoEfectivo = binding.etEfectivoRecibido.text?.toString()?.trim()
            efectivoRecibido = if (textoEfectivo.isNullOrBlank()) {
                totales.total
            } else {
                leerMonto(textoEfectivo)
            }
            val validacionEfectivo = ValidadorPagoAndroid.validarEfectivo(efectivoRecibido, totales.total)
            if (!validacionEfectivo.valido) {
                mostrarError("El efectivo recibido no cubre el total a cobrar (${totales.total.formatearMonedaSinSufijo()} MXN).")
                return
            }
            efectivoRecibido = validacionEfectivo.montoRecibido
        } else {
            val typedFolio = binding.etFolioVoucher.text?.toString()?.trim()
            if (!typedFolio.isNullOrBlank()) {
                val validacionFolio = ValidadorPagoAndroid.validarFolioVoucher(
                    folioVoucher = typedFolio,
                    obligatorio = true
                )
                if (!validacionFolio.valido) {
                    mostrarError(validacionFolio.mensajeError ?: "Folio o referencia inválida.")
                    return
                }
                folioVoucher = typedFolio
            } else {
                folioVoucher = "AUT-${System.currentTimeMillis() % 1000000}"
            }
        }

        realizarVenta(
            items = items,
            aperturaId = aperturaId,
            clienteId = cliente.id,
            metodo = metodo,
            empleadoId = empleadoId,
            efectivoRecibido = efectivoRecibido,
            folioVoucher = folioVoucher
        )
    }

    private fun realizarVenta(
        items: List<CartItem>,
        aperturaId: Int,
        clienteId: Int,
        metodo: MetodoPagoDto,
        empleadoId: Int,
        efectivoRecibido: Double?,
        folioVoucher: String?
    ) {
        cobrando = true
        binding.btnCobrar.isEnabled = false
        binding.btnCobrar.text = "Procesando venta…"

        val detalles = items.map { item ->
            when (item.tipo) {
                CartItemType.PRODUCTO -> DetalleVentaPosRequest(
                    tipoItem = "PRODUCTO",
                    productoId = item.id,
                    cantidad = item.cantidad,
                    descripcion = item.nombre
                )
                CartItemType.SERVICIO -> DetalleVentaPosRequest(
                    tipoItem = "SERVICIO",
                    servicioId = item.id,
                    cantidad = item.cantidad,
                    descripcion = item.nombre
                )
                CartItemType.MEMBRESIA -> DetalleVentaPosRequest(
                    tipoItem = "MEMBRESIA",
                    tipoMembresiaId = item.id,
                    cantidad = item.cantidad,
                    descripcion = item.nombre
                )
            }
        }

        val pagoRequest = when {
            esMetodoEfectivo(metodo) -> null
            esMetodoTransferencia(metodo) -> PagoVentaPosRequest(referenciaTransferencia = folioVoucher)
            else -> PagoVentaPosRequest(folioVoucher = folioVoucher)
        }

        val request = CrearVentaPosRequest(
            empleadoId = empleadoId,
            aperturaCajaId = aperturaId,
            clienteId = clienteId,
            metodoPagoId = metodo.id,
            subtotal = totales.subtotal,
            total = totales.total,
            montoRecibido = efectivoRecibido,
            uuidIdempotencia = idempotenciaKey,
            detalleVenta = detalles,
            pago = pagoRequest
        )

        val nombreMetodo = metodo.nombre?.takeIf { it.isNotBlank() } ?: "Pago"
        val concepto = if (items.size == 1) {
            items.first().nombre
        } else {
            "Venta POS (${items.sumOf { it.cantidad }} artículos)"
        }.take(240)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearVenta(request)
                val venta = response.dataExitosa()

                if (!isAdded || view == null) return@launch

                if (venta == null) {
                    idempotenciaKey = UUID.randomUUID().toString()
                    mostrarError(
                        response.mensajeDeError()
                            ?: response.body()?.message
                            ?: "No se pudo registrar la venta."
                    )
                    return@launch
                }

                val transaccionTicket = TransaccionPagoDto(
                    id = venta.folioVenta?.id ?: venta.id,
                    uuidIdempotencia = idempotenciaKey,
                    proveedor = metodo.codigo ?: nombreMetodo,
                    monto = venta.total,
                    estado = venta.estado ?: "completada",
                    concepto = concepto,
                    folioVoucher = folioVoucher ?: venta.folioVenta?.id?.let { "Folio #$it" },
                    createdAt = venta.createdAt
                )

                val itemsJson = construirItemsJson(items)
                CartManager.vaciarCarrito(requireContext())

                Toast.makeText(
                    requireContext(),
                    "Venta registrada y descontada del inventario.",
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
                            transaccion = transaccionTicket,
                            itemsJson = itemsJson,
                            metodoPago = nombreMetodo,
                            subtotal = if (venta.subtotal > 0.0) venta.subtotal else totales.subtotal,
                            iva = if (venta.subtotal > 0.0) (venta.total - venta.subtotal).coerceAtLeast(0.0) else totales.iva
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            } catch (e: Exception) {
                if (isAdded && view != null) mostrarError(e.mensajeUsuario())
            } finally {
                cobrando = false
                if (isAdded && view != null) {
                    binding.btnCobrar.isEnabled = true
                    binding.btnCobrar.text = "Cobrar ${totales.total.formatearMonedaSinSufijo()} MXN"
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

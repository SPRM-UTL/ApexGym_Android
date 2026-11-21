package com.sprm.apexgym.feature.inventario

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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
import com.sprm.apexgym.core.network.api.AjustarInventarioAlmacenRequest
import com.sprm.apexgym.core.network.api.AlmacenDto
import com.sprm.apexgym.core.network.api.CrearAlmacenRequest
import com.sprm.apexgym.core.network.api.InventarioAlmacenProductoDto
import com.sprm.apexgym.core.network.api.TransferirStockAlmacenRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.network.mensajeUsuario
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.databinding.FragmentAlmacenesBinding
import com.sprm.apexgym.databinding.ItemInventarioAlmacenBinding
import kotlinx.coroutines.launch

/**
 * Gestión multialmacén de la sucursal activa (`x-sucursal-id`):
 * lista almacenes, consulta existencias por almacén, permite crear nuevos almacenes,
 * ajustar stock y transferir unidades entre almacenes de la misma sucursal.
 */
class AlmacenesFragment : BaseFragment<FragmentAlmacenesBinding>() {

    private var almacenes: List<AlmacenDto> = emptyList()
    private var almacenSeleccionadoId: Int? = null
    private var inventarioActual: List<AlmacenStockLogic.ItemStockAlmacen> = emptyList()
    private var textoBusqueda: String = ""
    private var soloStockBajo: Boolean = false

    private lateinit var inventarioAdapter: InventarioAlmacenAdapter

    override fun getLayoutRes(): Int = R.layout.fragment_almacenes

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flRefresh.setOnClickListener { cargarAlmacenes() }

        binding.tvAlmacenesSucursal.text =
            "SUCURSAL • ${SessionManager.obtenerSucursalNombre(requireContext()).uppercase()}"

        inventarioAdapter = InventarioAlmacenAdapter(
            onAjustar = { item -> mostrarDialogoAjustarStock(item) },
            onTransferir = { item -> mostrarDialogoTransferir(productoInicial = item) }
        )

        binding.rvInventarioAlmacen.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            adapter = inventarioAdapter
        }

        binding.etBuscarProductoAlmacen.doAfterTextChanged { editable ->
            textoBusqueda = editable?.toString().orEmpty()
            aplicarFiltroInventario()
        }

        binding.tvFiltroStockBajo.setOnClickListener {
            soloStockBajo = !soloStockBajo
            actualizarEstiloFiltroStockBajo()
            aplicarFiltroInventario()
        }

        binding.btnNuevoAlmacen.setOnClickListener { mostrarDialogoCrearAlmacen() }
        binding.btnTransferirStock.setOnClickListener { mostrarDialogoTransferir() }

        actualizarEstiloFiltroStockBajo()
        cargarAlmacenes()
    }

    private fun cargarAlmacenes(seleccionarId: Int? = null) {
        binding.tvInventarioAlmacenVacio.visibility = View.VISIBLE
        binding.tvInventarioAlmacenVacio.text = "Cargando almacenes de la sucursal…"
        binding.rvInventarioAlmacen.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerAlmacenes()
                val lista = response.dataExitosa()
                if (!isAdded || view == null) return@launch

                if (lista == null) {
                    binding.tvInventarioAlmacenVacio.visibility = View.VISIBLE
                    binding.tvInventarioAlmacenVacio.text =
                        response.mensajeDeError() ?: "No se pudieron obtener los almacenes."
                    return@launch
                }

                almacenes = lista.filter { it.activo }
                binding.tvCantidadAlmacenes.text = "${almacenes.size}"

                val idPreferido = seleccionarId ?: almacenSeleccionadoId
                val elegido = almacenes.firstOrNull { it.id == idPreferido }
                    ?: almacenes.firstOrNull { it.esPrincipal }
                    ?: almacenes.firstOrNull()

                almacenSeleccionadoId = elegido?.id
                renderizarChipsAlmacenes()

                if (elegido == null) {
                    binding.tvAlmacenActivoNombre.text = "Sin almacenes activos"
                    binding.tvAlmacenPrincipalBadge.visibility = View.GONE
                    binding.tvAlmacenActivoDetalle.text = "Crea el primer almacén para esta sucursal."
                    binding.tvAlmacenResumenMetrics.text = "0 productos • 0 unidades"
                    inventarioActual = emptyList()
                    aplicarFiltroInventario()
                } else {
                    actualizarCabeceraAlmacen(elegido)
                    cargarInventarioAlmacen(elegido.id)
                }
            } catch (e: Exception) {
                if (!isAdded || view == null) return@launch
                binding.tvInventarioAlmacenVacio.visibility = View.VISIBLE
                binding.tvInventarioAlmacenVacio.text = e.mensajeUsuario()
                binding.rvInventarioAlmacen.visibility = View.GONE
            }
        }
    }

    private fun renderizarChipsAlmacenes() {
        val context = requireContext()
        binding.llAlmacenesChips.removeAllViews()

        almacenes.forEachIndexed { index, almacen ->
            val seleccionado = almacen.id == almacenSeleccionadoId
            val chip = TextView(context).apply {
                text = if (almacen.esPrincipal) "${almacen.nombre} ★" else almacen.nombre
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(16), 0, dp(16), 0)
                setBackgroundResource(
                    if (seleccionado) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
                )
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (seleccionado) R.color.white else R.color.apex_text_primary
                    )
                )
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(38)
                ).apply {
                    if (index > 0) marginStart = dp(8)
                }
                setOnClickListener {
                    if (almacenSeleccionadoId != almacen.id) {
                        almacenSeleccionadoId = almacen.id
                        renderizarChipsAlmacenes()
                        actualizarCabeceraAlmacen(almacen)
                        cargarInventarioAlmacen(almacen.id)
                    }
                }
            }
            binding.llAlmacenesChips.addView(chip)
        }
    }

    private fun actualizarCabeceraAlmacen(almacen: AlmacenDto) {
        binding.tvAlmacenActivoNombre.text = almacen.nombre
        binding.tvAlmacenPrincipalBadge.visibility =
            if (almacen.esPrincipal) View.VISIBLE else View.GONE
        binding.tvAlmacenActivoDetalle.text = buildString {
            append(almacen.tipo.ifBlank { "ALMACÉN" })
            almacen.codigo?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
            almacen.ubicacion?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
        }
        binding.tvAlmacenResumenMetrics.text =
            "${almacen.totalProductos} productos • ${almacen.totalUnidades} unidades • ${almacen.productosStockBajo} stock bajo"
    }

    private fun cargarInventarioAlmacen(almacenId: Int) {
        binding.tvInventarioAlmacenVacio.visibility = View.VISIBLE
        binding.tvInventarioAlmacenVacio.text = "Cargando inventario del almacén…"
        binding.rvInventarioAlmacen.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerInventarioAlmacen(almacenId)
                val payload = response.dataExitosa()
                if (!isAdded || view == null) return@launch

                if (payload == null) {
                    inventarioActual = emptyList()
                    binding.tvInventarioAlmacenVacio.visibility = View.VISIBLE
                    binding.tvInventarioAlmacenVacio.text =
                        response.mensajeDeError() ?: "No se pudo cargar el inventario del almacén."
                    binding.rvInventarioAlmacen.visibility = View.GONE
                    return@launch
                }

                payload.almacen?.let { actualizarCabeceraAlmacen(it) }
                inventarioActual = payload.productos.map { it.toStockItem() }
                val resumen = AlmacenStockLogic.calcularResumenAlmacen(inventarioActual)
                binding.tvAlmacenResumenMetrics.text =
                    "${resumen.totalProductos} productos • ${resumen.totalUnidades} unidades • ${resumen.productosStockBajo} stock bajo"

                aplicarFiltroInventario()
            } catch (e: Exception) {
                if (!isAdded || view == null) return@launch
                inventarioActual = emptyList()
                binding.tvInventarioAlmacenVacio.visibility = View.VISIBLE
                binding.tvInventarioAlmacenVacio.text = e.mensajeUsuario()
                binding.rvInventarioAlmacen.visibility = View.GONE
            }
        }
    }

    private fun aplicarFiltroInventario() {
        val filtrados = AlmacenStockLogic.filtrarInventario(
            items = inventarioActual,
            busqueda = textoBusqueda,
            soloStockBajo = soloStockBajo
        )
        inventarioAdapter.actualizar(filtrados)

        val vacio = filtrados.isEmpty()
        binding.tvInventarioAlmacenVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        binding.rvInventarioAlmacen.visibility = if (vacio) View.GONE else View.VISIBLE
        if (vacio) {
            binding.tvInventarioAlmacenVacio.text = when {
                inventarioActual.isEmpty() -> "No hay productos en este almacén."
                soloStockBajo -> "No hay productos con stock bajo en este filtro."
                else -> "No hay productos que coincidan con \"$textoBusqueda\"."
            }
        }
    }

    private fun actualizarEstiloFiltroStockBajo() {
        val context = requireContext()
        binding.tvFiltroStockBajo.setBackgroundResource(
            if (soloStockBajo) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )
        binding.tvFiltroStockBajo.setTextColor(
            ContextCompat.getColor(
                context,
                if (soloStockBajo) R.color.white else R.color.apex_text_primary
            )
        )
    }

    private fun mostrarDialogoCrearAlmacen() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }

        val etNombre = crearEditText("Nombre del almacén *", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val etCodigo = crearEditText("Código (opcional, ej. BOD-02)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)
        val etUbicacion = crearEditText("Ubicación (opcional)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

        val tipos = listOf("BODEGA", "PRINCIPAL", "MOSTRADOR", "SECUNDARIO")
        val spTipo = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, tipos)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { topMargin = dp(10) }
        }
        val cbPrincipal = CheckBox(context).apply {
            text = "Marcar como almacén principal"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }

        container.addView(etNombre)
        container.addView(etCodigo)
        container.addView(etUbicacion)
        container.addView(crearLabelDialogo("Tipo de almacén"))
        container.addView(spTipo)
        container.addView(cbPrincipal)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Nuevo almacén")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear", null)
            .create()

        dialog.setOnShowListener {
            val btnCrear = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            btnCrear.setOnClickListener {
                val nombre = etNombre.text?.toString()?.trim().orEmpty()
                val codigo = etCodigo.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                val ubicacion = etUbicacion.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                val tipo = tipos[spTipo.selectedItemPosition]
                val esPrincipal = cbPrincipal.isChecked

                val validacion = AlmacenStockLogic.validarCreacionAlmacen(
                    nombre = nombre,
                    codigo = codigo,
                    ubicacion = ubicacion,
                    nombresExistentes = almacenes.map { it.nombre }.toSet()
                )
                if (!validacion.valido) {
                    Toast.makeText(context, validacion.mensajeError, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnCrear.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.empleadoService.crearAlmacen(
                            CrearAlmacenRequest(
                                nombre = nombre,
                                codigo = codigo,
                                tipo = tipo,
                                ubicacion = ubicacion,
                                esPrincipal = esPrincipal
                            )
                        )
                        val creado = response.dataExitosa()
                        if (!isAdded) return@launch
                        if (creado == null) {
                            btnCrear.isEnabled = true
                            Toast.makeText(
                                context,
                                response.mensajeDeError() ?: "No se pudo crear el almacén",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                        dialog.dismiss()
                        Toast.makeText(context, "Almacén \"${creado.nombre}\" creado", Toast.LENGTH_SHORT).show()
                        cargarAlmacenes(seleccionarId = creado.id)
                    } catch (e: Exception) {
                        if (isAdded) {
                            btnCrear.isEnabled = true
                            Toast.makeText(context, e.mensajeUsuario(), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoAjustarStock(item: AlmacenStockLogic.ItemStockAlmacen) {
        val almacenId = almacenSeleccionadoId ?: return
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }

        container.addView(crearLabelDialogo("Stock actual en este almacén *"))
        val etStockActual = crearEditText("0", InputType.TYPE_CLASS_NUMBER).apply {
            setText(item.stockActual.toString())
        }
        container.addView(etStockActual)

        container.addView(crearLabelDialogo("Stock mínimo"))
        val etStockMinimo = crearEditText("1", InputType.TYPE_CLASS_NUMBER).apply {
            setText(item.stockMinimo.toString())
        }
        container.addView(etStockMinimo)

        container.addView(crearLabelDialogo("Ubicación / estante (opcional)"))
        val etEstante = crearEditText("Ej. Pasillo A - Nivel 2", InputType.TYPE_CLASS_TEXT).apply {
            setText(item.ubicacionEstante.orEmpty())
        }
        container.addView(etEstante)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Ajustar: ${item.nombre}")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            val btnGuardar = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            btnGuardar.setOnClickListener {
                val stockActual = etStockActual.text?.toString()?.trim()?.toIntOrNull()
                val stockMinimo = etStockMinimo.text?.toString()?.trim()?.toIntOrNull() ?: item.stockMinimo
                val estante = etEstante.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

                val validacion = AlmacenStockLogic.validarAjusteStock(
                    stockActual = stockActual,
                    stockMinimo = stockMinimo,
                    ubicacionEstante = estante
                )
                if (!validacion.valido || stockActual == null) {
                    Toast.makeText(context, validacion.mensajeError ?: "Datos inválidos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnGuardar.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.empleadoService.ajustarInventarioAlmacen(
                            id = almacenId,
                            productoId = item.productoId,
                            request = AjustarInventarioAlmacenRequest(
                                stockActual = stockActual,
                                stockMinimo = stockMinimo,
                                ubicacionEstante = estante
                            )
                        )
                        val actualizado = response.dataExitosa()
                        if (!isAdded) return@launch
                        if (actualizado == null) {
                            btnGuardar.isEnabled = true
                            Toast.makeText(
                                context,
                                response.mensajeDeError() ?: "No se pudo ajustar el stock",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                        dialog.dismiss()
                        Toast.makeText(
                            context,
                            "Stock actualizado (${actualizado.stockActual} u. en almacén)",
                            Toast.LENGTH_SHORT
                        ).show()
                        cargarAlmacenes(seleccionarId = almacenId)
                    } catch (e: Exception) {
                        if (isAdded) {
                            btnGuardar.isEnabled = true
                            Toast.makeText(context, e.mensajeUsuario(), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoTransferir(productoInicial: AlmacenStockLogic.ItemStockAlmacen? = null) {
        val context = requireContext()
        if (almacenes.size < 2) {
            Toast.makeText(
                context,
                "Necesitas al menos 2 almacenes activos en esta sucursal para transferir stock.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val origenActual = almacenes.firstOrNull { it.id == almacenSeleccionadoId } ?: almacenes.first()
        val destinos = almacenes.filter { it.id != origenActual.id }
        val productosTransferibles = if (productoInicial != null) {
            listOf(productoInicial)
        } else {
            inventarioActual.filter { it.stockActual > 0 }
        }

        if (productosTransferibles.isEmpty()) {
            Toast.makeText(
                context,
                "El almacén seleccionado no tiene unidades disponibles para transferir.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }

        container.addView(crearLabelDialogo("Almacén origen"))
        val tvOrigen = TextView(context).apply {
            text = origenActual.nombre
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        }
        container.addView(tvOrigen)

        container.addView(crearLabelDialogo("Almacén destino"))
        val spDestino = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                destinos.map { it.nombre }
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { topMargin = dp(4) }
        }
        container.addView(spDestino)

        container.addView(crearLabelDialogo("Producto"))
        val spProducto = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                productosTransferibles.map { "${it.nombre} (Disp: ${it.stockActual})" }
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { topMargin = dp(4) }
        }
        container.addView(spProducto)

        val tvDisponible = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }
        container.addView(tvDisponible)

        spProducto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val prod = productosTransferibles.getOrNull(pos)
                tvDisponible.text = "Disponible en ${origenActual.nombre}: ${prod?.stockActual ?: 0} u."
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        container.addView(crearLabelDialogo("Cantidad a transferir"))
        val etCantidad = crearEditText("1", InputType.TYPE_CLASS_NUMBER).apply {
            setText("1")
        }
        container.addView(etCantidad)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Transferir stock")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Transferir", null)
            .create()

        dialog.setOnShowListener {
            val btnTransferir = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            btnTransferir.setOnClickListener {
                val destino = destinos.getOrNull(spDestino.selectedItemPosition)
                val producto = productosTransferibles.getOrNull(spProducto.selectedItemPosition)
                val cantidad = etCantidad.text?.toString()?.trim()?.toIntOrNull()

                val validacion = AlmacenStockLogic.validarTransferencia(
                    almacenOrigenId = origenActual.id,
                    almacenDestinoId = destino?.id,
                    productoId = producto?.productoId,
                    cantidad = cantidad,
                    stockDisponibleOrigen = producto?.stockActual ?: 0
                )
                if (!validacion.valido || destino == null || producto == null || cantidad == null) {
                    Toast.makeText(
                        context,
                        validacion.mensajeError ?: "Completa los datos de transferencia",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                btnTransferir.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.empleadoService.transferirStockAlmacen(
                            TransferirStockAlmacenRequest(
                                origenAlmacenId = origenActual.id,
                                destinoAlmacenId = destino.id,
                                productoId = producto.productoId,
                                cantidad = cantidad
                            )
                        )
                        val resultado = response.dataExitosa()
                        if (!isAdded) return@launch
                        if (resultado == null) {
                            btnTransferir.isEnabled = true
                            Toast.makeText(
                                context,
                                response.mensajeDeError() ?: "No se pudo transferir el stock",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                        dialog.dismiss()
                        Toast.makeText(
                            context,
                            "Transferidas $cantidad u. de ${producto.nombre} a ${destino.nombre}",
                            Toast.LENGTH_LONG
                        ).show()
                        cargarAlmacenes(seleccionarId = origenActual.id)
                    } catch (e: Exception) {
                        if (isAdded) {
                            btnTransferir.isEnabled = true
                            Toast.makeText(context, e.mensajeUsuario(), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun crearLabelDialogo(texto: String): TextView = TextView(requireContext()).apply {
        text = texto
        setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_secondary))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) }
    }

    private fun crearEditText(hint: String, inputType: Int): EditText = EditText(requireContext()).apply {
        this.hint = hint
        this.inputType = inputType
        setTextColor(ContextCompat.getColor(requireContext(), R.color.apex_text_primary))
        setHintTextColor(ContextCompat.getColor(requireContext(), R.color.apex_nav_inactive))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4) }
    }

    private fun InventarioAlmacenProductoDto.toStockItem() = AlmacenStockLogic.ItemStockAlmacen(
        productoId = productoId,
        nombre = nombre,
        codigoBarras = codigoBarras,
        categoria = categoria,
        stockActual = stockActual,
        stockMinimo = stockMinimo,
        stockTotalSucursal = stockGlobal,
        ubicacionEstante = ubicacionEstante,
        precioVenta = precioVenta,
        precioCompra = precioCompra
    )

    private class InventarioAlmacenAdapter(
        private val onAjustar: (AlmacenStockLogic.ItemStockAlmacen) -> Unit,
        private val onTransferir: (AlmacenStockLogic.ItemStockAlmacen) -> Unit
    ) : RecyclerView.Adapter<InventarioAlmacenAdapter.Holder>() {

        private var items: List<AlmacenStockLogic.ItemStockAlmacen> = emptyList()

        fun actualizar(nuevos: List<AlmacenStockLogic.ItemStockAlmacen>) {
            items = nuevos
            notifyDataSetChanged()
        }

        class Holder(val binding: ItemInventarioAlmacenBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemInventarioAlmacenBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            val context = holder.itemView.context

            holder.binding.tvProductoAlmacenNombre.text = item.nombre
            holder.binding.tvProductoAlmacenMeta.text = buildString {
                append(item.categoria?.takeIf { it.isNotBlank() } ?: "Producto")
                item.codigoBarras?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                item.ubicacionEstante?.takeIf { it.isNotBlank() }?.let { append(" • Estante: $it") }
            }

            holder.binding.tvProductoAlmacenStock.text =
                "En almacén: ${item.stockActual} u. • Mín: ${item.stockMinimo}"
            holder.binding.tvProductoAlmacenStock.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.esStockBajo) R.color.apex_orange else R.color.apex_text_primary
                )
            )
            holder.binding.tvProductoAlmacenStockTotal.text =
                "Total sucursal: ${item.stockTotalSucursal} u."

            holder.binding.btnAjustarStockItem.setOnClickListener { onAjustar(item) }
            holder.binding.btnTransferirStockItem.isEnabled = item.stockActual > 0
            holder.binding.btnTransferirStockItem.alpha = if (item.stockActual > 0) 1f else 0.45f
            holder.binding.btnTransferirStockItem.setOnClickListener {
                if (item.stockActual > 0) {
                    onTransferir(item)
                } else {
                    Toast.makeText(
                        context,
                        "Este almacén no tiene stock disponible de este producto.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            holder.binding.vDividerInventarioAlmacen.visibility =
                if (position == items.lastIndex) View.GONE else View.VISIBLE
        }

        override fun getItemCount(): Int = items.size
    }

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()
}

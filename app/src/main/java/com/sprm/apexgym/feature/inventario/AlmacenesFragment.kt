package com.sprm.apexgym.feature.inventario

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
            onAjustar = { item -> mostrarSheetAjustarStock(item) },
            onTransferir = { item -> mostrarSheetTransferir(productoInicial = item) }
        )

        binding.rvInventarioAlmacen.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            adapter = inventarioAdapter
        }

        binding.ivLimpiarBusquedaAlmacen.setOnClickListener {
            binding.etBuscarProductoAlmacen.setText("")
        }
        binding.etBuscarProductoAlmacen.doAfterTextChanged { editable ->
            textoBusqueda = editable?.toString().orEmpty()
            binding.ivLimpiarBusquedaAlmacen.visibility =
                if (textoBusqueda.isNotBlank()) View.VISIBLE else View.GONE
            aplicarFiltroInventario()
        }

        binding.tvFiltroStockBajo.setOnClickListener {
            soloStockBajo = !soloStockBajo
            actualizarEstiloFiltroStockBajo()
            aplicarFiltroInventario()
        }

        binding.btnNuevoAlmacen.setOnClickListener { mostrarSheetCrearAlmacen() }
        binding.btnTransferirStock.setOnClickListener { mostrarSheetTransferir() }

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
                binding.tvCantidadAlmacenes.text = if (almacenes.size == 1) {
                    "1 activo"
                } else {
                    "${almacenes.size} activos"
                }
                binding.btnTransferirStock.alpha = if (almacenes.size >= 2) 1f else 0.55f

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
                    actualizarKpis(0, 0, 0)
                    inventarioActual = emptyList()
                    aplicarFiltroInventario()
                } else {
                    actualizarCabeceraAlmacen(elegido, incluirKpis = true)
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
                text = almacen.nombre
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
                        actualizarCabeceraAlmacen(almacen, incluirKpis = true)
                        cargarInventarioAlmacen(almacen.id)
                    }
                }
            }
            binding.llAlmacenesChips.addView(chip)
        }
    }

    private fun actualizarCabeceraAlmacen(almacen: AlmacenDto, incluirKpis: Boolean) {
        binding.tvAlmacenActivoNombre.text = almacen.nombre
        binding.tvAlmacenPrincipalBadge.visibility =
            if (almacen.esPrincipal) View.VISIBLE else View.GONE
        val tipoLegible = when (almacen.tipo.trim().uppercase()) {
            "PRINCIPAL" -> "Principal"
            "BODEGA" -> "Bodega"
            "MOSTRADOR" -> "Mostrador"
            "SECUNDARIO" -> "Secundario"
            "" -> "Almacén"
            else -> almacen.tipo.lowercase().replaceFirstChar { it.uppercase() }
        }
        binding.tvAlmacenActivoDetalle.text = buildString {
            append(tipoLegible)
            almacen.codigo?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
            almacen.ubicacion?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
        }
        if (incluirKpis) {
            actualizarKpis(
                almacen.totalProductos,
                almacen.totalUnidades,
                almacen.productosStockBajo
            )
        }
    }

    private fun actualizarKpis(productos: Int, unidades: Int, stockBajo: Int) {
        binding.tvKpiAlmacenProductos.text = productos.toString()
        binding.tvKpiAlmacenUnidades.text = unidades.toString()
        binding.tvKpiAlmacenStockBajo.text = stockBajo.toString()
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

                inventarioActual = payload.productos.map { it.toStockItem() }
                val resumen = AlmacenStockLogic.calcularResumenAlmacen(inventarioActual)
                payload.almacen?.let { actualizarCabeceraAlmacen(it, incluirKpis = false) }
                actualizarKpis(
                    resumen.totalProductos,
                    resumen.totalUnidades,
                    resumen.productosStockBajo
                )
                actualizarEstiloFiltroStockBajo(resumen.productosStockBajo)
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

    private fun actualizarEstiloFiltroStockBajo(stockBajoCount: Int? = null) {
        val context = requireContext()
        val count = stockBajoCount ?: inventarioActual.count { it.esStockBajo }
        binding.tvFiltroStockBajo.text = if (count > 0) "Stock bajo ($count)" else "Stock bajo"
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

    private fun crearBottomSheet(layoutRes: Int): Pair<BottomSheetDialog, View> {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(layoutRes, null)
        dialog.setContentView(sheetView)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog to sheetView
    }

    private fun mostrarSheetCrearAlmacen() {
        val context = requireContext()
        val (dialog, sheetView) = crearBottomSheet(R.layout.dialog_almacen_nuevo)

        val btnCerrar = sheetView.findViewById<View>(R.id.btnCerrarNuevoAlmacen)
        val tilNombre = sheetView.findViewById<TextInputLayout>(R.id.tilNombreAlmacen)
        val etNombre = sheetView.findViewById<TextInputEditText>(R.id.etNombreAlmacen)
        val etCodigo = sheetView.findViewById<TextInputEditText>(R.id.etCodigoAlmacen)
        val etUbicacion = sheetView.findViewById<TextInputEditText>(R.id.etUbicacionAlmacen)
        val actvTipo = sheetView.findViewById<AutoCompleteTextView>(R.id.actvTipoAlmacen)
        val cbPrincipal = sheetView.findViewById<MaterialCheckBox>(R.id.cbAlmacenPrincipal)
        val btnGuardar = sheetView.findViewById<MaterialButton>(R.id.btnGuardarNuevoAlmacen)

        val tipos = listOf(
            "Bodega" to "BODEGA",
            "Principal" to "PRINCIPAL",
            "Mostrador" to "MOSTRADOR",
            "Secundario" to "SECUNDARIO"
        )
        var tipoSeleccionado = "BODEGA"
        actvTipo.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, tipos.map { it.first })
        )
        actvTipo.setText("Bodega", false)
        actvTipo.setOnItemClickListener { _, _, pos, _ ->
            tipoSeleccionado = tipos.getOrNull(pos)?.second ?: "BODEGA"
        }

        etNombre.doAfterTextChanged { tilNombre.error = null }
        btnCerrar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text?.toString()?.trim().orEmpty()
            val codigo = etCodigo.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val ubicacion = etUbicacion.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val esPrincipal = cbPrincipal.isChecked

            val validacion = AlmacenStockLogic.validarCreacionAlmacen(
                nombre = nombre,
                codigo = codigo,
                ubicacion = ubicacion,
                nombresExistentes = almacenes.map { it.nombre }.toSet()
            )
            if (!validacion.valido) {
                tilNombre.error = validacion.mensajeError
                return@setOnClickListener
            }

            btnGuardar.isEnabled = false
            btnGuardar.text = "Creando…"
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.empleadoService.crearAlmacen(
                        CrearAlmacenRequest(
                            nombre = nombre,
                            codigo = codigo,
                            tipo = tipoSeleccionado,
                            ubicacion = ubicacion,
                            esPrincipal = esPrincipal
                        )
                    )
                    val creado = response.dataExitosa()
                    if (!isAdded) return@launch
                    if (creado == null) {
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Crear almacén"
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
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Crear almacén"
                        Toast.makeText(context, e.mensajeUsuario(), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun mostrarSheetAjustarStock(item: AlmacenStockLogic.ItemStockAlmacen) {
        val almacenId = almacenSeleccionadoId ?: return
        val context = requireContext()
        val (dialog, sheetView) = crearBottomSheet(R.layout.dialog_almacen_ajustar)

        val btnCerrar = sheetView.findViewById<View>(R.id.btnCerrarAjusteAlmacen)
        val tvTitulo = sheetView.findViewById<TextView>(R.id.tvTituloAjusteAlmacen)
        val tilStockActual = sheetView.findViewById<TextInputLayout>(R.id.tilStockActualAjuste)
        val etStockActual = sheetView.findViewById<TextInputEditText>(R.id.etStockActualAjuste)
        val tilStockMinimo = sheetView.findViewById<TextInputLayout>(R.id.tilStockMinimoAjuste)
        val etStockMinimo = sheetView.findViewById<TextInputEditText>(R.id.etStockMinimoAjuste)
        val etEstante = sheetView.findViewById<TextInputEditText>(R.id.etEstanteAjuste)
        val btnGuardar = sheetView.findViewById<MaterialButton>(R.id.btnGuardarAjusteAlmacen)

        tvTitulo.text = item.nombre
        etStockActual.setText(item.stockActual.toString())
        etStockMinimo.setText(item.stockMinimo.toString())
        etEstante.setText(item.ubicacionEstante.orEmpty())

        etStockActual.doAfterTextChanged { tilStockActual.error = null }
        etStockMinimo.doAfterTextChanged { tilStockMinimo.error = null }
        btnCerrar.setOnClickListener { dialog.dismiss() }

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
                tilStockActual.error = validacion.mensajeError ?: "Ingresa un stock válido"
                return@setOnClickListener
            }

            btnGuardar.isEnabled = false
            btnGuardar.text = "Guardando…"
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
                        btnGuardar.text = "Guardar cambios"
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
                        btnGuardar.text = "Guardar cambios"
                        Toast.makeText(context, e.mensajeUsuario(), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun mostrarSheetTransferir(productoInicial: AlmacenStockLogic.ItemStockAlmacen? = null) {
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

        val (dialog, sheetView) = crearBottomSheet(R.layout.dialog_almacen_transferir)

        val btnCerrar = sheetView.findViewById<View>(R.id.btnCerrarTransferirAlmacen)
        val tvOrigen = sheetView.findViewById<TextView>(R.id.tvOrigenTransferencia)
        val actvDestino = sheetView.findViewById<AutoCompleteTextView>(R.id.actvDestinoTransferencia)
        val actvProducto = sheetView.findViewById<AutoCompleteTextView>(R.id.actvProductoTransferencia)
        val tvDisponible = sheetView.findViewById<TextView>(R.id.tvDisponibleTransferencia)
        val tilCantidad = sheetView.findViewById<TextInputLayout>(R.id.tilCantidadTransferencia)
        val etCantidad = sheetView.findViewById<TextInputEditText>(R.id.etCantidadTransferencia)
        val btnConfirmar = sheetView.findViewById<MaterialButton>(R.id.btnConfirmarTransferencia)

        tvOrigen.text = origenActual.nombre

        var destinoSeleccionado = destinos.first()
        actvDestino.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, destinos.map { it.nombre })
        )
        actvDestino.setText(destinoSeleccionado.nombre, false)
        actvDestino.setOnItemClickListener { _, _, pos, _ ->
            destinoSeleccionado = destinos[pos]
        }

        var productoSeleccionado = productosTransferibles.first()
        val nombresProductos = productosTransferibles.map { "${it.nombre} (${it.stockActual} u.)" }
        actvProducto.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, nombresProductos)
        )
        actvProducto.setText(nombresProductos.first(), false)
        tvDisponible.text = "Disponible en ${origenActual.nombre}: ${productoSeleccionado.stockActual} unidades"

        actvProducto.setOnItemClickListener { _, _, pos, _ ->
            productoSeleccionado = productosTransferibles[pos]
            tvDisponible.text = "Disponible en ${origenActual.nombre}: ${productoSeleccionado.stockActual} unidades"
            tilCantidad.error = null
        }

        etCantidad.doAfterTextChanged { tilCantidad.error = null }
        btnCerrar.setOnClickListener { dialog.dismiss() }

        btnConfirmar.setOnClickListener {
            val cantidad = etCantidad.text?.toString()?.trim()?.toIntOrNull()
            val validacion = AlmacenStockLogic.validarTransferencia(
                almacenOrigenId = origenActual.id,
                almacenDestinoId = destinoSeleccionado.id,
                productoId = productoSeleccionado.productoId,
                cantidad = cantidad,
                stockDisponibleOrigen = productoSeleccionado.stockActual
            )
            if (!validacion.valido || cantidad == null) {
                tilCantidad.error = validacion.mensajeError ?: "Cantidad inválida"
                return@setOnClickListener
            }

            btnConfirmar.isEnabled = false
            btnConfirmar.text = "Transfiriendo…"
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.empleadoService.transferirStockAlmacen(
                        TransferirStockAlmacenRequest(
                            origenAlmacenId = origenActual.id,
                            destinoAlmacenId = destinoSeleccionado.id,
                            productoId = productoSeleccionado.productoId,
                            cantidad = cantidad
                        )
                    )
                    val resultado = response.dataExitosa()
                    if (!isAdded) return@launch
                    if (resultado == null) {
                        btnConfirmar.isEnabled = true
                        btnConfirmar.text = "Transferir stock"
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
                        "Transferidas $cantidad u. de ${productoSeleccionado.nombre} a ${destinoSeleccionado.nombre}",
                        Toast.LENGTH_LONG
                    ).show()
                    cargarAlmacenes(seleccionarId = origenActual.id)
                } catch (e: Exception) {
                    if (isAdded) {
                        btnConfirmar.isEnabled = true
                        btnConfirmar.text = "Transferir stock"
                        Toast.makeText(context, e.mensajeUsuario(), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
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

            val (badgeTexto, bgHex, txtHex) = when {
                item.stockActual <= 0 -> Triple("Sin stock", "#FEE2E2", "#991B1B")
                item.esStockBajo -> Triple("Stock bajo", "#FFEDD5", "#9A3412")
                else -> Triple("Disponible", "#DCFCE7", "#166534")
            }
            holder.binding.tvBadgeEstadoAlmacen.text = badgeTexto
            holder.binding.tvBadgeEstadoAlmacen.setTextColor(Color.parseColor(txtHex))
            holder.binding.tvBadgeEstadoAlmacen.background = GradientDrawable().apply {
                cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 100f, context.resources.displayMetrics
                )
                setColor(Color.parseColor(bgHex))
            }

            holder.binding.tvProductoAlmacenStock.text =
                "En almacén: ${item.stockActual} u. (Mín: ${item.stockMinimo})"
            holder.binding.tvProductoAlmacenStock.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.esStockBajo) R.color.apex_orange else R.color.apex_text_primary
                )
            )
            holder.binding.tvProductoAlmacenStockTotal.text =
                "Sucursal: ${item.stockTotalSucursal} u."

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
        }

        override fun getItemCount(): Int = items.size
    }

    private fun dp(valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        resources.displayMetrics
    ).toInt()
}

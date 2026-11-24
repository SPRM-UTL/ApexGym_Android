package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.ProductoClienteDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.feature.tienda.CalculoVentaPos
import com.sprm.apexgym.feature.tienda.CarritoBottomSheetFragment
import com.sprm.apexgym.feature.main.CartBottomSheetFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class ProductDetailBottomSheetFragment : BottomSheetDialogFragment() {

    private var cantidadSeleccionada = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_product_detail_sheet, container, false)
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

        val args = arguments ?: Bundle()
        val idProducto = args.getInt(ARG_ID, 0)
        val nombre = args.getString(ARG_NOMBRE, "Producto ApexGym")
        val categoria = args.getString(ARG_CATEGORIA, "General")
        val codigoBarras = args.getString(ARG_CODIGO, "")
        val descripcion = args.getString(ARG_DESCRIPCION, "")
        val precioVenta = args.getDouble(ARG_PRECIO, 0.0)
        val descuentoPorcentaje = args.getDouble(ARG_DESCUENTO, 0.0)
        val precioUnitarioNeto = CalculoVentaPos.precioConDescuento(precioVenta, descuentoPorcentaje)
        val stockActual = args.getInt(ARG_STOCK, 0)
        val stockMinimo = args.getInt(ARG_STOCK_MINIMO, 1)
        val imagenUrl = args.getString(ARG_IMAGEN_URL, null)

        val tvDetailProdCategoria = view.findViewById<TextView>(R.id.tvDetailProdCategoria)
        val btnCerrarProductDetail = view.findViewById<View>(R.id.btnCerrarProductDetail)
        val flDetailProdImageContainer = view.findViewById<FrameLayout>(R.id.flDetailProdImageContainer)
        val tvDetailProdStockBadge = view.findViewById<TextView>(R.id.tvDetailProdStockBadge)
        val tvDetailProdNombre = view.findViewById<TextView>(R.id.tvDetailProdNombre)
        val tvDetailProdCodigo = view.findViewById<TextView>(R.id.tvDetailProdCodigo)
        val tvDetailProdPrecioUnitario = view.findViewById<TextView>(R.id.tvDetailProdPrecioUnitario)
        val tvDetailProdStockValor = view.findViewById<TextView>(R.id.tvDetailProdStockValor)
        val tvDetailProdDescripcion = view.findViewById<TextView>(R.id.tvDetailProdDescripcion)
        val btnProdCantidadMenos = view.findViewById<TextView>(R.id.btnProdCantidadMenos)
        val tvProdCantidadValor = view.findViewById<TextView>(R.id.tvProdCantidadValor)
        val btnProdCantidadMas = view.findViewById<TextView>(R.id.btnProdCantidadMas)
        val btnConfirmarProducto = view.findViewById<MaterialButton>(R.id.btnConfirmarProducto)

        tvDetailProdCategoria?.text = categoria.uppercase(Locale.getDefault())
        tvDetailProdNombre?.text = nombre
        tvDetailProdCodigo?.text = if (!codigoBarras.isNullOrBlank()) {
            "Código: $codigoBarras"
        } else {
            "Catálogo oficial ApexGym"
        }
        tvDetailProdPrecioUnitario?.text = if (descuentoPorcentaje > 0.0) {
            "${formatearPrecio(precioUnitarioNeto)} (-${descuentoPorcentaje.toInt()}%)"
        } else {
            formatearPrecio(precioUnitarioNeto)
        }

        when {
            stockActual <= 0 -> {
                tvDetailProdStockBadge?.text = "Agotado temporalmente"
                tvDetailProdStockBadge?.setBackgroundResource(R.drawable.bg_badge_orange)
                tvDetailProdStockValor?.text = "Sin existencias"
            }
            stockActual <= stockMinimo -> {
                tvDetailProdStockBadge?.text = "Últimas $stockActual unidades"
                tvDetailProdStockBadge?.setBackgroundResource(R.drawable.bg_badge_orange)
                tvDetailProdStockValor?.text = "$stockActual disponibles"
            }
            else -> {
                tvDetailProdStockBadge?.text = "En stock • Disponible"
                tvDetailProdStockBadge?.setBackgroundResource(R.drawable.bg_badge_green)
                tvDetailProdStockValor?.text = "$stockActual disponibles"
            }
        }

        val esEmpleado = SessionManager.esEmpleado(requireContext())
        tvDetailProdDescripcion?.text = if (!descripcion.isNullOrBlank()) {
            descripcion
        } else if (esEmpleado) {
            "Producto disponible para venta en mostrador de sucursal ApexGym."
        } else {
            "Producto disponible directamente en la recepción de tu sucursal ApexGym. Solicítalo al personal en mostrador."
        }

        flDetailProdImageContainer?.clipToOutline = true
        if (!imagenUrl.isNullOrBlank() && flDetailProdImageContainer != null) {
            cargarImagenProducto(imagenUrl, flDetailProdImageContainer)
        }

        val maxCantidad = if (stockActual > 0) stockActual else 1

        fun actualizarTotal() {
            tvProdCantidadValor?.text = cantidadSeleccionada.toString()
            if (stockActual <= 0) {
                btnConfirmarProducto?.isEnabled = false
                btnConfirmarProducto?.text = "Agotado"
                btnConfirmarProducto?.alpha = 0.6f
            } else {
                btnConfirmarProducto?.isEnabled = true
                btnConfirmarProducto?.alpha = 1f
                btnConfirmarProducto?.text = "Agregar"
            }
        }

        actualizarTotal()

        btnProdCantidadMenos?.setOnClickListener {
            if (cantidadSeleccionada > 1) {
                cantidadSeleccionada--
                actualizarTotal()
            }
        }

        btnProdCantidadMas?.setOnClickListener {
            if (cantidadSeleccionada < maxCantidad) {
                cantidadSeleccionada++
                actualizarTotal()
            } else if (stockActual > 0) {
                Toast.makeText(
                    requireContext(),
                    "Stock máximo disponible: $stockActual unidades",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnCerrarProductDetail?.setOnClickListener {
            dismiss()
        }

        btnConfirmarProducto?.setOnClickListener {

            val productoDto = ProductoClienteDto(
                id = idProducto,
                nombre = nombre,
                codigoBarras = codigoBarras,
                descripcion = descripcion,
                precioVenta = precioVenta,
                descuentoPorcentaje = descuentoPorcentaje,
                stockActual = stockActual,
                stockMinimo = stockMinimo,
                imagenUrl = imagenUrl,
                categoria = com.sprm.apexgym.core.network.client.CategoriaProductoDto(
                    id = 0,
                    nombre = categoria
                )
            )

            val resultado = CartManager.agregarProducto(
                context = requireContext(),
                producto = productoDto,
                cantidadAgregar = cantidadSeleccionada
            )

            Toast.makeText(
                requireContext(),
                resultado.mensaje,
                Toast.LENGTH_SHORT
            ).show()

            if (!resultado.exito) return@setOnClickListener

            val fm = try { requireActivity().supportFragmentManager } catch (_: Exception) { parentFragmentManager }
            dismiss()

            if (esEmpleado) {
                val existing = fm.findFragmentByTag(CarritoBottomSheetFragment.TAG)
                if (existing == null) {
                    CarritoBottomSheetFragment().show(fm, CarritoBottomSheetFragment.TAG)
                }
            } else {
                val existing = fm.findFragmentByTag(CartBottomSheetFragment.TAG)
                if (existing == null) {
                    CartBottomSheetFragment().show(fm, CartBottomSheetFragment.TAG)
                }
            }
        }
    }

    private fun cargarImagenProducto(rutaImagen: String, contenedor: FrameLayout) {
        val urlCompleta = RetrofitClient.resolverUrlImagen(rutaImagen) ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = try {
                if (urlCompleta.startsWith("data:image")) {
                    val base64Part = urlCompleta.substringAfter("base64,", "")
                    if (base64Part.isNotEmpty()) {
                        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                } else {
                    URL(urlCompleta).openStream().use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
            } catch (_: Exception) {
                null
            }

            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        contenedor.removeAllViews()
                        val imageView = ImageView(requireContext()).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageBitmap(bitmap)
                        }
                        contenedor.addView(imageView)
                    }
                }
            }
        }
    }

    private fun formatearPrecio(monto: Double): String {
        val formato = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (monto % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$${formato.format(monto)}"
    }

    companion object {
        const val TAG = "ProductDetailBottomSheet"
        private const val ARG_ID = "arg_id"
        private const val ARG_NOMBRE = "arg_nombre"
        private const val ARG_CATEGORIA = "arg_categoria"
        private const val ARG_CODIGO = "arg_codigo"
        private const val ARG_DESCRIPCION = "arg_descripcion"
        private const val ARG_PRECIO = "arg_precio"
        private const val ARG_DESCUENTO = "arg_descuento"
        private const val ARG_STOCK = "arg_stock"
        private const val ARG_STOCK_MINIMO = "arg_stock_minimo"
        private const val ARG_IMAGEN_URL = "arg_imagen_url"

        fun newInstance(producto: ProductoClienteDto): ProductDetailBottomSheetFragment {
            return ProductDetailBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, producto.id)
                    putString(ARG_NOMBRE, producto.nombre)
                    putString(ARG_CATEGORIA, producto.categoria?.nombre ?: "General")
                    putString(ARG_CODIGO, producto.codigoBarras)
                    putString(ARG_DESCRIPCION, producto.descripcion)
                    putDouble(ARG_PRECIO, producto.precioVenta)
                    putDouble(ARG_DESCUENTO, producto.descuentoPorcentaje)
                    putInt(ARG_STOCK, producto.stockActual)
                    putInt(ARG_STOCK_MINIMO, producto.stockMinimo)
                    putString(ARG_IMAGEN_URL, producto.imagenUrl)
                }
            }
        }
    }
}

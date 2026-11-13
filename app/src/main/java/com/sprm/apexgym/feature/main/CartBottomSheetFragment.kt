package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class CartBottomSheetFragment : BottomSheetDialogFragment() {

    private var cartListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_carrito_bottom_sheet, container, false)
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

        val btnCloseCart = view.findViewById<View>(R.id.btnCloseCart)
        val btnProcederPago = view.findViewById<MaterialButton>(R.id.btnProcederPago)

        btnCloseCart?.setOnClickListener {
            dismiss()
        }

        btnProcederPago?.setOnClickListener {
            val context = requireContext()
            val items = CartManager.obtenerItems(context)
            if (items.isEmpty()) return@setOnClickListener

            dismiss()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CheckoutFragment())
                .addToBackStack("checkout")
                .commit()
        }

        val listener: () -> Unit = {
            if (isAdded && this.view != null) {
                renderizarCarrito(requireView())
            }
        }
        cartListener = listener
        CartManager.addListener(listener)

        renderizarCarrito(view)
    }

    private fun renderizarCarrito(rootView: View) {
        val context = requireContext()
        val items = CartManager.obtenerItems(context)

        val tvCartItemCount = rootView.findViewById<TextView>(R.id.tvCartItemCount)
        val llEmptyCart = rootView.findViewById<View>(R.id.llEmptyCart)
        val llCartItemsContainer = rootView.findViewWithTag<LinearLayout>("cart_items_container")
        val llCartSummarySection = rootView.findViewWithTag<View>("cart_summary_section")
        val tvTotal = rootView.findViewById<TextView>(R.id.tvTotal)
        val btnProcederPago = rootView.findViewById<MaterialButton>(R.id.btnProcederPago)

        llCartItemsContainer?.removeAllViews()

        if (items.isEmpty()) {
            tvCartItemCount?.text = "0 artículos seleccionados"
            llEmptyCart?.visibility = View.VISIBLE
            llCartItemsContainer?.visibility = View.GONE
            llCartSummarySection?.visibility = View.GONE
            return
        }

        val totalUnidades = CartManager.obtenerCantidadTotalArticulos(context)
        tvCartItemCount?.text = if (totalUnidades == 1) {
            "1 artículo seleccionado"
        } else {
            "$totalUnidades artículos seleccionados"
        }

        llEmptyCart?.visibility = View.GONE
        llCartItemsContainer?.visibility = View.VISIBLE
        llCartSummarySection?.visibility = View.VISIBLE

        items.forEach { item ->
            llCartItemsContainer?.addView(crearTarjetaItemCarrito(item))
        }

        val total = CartManager.obtenerTotalPrecio(context)
        tvTotal?.text = "${formatearPrecio(total)} MXN"
        btnProcederPago?.text = "Continuar"
    }

    private fun crearTarjetaItemCarrito(item: CartItem): View {
        val context = requireContext()
        val esMembresia = item.tipo == CartItemType.MEMBRESIA

        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            radius = dpToPx(16).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1f)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        val mediaBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
            setBackgroundResource(R.drawable.bg_chip_unselected)
            clipToOutline = true

            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(22), dpToPx(22), Gravity.CENTER)
                setImageResource(
                    if (esMembresia) R.drawable.ic_membership_card
                    else R.drawable.ic_supplement_bottle
                )
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_orange))
                )
            }
            addView(icon)
        }

        if (!esMembresia && !item.imagenUrl.isNullOrBlank()) {
            cargarImagenItem(item.imagenUrl, mediaBox)
        }
        row.addView(mediaBox)

        val infoCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
                marginEnd = dpToPx(8)
            }
        }

        val tvCategoria = TextView(context).apply {
            text = if (esMembresia) {
                "Membresía • ${item.subtitulo}"
            } else {
                "Producto • ${formatearPrecio(item.precioUnitario)} c/u"
            }
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        infoCol.addView(tvCategoria)

        val tvNombre = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            text = item.nombre
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        infoCol.addView(tvNombre)

        val tvPrecioSubtotal = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(3)
            }
            text = "${formatearPrecio(item.subtotal)} MXN"
            setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
        }
        infoCol.addView(tvPrecioSubtotal)

        row.addView(infoCol)

        val actionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        if (!esMembresia) {
            val stepperLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_chip_unselected)
                setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(6)
                }
            }

            val btnMenos = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
                gravity = Gravity.CENTER
                text = "−"
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    CartManager.cambiarCantidadProducto(context, item.id, -1)
                }
            }
            stepperLayout.addView(btnMenos)

            val tvCantidad = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(24), ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
                text = item.cantidad.toString()
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
            }
            stepperLayout.addView(tvCantidad)

            val btnMas = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
                gravity = Gravity.CENTER
                text = "+"
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val res = CartManager.cambiarCantidadProducto(context, item.id, 1)
                    if (!res.exito) {
                        Toast.makeText(context, res.mensaje, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            stepperLayout.addView(btnMas)
            actionsRow.addView(stepperLayout)
        }

        val btnDelete = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            setBackgroundResource(R.drawable.bg_chip_unselected)
            isClickable = true
            isFocusable = true

            val deleteIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(15), dpToPx(15), Gravity.CENTER)
                setImageResource(R.drawable.ic_delete)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_text_secondary))
                )
            }
            addView(deleteIcon)

            setOnClickListener {
                CartManager.eliminarItem(context, item.tipo, item.id)
            }
        }
        actionsRow.addView(btnDelete)

        row.addView(actionsRow)
        card.addView(row)
        return card
    }

    private fun cargarImagenItem(rutaImagen: String, contenedor: FrameLayout) {
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

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroyView() {
        cartListener?.let { CartManager.removeListener(it) }
        cartListener = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "CartBottomSheet"
    }
}

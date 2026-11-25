package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.api.SucursalDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.session.SessionManager
import kotlinx.coroutines.launch

class SucursalSelectorBottomSheetFragment : BottomSheetDialogFragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val rootScroll = NestedScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.bg_bottom_sheet_rounded)
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(28))
        }

        val handle = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(4)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setBackgroundResource(R.drawable.bg_drag_handle)
        }
        root.addView(handle)

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16)
            }
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvTitle = TextView(context).apply {
            text = "Seleccionar sucursal"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
        }
        titleCol.addView(tvTitle)

        val tvSub = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            text = "Todos los módulos mostrarán datos de la sucursal elegida"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        }
        titleCol.addView(tvSub)
        headerRow.addView(titleCol)

        val btnClose = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setBackgroundResource(R.drawable.bg_circle_white_elevated)
            isClickable = true
            isFocusable = true

            val closeIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(18), dpToPx(18), Gravity.CENTER)
                setImageResource(R.drawable.ic_close)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.apex_text_primary))
                )
            }
            addView(closeIcon)
            setOnClickListener { dismiss() }
        }
        headerRow.addView(btnClose)
        root.addView(headerRow)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(14)
                bottomMargin = dpToPx(14)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.apex_stroke_light))
        }
        root.addView(divider)

        val progress = ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(16)
                bottomMargin = dpToPx(16)
            }
            indeterminateTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.apex_orange)
            )
        }
        root.addView(progress)

        val listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(listContainer)

        rootScroll.addView(root)

        cargarSucursales(progress, listContainer)

        return rootScroll
    }

    private fun cargarSucursales(progress: ProgressBar, container: LinearLayout) {
        viewLifecycleOwner.lifecycleScope.launch {
            val lista = try {
                val disponibles = RetrofitClient.empleadoService.obtenerSucursalesDisponibles()
                    .dataExitosa()
                    .orEmpty()
                    .filter { it.activo }
                if (disponibles.isNotEmpty()) {
                    disponibles
                } else {
                    val todas = RetrofitClient.empleadoService.obtenerSucursales()
                        .dataExitosa()
                        .orEmpty()
                        .filter { it.activo }
                    if (todas.isNotEmpty()) {
                        todas
                    } else {
                        listOfNotNull(
                            RetrofitClient.empleadoService.obtenerSucursalActual().dataExitosa()
                        )
                    }
                }
            } catch (_: Exception) {
                try {
                    listOfNotNull(
                        RetrofitClient.empleadoService.obtenerSucursalActual().dataExitosa()
                    )
                } catch (_: Exception) {
                    emptyList()
                }
            }

            if (!isAdded) return@launch
            progress.visibility = View.GONE
            container.removeAllViews()

            val sucursalesFinales = if (lista.isNotEmpty()) {
                lista
            } else {
                listOf(
                    SucursalDto(
                        id = SessionManager.obtenerSucursalId(requireContext()),
                        nombre = SessionManager.obtenerSucursalNombre(requireContext()),
                        codigo = SessionManager.obtenerSucursalCodigo(requireContext()),
                        ciudad = SessionManager.obtenerSucursalCiudad(requireContext())
                    )
                )
            }

            val activaId = SessionManager.obtenerSucursalId(requireContext())

            sucursalesFinales.forEach { sucursal ->
                container.addView(crearTarjetaSucursal(sucursal, sucursal.id == activaId))
            }
        }
    }

    private fun crearTarjetaSucursal(sucursal: SucursalDto, esActiva: Boolean): View {
        val context = requireContext()

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
            strokeWidth = dpToPx(if (esActiva) 2 else 1)
            strokeColor = ContextCompat.getColor(
                context,
                if (esActiva) R.color.apex_orange else R.color.apex_stroke_light
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                SessionManager.guardarSucursal(
                    context = context,
                    sucursalId = sucursal.id,
                    nombre = sucursal.nombre,
                    codigo = sucursal.codigo,
                    ciudad = sucursal.ciudad
                )
                CartManager.vaciarCarrito(context)
                parentFragmentManager.setFragmentResult(
                    RESULT_KEY_SUCURSAL_ACTUALIZADA,
                    Bundle().apply { putInt("sucursal_id", sucursal.id) }
                )
                Toast.makeText(
                    context,
                    "Sucursal activa: ${sucursal.nombre ?: SessionManager.DEFAULT_SUCURSAL_NOMBRE}",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val radioOuter = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(context, R.color.white))
                setStroke(
                    dpToPx(if (esActiva) 2 else 1),
                    ContextCompat.getColor(
                        context,
                        if (esActiva) R.color.apex_orange else R.color.apex_stroke_light
                    )
                )
            }
            if (esActiva) {
                val dot = View(context).apply {
                    layoutParams = FrameLayout.LayoutParams(dpToPx(12), dpToPx(12), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(context, R.color.apex_orange))
                    }
                }
                addView(dot)
            }
        }
        row.addView(radioOuter)

        val infoCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
                marginEnd = dpToPx(8)
            }
        }

        val tvNombre = TextView(context).apply {
            text = sucursal.nombre?.takeIf { it.isNotBlank() } ?: "Sucursal #${sucursal.id}"
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        infoCol.addView(tvNombre)

        val detalle = listOfNotNull(
            sucursal.codigo?.takeIf { it.isNotBlank() },
            sucursal.ciudad?.takeIf { it.isNotBlank() },
            sucursal.direccion?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        if (detalle.isNotBlank()) {
            val tvDetalle = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(2)
                }
                text = detalle
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            }
            infoCol.addView(tvDetalle)
        }
        row.addView(infoCol)

        if (esActiva) {
            val badge = TextView(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(8).toFloat()
                    setColor(ContextCompat.getColor(context, R.color.apex_orange_translucent))
                }
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                text = "Activa"
                setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
            }
            row.addView(badge)
        }

        card.addView(row)
        return card
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val TAG = "SucursalSelectorBottomSheet"
        const val RESULT_KEY_SUCURSAL_ACTUALIZADA = "resultado_sucursal_actualizada"
    }
}

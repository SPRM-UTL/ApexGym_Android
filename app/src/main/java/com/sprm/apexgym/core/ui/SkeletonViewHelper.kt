package com.sprm.apexgym.core.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.sprm.apexgym.R

/**
 * Genera vistas esqueleto animadas ligeras para carga inicial de listas y grids
 * sin librerías externas. Cancela su animador al desprenderse de la ventana.
 */
object SkeletonViewHelper {

    private const val TAG_SKELETON = "apex_skeleton_view"

    fun mostrarSkeletonProductosPopular(container: LinearLayout?) {
        mostrarGridDosColumnas(container, filas = 2, altoTarjetaDp = 172)
    }

    fun mostrarSkeletonProductosGrid(container: LinearLayout?) {
        mostrarGridDosColumnas(container, filas = 2, altoTarjetaDp = 196)
    }

    fun mostrarSkeletonServiciosLista(container: LinearLayout?) {
        if (container == null) return
        val context = container.context
        limpiarSkeletons(container)
        repeat(3) { index ->
            val tarjeta = crearBloquePulsante(context).apply {
                tag = TAG_SKELETON
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(context, 92)
                ).apply {
                    if (index > 0) topMargin = dp(context, 12)
                }
            }
            container.addView(tarjeta)
        }
    }

    fun mostrarSkeletonMembresiaPlanes(container: LinearLayout?) {
        if (container == null) return
        val context = container.context
        limpiarSkeletons(container)
        repeat(2) { index ->
            val tarjeta = crearBloquePulsante(context).apply {
                tag = TAG_SKELETON
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(context, 126)
                ).apply {
                    if (index > 0) topMargin = dp(context, 12)
                }
            }
            container.addView(tarjeta)
        }
    }

    fun limpiarSkeletons(container: LinearLayout?) {
        if (container == null) return
        for (i in container.childCount - 1 downTo 0) {
            if (container.getChildAt(i).tag == TAG_SKELETON) {
                container.removeViewAt(i)
            }
        }
    }

    private fun mostrarGridDosColumnas(
        container: LinearLayout?,
        filas: Int,
        altoTarjetaDp: Int
    ) {
        if (container == null) return
        val context = container.context
        limpiarSkeletons(container)
        repeat(filas) { filaIndex ->
            val row = LinearLayout(context).apply {
                tag = TAG_SKELETON
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (filaIndex > 0) topMargin = dp(context, 10)
                }
            }
            repeat(2) { colIndex ->
                val card = crearBloquePulsante(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        dp(context, altoTarjetaDp),
                        1f
                    ).apply {
                        if (colIndex == 0) marginEnd = dp(context, 5)
                        else marginStart = dp(context, 5)
                    }
                }
                row.addView(card)
            }
            container.addView(row)
        }
    }

    private fun crearBloquePulsante(context: Context): View {
        val vista = View(context).apply {
            setBackgroundResource(R.drawable.bg_skeleton_block)
            alpha = 0.55f
        }
        val animador = ObjectAnimator.ofFloat(vista, View.ALPHA, 0.45f, 0.95f).apply {
            duration = 750L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        vista.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                if (!animador.isRunning) animador.start()
            }

            override fun onViewDetachedFromWindow(v: View) {
                animador.cancel()
            }
        })
        animador.start()
        return vista
    }

    private fun dp(context: Context, valor: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        valor.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}

package com.sprm.apexgym.core.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object TicketImageSharer {

    private const val DIR_SHARED_TICKETS = "shared_tickets"
    private const val TARGET_CARD_WIDTH_PX = 960f

    fun compartirComprobanteComoImagen(
        context: Context,
        cardView: View?,
        nombreArchivoBase: String,
        tituloChooser: String,
        asunto: String,
        textoRespaldo: String
    ) {
        val archivoImagen = try {
            cardView?.let { vista ->
                val bitmap = renderizarComprobante(vista)
                if (bitmap != null) {
                    guardarBitmapTemporal(context, bitmap, nombreArchivoBase).also {
                        bitmap.recycle()
                    }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }

        if (archivoImagen == null) {
            compartirTexto(context, tituloChooser, asunto, textoRespaldo)
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, archivoImagen)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, asunto)
                clipData = ClipData.newUri(context.contentResolver, asunto, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, tituloChooser).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            compartirTexto(context, tituloChooser, asunto, textoRespaldo)
        }
    }

    private fun renderizarComprobante(cardView: View): Bitmap? {
        if (cardView.width <= 0 || cardView.height <= 0) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            cardView.measure(widthSpec, heightSpec)
            if (cardView.measuredWidth <= 0 || cardView.measuredHeight <= 0) return null
            cardView.layout(0, 0, cardView.measuredWidth, cardView.measuredHeight)
        }

        val viewWidth = cardView.width.takeIf { it > 0 } ?: cardView.measuredWidth
        val viewHeight = cardView.height.takeIf { it > 0 } ?: cardView.measuredHeight
        if (viewWidth <= 0 || viewHeight <= 0) return null

        val scale = (TARGET_CARD_WIDTH_PX / viewWidth.toFloat()).coerceIn(1.25f, 2.5f)
        val scaledCardWidth = (viewWidth * scale).roundToInt()
        val scaledCardHeight = (viewHeight * scale).roundToInt()

        val padHorizontal = 56
        val accentHeight = 14
        val padTop = accentHeight + 52
        val padBottom = 92

        val totalWidth = scaledCardWidth + (padHorizontal * 2)
        val totalHeight = padTop + scaledCardHeight + padBottom

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.parseColor("#F3F4F6"))

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6B00")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, totalWidth.toFloat(), accentHeight.toFloat(), accentPaint)

        val cardRect = RectF(
            padHorizontal.toFloat(),
            padTop.toFloat(),
            (padHorizontal + scaledCardWidth).toFloat(),
            (padTop + scaledCardHeight).toFloat()
        )
        val cornerRadius = 36f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(26f, 0f, 10f, Color.argb(38, 15, 23, 42))
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, shadowPaint)

        val clipPath = Path().apply {
            addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.translate(cardRect.left, cardRect.top)
        canvas.scale(scale, scale)
        cardView.draw(canvas)
        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 25f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val baselineY = cardRect.bottom + 56f
        canvas.drawText(
            "Comprobante digital · ApexGym",
            totalWidth / 2f,
            baselineY,
            watermarkPaint
        )

        return bitmap
    }

    private fun guardarBitmapTemporal(
        context: Context,
        bitmap: Bitmap,
        nombreArchivoBase: String
    ): File {
        val dir = File(context.cacheDir, DIR_SHARED_TICKETS).apply {
            if (!exists()) mkdirs()
        }
        dir.listFiles()?.forEach { archivoAnterior ->
            runCatching { archivoAnterior.delete() }
        }
        val nombreLimpio = nombreArchivoBase
            .lowercase()
            .replace(Regex("[^a-z0-9_-]"), "_")
            .trim('_')
            .ifBlank { "comprobante_apexgym" }
        val archivo = File(dir, "${nombreLimpio}_${System.currentTimeMillis()}.png")
        FileOutputStream(archivo).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return archivo
    }

    private fun compartirTexto(
        context: Context,
        tituloChooser: String,
        asunto: String,
        texto: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        context.startActivity(Intent.createChooser(intent, tituloChooser))
    }
}

package com.sprm.apexgym.core.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrGenerator {

    /**
     * Genera un Bitmap que representa un código QR con el contenido dado.
     */
    fun generarQr(contenido: String, ancho: Int = 512, alto: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(contenido, BarcodeFormat.QR_CODE, ancho, alto)
            val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.RGB_565)

            for (x in 0 until ancho) {
                for (y in 0 until alto) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

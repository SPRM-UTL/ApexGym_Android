package com.sprm.apexgym.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/** Canal único de notificaciones push de ApexGym. */
object NotificacionesCanal {

    const val ID = "apexgym_notificaciones"
    private const val NOMBRE = "Notificaciones"
    private const val DESCRIPCION = "Avisos y recordatorios de ApexGym"

    fun crear(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        val canal = NotificationChannel(
            ID,
            NOMBRE,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = DESCRIPCION
        }
        manager.createNotificationChannel(canal)
    }
}

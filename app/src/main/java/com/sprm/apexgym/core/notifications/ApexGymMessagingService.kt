package com.sprm.apexgym.core.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sprm.apexgym.R
import com.sprm.apexgym.feature.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Recibe los push de FCM: construye la notificación local y mantiene el token
 * del dispositivo sincronizado con el backend.
 */
class ApexGymMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.guardarToken(this, token)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            FcmTokenManager.registrarToken(this@ApexGymMessagingService, token)
        }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        super.onMessageReceived(mensaje)

        val titulo = mensaje.notification?.title
            ?: mensaje.data[CLAVE_TITULO]
            ?: getString(R.string.app_name)
        val cuerpo = mensaje.notification?.body
            ?: mensaje.data[CLAVE_MENSAJE]
            ?: return

        mostrarNotificacion(
            titulo = titulo,
            cuerpo = cuerpo,
            notificacionId = mensaje.data[CLAVE_NOTIFICACION_ID]?.toIntOrNull()
        )
    }

    @SuppressLint("MissingPermission")
    private fun mostrarNotificacion(
        titulo: String,
        cuerpo: String,
        notificacionId: Int?
    ) {
        NotificacionesCanal.crear(this)
        if (!NotificacionPermiso.estaConcedido(this)) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ABRIR_NOTIFICACIONES, true)
            notificacionId?.let { putExtra(MainActivity.EXTRA_NOTIFICACION_ID, it) }
        }

        val idNotificacion = notificacionId ?: ID_NOTIFICACION_GENERAL
        val pendingIntent = PendingIntent.getActivity(
            this,
            idNotificacion,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(this, NotificacionesCanal.ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setColor(ContextCompat.getColor(this, R.color.apex_orange))
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(idNotificacion, notificacion)
    }

    companion object {
        private const val ID_NOTIFICACION_GENERAL = 7001
        private const val CLAVE_TITULO = "titulo"
        private const val CLAVE_MENSAJE = "mensaje"
        private const val CLAVE_NOTIFICACION_ID = "notificacionId"
    }
}

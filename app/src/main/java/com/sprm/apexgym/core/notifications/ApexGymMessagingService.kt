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
import com.sprm.apexgym.core.network.api.NotificacionDto
import com.sprm.apexgym.feature.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Recibe los push de FCM: construye la notificación local, emite broadcast local
 * para actualizar pantallas activas y mantiene el token del dispositivo sincronizado.
 */
class ApexGymMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.guardarToken(this, token)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            FcmTokenManager.registrarToken(this@ApexGymMessagingService, token, forzar = true)
        }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        super.onMessageReceived(mensaje)

        val tipo = mensaje.data[CLAVE_TIPO]?.trim()?.takeIf { it.isNotEmpty() } ?: TIPO_GENERAL
        val notificacionId = mensaje.data[CLAVE_NOTIFICACION_ID]?.toIntOrNull()?.takeIf { it > 0 }
        val titulo = mensaje.notification?.title
            ?: mensaje.data[CLAVE_TITULO]
            ?: getString(R.string.app_name)
        val cuerpo = mensaje.notification?.body
            ?: mensaje.data[CLAVE_MENSAJE]
            ?: mensaje.data[CLAVE_BODY]

        emitirBroadcastNotificacion(
            notificacionId = notificacionId,
            tipo = tipo,
            titulo = titulo,
            mensaje = cuerpo
        )

        if (cuerpo.isNullOrBlank()) return

        mostrarNotificacion(
            titulo = titulo,
            cuerpo = cuerpo,
            notificacionId = notificacionId,
            tipo = tipo
        )
    }

    private fun emitirBroadcastNotificacion(
        notificacionId: Int?,
        tipo: String,
        titulo: String,
        mensaje: String?
    ) {
        val intent = Intent(ACTION_NOTIFICACION_RECIBIDA).apply {
            setPackage(packageName)
            notificacionId?.let { putExtra(EXTRA_NOTIFICACION_ID, it) }
            putExtra(EXTRA_TIPO, tipo)
            putExtra(EXTRA_TITULO, titulo)
            mensaje?.let { putExtra(EXTRA_MENSAJE, it) }
        }
        sendBroadcast(intent)
    }

    @SuppressLint("MissingPermission")
    private fun mostrarNotificacion(
        titulo: String,
        cuerpo: String,
        notificacionId: Int?,
        tipo: String
    ) {
        NotificacionesCanal.crear(this)
        if (!NotificacionPermiso.estaConcedido(this)) return

        val esActividadEmpleado = tipo.equals(TIPO_ACTIVIDAD_EMPLEADO, ignoreCase = true)
        val idNotificacion = generarIdUnico(notificacionId)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ABRIR_NOTIFICACIONES, true)
            putExtra(EXTRA_TIPO, tipo)
            notificacionId?.let { putExtra(MainActivity.EXTRA_NOTIFICACION_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            idNotificacion,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificacionesCanal.ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setColor(ContextCompat.getColor(this, R.color.apex_orange))
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (esActividadEmpleado) {
            builder
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSubText("Actividad de empleado")
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        NotificationManagerCompat.from(this).notify(idNotificacion, builder.build())
    }

    companion object {
        const val ACTION_NOTIFICACION_RECIBIDA = "com.sprm.apexgym.ACTION_NOTIFICACION_RECIBIDA"
        const val EXTRA_NOTIFICACION_ID = "notificacionId"
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_MENSAJE = "mensaje"
        const val TIPO_ACTIVIDAD_EMPLEADO = NotificacionDto.TIPO_ACTIVIDAD_EMPLEADO

        private const val TIPO_GENERAL = "GENERAL"
        private const val CLAVE_TITULO = "titulo"
        private const val CLAVE_MENSAJE = "mensaje"
        private const val CLAVE_BODY = "body"
        private const val CLAVE_TIPO = "tipo"
        private const val CLAVE_NOTIFICACION_ID = "notificacionId"

        private val secuenciaUnica = AtomicInteger(7001)

        internal fun generarIdUnico(notificacionId: Int?): Int {
            if (notificacionId != null && notificacionId > 0) {
                return notificacionId
            }
            val seq = secuenciaUnica.getAndIncrement() and 0x0FFF
            val tiempo = ((System.currentTimeMillis() / 10L) and 0x0FFFF000L).toInt()
            val combinado = tiempo or seq
            return if (combinado > 0) combinado else 7001 + seq
        }
    }
}

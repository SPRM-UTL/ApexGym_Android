package com.sprm.apexgym.core.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.sprm.apexgym.core.network.api.RegistrarTokenDispositivoRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Obtiene el token FCM del dispositivo y lo registra en el backend
 * (POST /api/dispositivos/token). El backend asocia el token con la cuenta
 * autenticada mediante la cookie de sesión.
 */
object FcmTokenManager {

    private const val PREFS = "fcm_apexgym"
    private const val KEY_TOKEN = "token_actual"
    private const val KEY_ULTIMO_REGISTRO = "ultimo_registro"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Guarda el token recibido en `onNewToken` para reenviarlo tras el login. */
    fun guardarToken(context: Context, token: String) {
        if (token.isBlank()) return
        prefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Registra el token en segundo plano. Pensado para llamarse después del
     * login sin bloquear la navegación ni depender del ciclo de vida de la vista.
     */
    fun registrarAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch { registrar(appContext) }
    }

    /** Obtiene el token actual de Firebase y lo registra si hay sesión. */
    suspend fun registrar(context: Context): Boolean {
        val token = obtenerTokenActual()
            ?: prefs(context).getString(KEY_TOKEN, null)
            ?: return false
        return registrarToken(context, token)
    }

    /**
     * Envía el token al backend. No falla si no hay sesión activa o si el
     * backend rechaza la cuenta; se reintenta en el próximo login o refresh.
     */
    suspend fun registrarToken(context: Context, token: String): Boolean {
        if (token.isBlank()) return false
        val appContext = context.applicationContext
        RetrofitClient.initialize(appContext)

        val usuarioId = SessionManager.obtenerUsuarioId(appContext)
        if (usuarioId <= 0 || !SessionManager.haySesion(appContext)) return false
        if (!RetrofitClient.cookieJar.tieneToken()) return false

        val marca = "$usuarioId:$token"
        if (prefs(appContext).getString(KEY_ULTIMO_REGISTRO, null) == marca) return true

        return try {
            val respuesta = RetrofitClient.dispositivoService.registrarToken(
                RegistrarTokenDispositivoRequest(token = token)
            )
            val registrado = respuesta.isSuccessful && respuesta.body()?.responseFlag == 0
            if (registrado) {
                prefs(appContext).edit().putString(KEY_ULTIMO_REGISTRO, marca).apply()
            }
            registrado
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun obtenerTokenActual(): String? = try {
        suspendCancellableCoroutine { continuacion ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    val token = tarea.result
                    if (!token.isNullOrBlank()) {
                        continuacion.resume(token)
                    } else {
                        continuacion.resumeWithException(
                            IllegalStateException("Firebase no devolvió un token FCM")
                        )
                    }
                } else {
                    continuacion.resumeWithException(
                        tarea.exception ?: IllegalStateException("No se pudo obtener el token FCM")
                    )
                }
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

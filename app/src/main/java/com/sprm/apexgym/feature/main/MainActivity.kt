package com.sprm.apexgym.feature.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.notifications.FcmTokenManager
import com.sprm.apexgym.core.notifications.NotificacionPermiso
import com.sprm.apexgym.core.notifications.NotificacionesCanal
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.feature.auth.LoginFragment
import com.sprm.apexgym.feature.notificaciones.NotificacionesListFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializar cliente HTTP con contexto de aplicación
        RetrofitClient.initialize(this)
        NotificacionesCanal.crear(this)

        if (savedInstanceState == null) {
            val sesionValida = SessionManager.haySesion(this) &&
                RetrofitClient.cookieJar.tieneToken()
            if (!sesionValida) {
                SessionManager.limpiar(this)
            }
            val destino = when {
                !sesionValida -> LoginFragment()
                SessionManager.esEmpleado(this) -> EmpleadoDashboardFragment()
                else -> HomeFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, destino)
                .commit()
        }

        if (SessionManager.haySesion(this)) {
            NotificacionPermiso.solicitarSiHaceFalta(this)
            FcmTokenManager.registrarAsync(this)
        }
        procesarIntentoNotificaciones(intent)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        procesarIntentoNotificaciones(intent)
    }

    /**
     * Abre Notificaciones cuando la app llega desde un push: por el intent
     * propio del servicio o por los datos que FCM agrega al tocar la
     * notificación mostrada en segundo plano.
     */
    private fun procesarIntentoNotificaciones(intent: Intent?) {
        if (intent == null) return
        val vieneDePush = intent.getBooleanExtra(EXTRA_ABRIR_NOTIFICACIONES, false) ||
            intent.hasExtra(EXTRA_NOTIFICACION_ID)
        if (!vieneDePush) return

        intent.removeExtra(EXTRA_ABRIR_NOTIFICACIONES)
        intent.removeExtra(EXTRA_NOTIFICACION_ID)

        if (!SessionManager.haySesion(this) || !RetrofitClient.cookieJar.tieneToken()) return
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) is NotificacionesListFragment) {
            return
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, NotificacionesListFragment())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        const val EXTRA_ABRIR_NOTIFICACIONES = "com.sprm.apexgym.ABRIR_NOTIFICACIONES"
        const val EXTRA_NOTIFICACION_ID = "notificacionId"
    }
}

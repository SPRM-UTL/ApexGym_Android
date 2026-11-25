package com.sprm.apexgym.feature.main

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.hypot
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.notifications.FcmTokenManager
import com.sprm.apexgym.core.notifications.NotificacionPermiso
import com.sprm.apexgym.core.notifications.NotificacionesCanal
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.feature.auth.LoginFragment
import com.sprm.apexgym.feature.notificaciones.NotificacionesListFragment

class MainActivity : AppCompatActivity() {

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var esArrastre = false

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
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.rawX
                touchDownY = ev.rawY
                esArrastre = false
            }
            MotionEvent.ACTION_MOVE -> {
                val slop = ViewConfiguration.get(this).scaledTouchSlop
                if (hypot((ev.rawX - touchDownX).toDouble(), (ev.rawY - touchDownY).toDouble()) > slop) {
                    esArrastre = true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!esArrastre) {
                    val focused = currentFocus
                    if (focused is EditText) {
                        val rx = ev.rawX.toInt()
                        val ry = ev.rawY.toInt()
                        val root = window.decorView
                        if (!tocoCampoDeTexto(root, rx, ry)) {
                            focused.clearFocus()
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.hideSoftInputFromWindow(focused.windowToken, 0)
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun tocoCampoDeTexto(view: View, rawX: Int, rawY: Int): Boolean {
        if (!view.isShown) return false
        if (view is EditText || view is TextInputLayout) {
            val rect = Rect()
            view.getGlobalVisibleRect(rect)
            if (rect.contains(rawX, rawY)) return true
        }
        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                if (tocoCampoDeTexto(view.getChildAt(i), rawX, rawY)) return true
            }
        }
        return false
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

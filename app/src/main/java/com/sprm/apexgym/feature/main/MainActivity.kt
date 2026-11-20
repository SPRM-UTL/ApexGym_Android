package com.sprm.apexgym.feature.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.auth.LoginFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializar cliente HTTP con contexto de aplicación
        RetrofitClient.initialize(this)

        if (savedInstanceState == null) {
            val sesionValida = com.sprm.apexgym.core.session.SessionManager.haySesion(this) &&
                RetrofitClient.cookieJar.tieneToken()
            if (!sesionValida) {
                com.sprm.apexgym.core.session.SessionManager.limpiar(this)
            }
            val destino = when {
                !sesionValida -> LoginFragment()
                com.sprm.apexgym.core.session.SessionManager.esEmpleado(this) -> EmpleadoDashboardFragment()
                else -> HomeFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, destino)
                .commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
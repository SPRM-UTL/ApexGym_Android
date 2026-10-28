package com.sprm.apexgym.feature.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.sprm.apexgym.R

/**
 * Pantalla de Inicio de Sesión (Mockup UI/UX Material Design 3).
 * Navegación de prototipo:
 * - "Iniciar sesión" -> HomeFragment (Pantalla de Inicio)
 * - "Registrarme"    -> RegisterFragment (Pantalla de Registro)
 */
class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnIniciarSesion = view.findViewById<MaterialButton>(R.id.btnIniciarSesion)
        val tvRegistrarse = view.findViewById<View>(R.id.tvRegistrarse)
        val llIrARegistro = view.findViewById<View>(R.id.llIrARegistro)

        // Ir a la pantalla principal (Inicio)
        btnIniciarSesion.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, HomeFragment())
                .addToBackStack("login_to_home")
                .commit()
        }

        // Ir a la pantalla de registro
        val irARegistroListener = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack("login_to_register")
                .commit()
        }

        tvRegistrarse.setOnClickListener(irARegistroListener)
        llIrARegistro.setOnClickListener(irARegistroListener)
    }
}

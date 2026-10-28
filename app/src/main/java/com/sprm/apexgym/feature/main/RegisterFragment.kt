package com.sprm.apexgym.feature.main

import android.app.DatePickerDialog
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.sprm.apexgym.R
import java.util.Calendar
import java.util.Locale

/**
 * Pantalla de Registro de Socio (Mockup UI/UX Material Design 3).
 * Organizada en 3 pasos progresivos (Progressive Disclosure) alineados a POST /api/clientes/registrar:
 * - Paso 1: Cuenta (fotoUrl, usuario, email, contrasenia)
 * - Paso 2: Datos personales (nombre, apellidoPaterno, apellidoMaterno, fechaNacimiento, genero)
 * - Paso 3: Contacto y emergencia (telefono, direccion, contactoEmergenciaNombre, contactoEmergenciaTelefono)
 */
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var currentStep = 1
    private var selectedGender = "Masculino"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollRegister = view.findViewById<NestedScrollView>(R.id.scrollRegister)
        val llRegisterRoot = view.findViewById<ViewGroup>(R.id.llRegisterRoot)

        val btnVolverLogin = view.findViewById<View>(R.id.btnVolverLogin)
        val tvBackLabel = view.findViewById<TextView>(R.id.tvBackLabel)
        val tvTopIrALogin = view.findViewById<View>(R.id.tvTopIrALogin)
        val tvIrALogin = view.findViewById<View>(R.id.tvIrALogin)
        val llIrALogin = view.findViewById<View>(R.id.llIrALogin)
        val btnCrearCuenta = view.findViewById<MaterialButton>(R.id.btnCrearCuenta)

        val viewStep1 = view.findViewById<View>(R.id.viewStep1)
        val viewStep2 = view.findViewById<View>(R.id.viewStep2)
        val viewStep3 = view.findViewById<View>(R.id.viewStep3)

        val tvStepOverline = view.findViewById<TextView>(R.id.tvStepOverline)
        val tvStepTitle = view.findViewById<TextView>(R.id.tvStepTitle)
        val tvStepSubtitle = view.findViewById<TextView>(R.id.tvStepSubtitle)

        val layoutStep1 = view.findViewById<View>(R.id.layoutStep1)
        val layoutStep2 = view.findViewById<View>(R.id.layoutStep2)
        val layoutStep3 = view.findViewById<View>(R.id.layoutStep3)

        // Selector de Fecha de Nacimiento (Paso 2)
        val etFechaNacimiento = view.findViewById<TextInputEditText>(R.id.etFechaNacimiento)
        etFechaNacimiento.setOnClickListener {
            mostrarSelectorFecha(etFechaNacimiento)
        }

        // Selector de Género por Chips (Paso 2)
        val chipMasculino = view.findViewById<TextView>(R.id.chipGeneroMasculino)
        val chipFemenino = view.findViewById<TextView>(R.id.chipGeneroFemenino)
        val chipOtro = view.findViewById<TextView>(R.id.chipGeneroOtro)
        val genderChips = listOf(chipMasculino, chipFemenino, chipOtro)

        genderChips.forEach { chip ->
            chip.setOnClickListener {
                selectedGender = chip.text.toString()
                actualizarChipsGenero(genderChips, chip)
            }
        }

        fun actualizarVistaPaso(step: Int) {
            currentStep = step

            val transition = AutoTransition().apply {
                duration = 260L
                interpolator = FastOutSlowInInterpolator()
            }
            TransitionManager.beginDelayedTransition(llRegisterRoot, transition)

            // Indicadores de barra de progreso
            viewStep1.setBackgroundResource(R.drawable.bg_step_active)
            viewStep2.setBackgroundResource(
                if (step >= 2) R.drawable.bg_step_active else R.drawable.bg_step_inactive
            )
            viewStep3.setBackgroundResource(
                if (step >= 3) R.drawable.bg_step_active else R.drawable.bg_step_inactive
            )

            // Visibilidad de bloques por paso
            layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
            layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
            layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE

            // Textos contextuales según el paso
            when (step) {
                1 -> {
                    tvBackLabel.setText(R.string.register_back_to_login)
                    tvStepOverline.setText(R.string.register_step1_overline)
                    tvStepTitle.setText(R.string.register_step1_title)
                    tvStepSubtitle.setText(R.string.register_step1_subtitle)
                    btnCrearCuenta.setText(R.string.register_btn_next)
                }
                2 -> {
                    tvBackLabel.setText(R.string.register_prev_step)
                    tvStepOverline.setText(R.string.register_step2_overline)
                    tvStepTitle.setText(R.string.register_step2_title)
                    tvStepSubtitle.setText(R.string.register_step2_subtitle)
                    btnCrearCuenta.setText(R.string.register_btn_next)
                }
                3 -> {
                    tvBackLabel.setText(R.string.register_prev_step)
                    tvStepOverline.setText(R.string.register_step3_overline)
                    tvStepTitle.setText(R.string.register_step3_title)
                    tvStepSubtitle.setText(R.string.register_step3_subtitle)
                    btnCrearCuenta.setText(R.string.register_btn_submit)
                }
            }

            scrollRegister.smoothScrollTo(0, 0)
        }

        val irALoginDirecto = View.OnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.fragment_container, LoginFragment())
                    .commit()
            }
        }

        // Botón superior izquierdo: retrocede un paso o vuelve al Login si está en el Paso 1
        btnVolverLogin.setOnClickListener {
            if (currentStep > 1) {
                actualizarVistaPaso(currentStep - 1)
            } else {
                irALoginDirecto.onClick(it)
            }
        }

        // Botón principal: avanza al siguiente paso o finaliza enviando al Login
        btnCrearCuenta.setOnClickListener {
            if (currentStep < 3) {
                actualizarVistaPaso(currentStep + 1)
            } else {
                irALoginDirecto.onClick(it)
            }
        }

        // Enlaces directos "Ingresar" (superior e inferior) siempre envían a Login
        tvTopIrALogin.setOnClickListener(irALoginDirecto)
        tvIrALogin.setOnClickListener(irALoginDirecto)
        llIrALogin.setOnClickListener(irALoginDirecto)
    }

    private fun mostrarSelectorFecha(targetInput: TextInputEditText) {
        val calendar = Calendar.getInstance()
        calendar.set(2000, Calendar.JANUARY, 1)

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val fechaFormateada = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    dayOfMonth
                )
                targetInput.setText(fechaFormateada)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    private fun actualizarChipsGenero(chips: List<TextView>, seleccionado: TextView) {
        val context = requireContext()
        val colorWhite = ContextCompat.getColor(context, R.color.white)
        val colorPrimaryText = ContextCompat.getColor(context, R.color.apex_text_primary)

        chips.forEach { chip ->
            if (chip == seleccionado) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(colorWhite)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(colorPrimaryText)
            }
        }
    }
}

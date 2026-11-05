package com.sprm.apexgym.feature.main

import android.app.DatePickerDialog
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.CorreoRequest
import com.sprm.apexgym.core.network.client.RegistroRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.auth.LoginFragment
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var currentStep = 1
    private var selectedGender = "Masculino"
    private var procesandoPeticion = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollRegister = view.findViewById<NestedScrollView>(R.id.scrollRegister)
        val llRegisterRoot = view.findViewById<ViewGroup>(R.id.llRegisterRoot)

        val btnVolverLogin = view.findViewById<View>(R.id.btnVolverLogin)
        val tvBackLabel = view.findViewById<TextView>(R.id.tvBackLabel)
        val tvIrALogin = view.findViewById<View>(R.id.tvIrALogin)
        val llIrALogin = view.findViewById<View>(R.id.llIrALogin)
        val btnCrearCuenta = view.findViewById<MaterialButton>(R.id.btnCrearCuenta)

        val viewStep1 = view.findViewById<View>(R.id.viewStep1)
        val viewStep2 = view.findViewById<View>(R.id.viewStep2)
        val viewStep3 = view.findViewById<View>(R.id.viewStep3)

        val tvStepTitle = view.findViewById<TextView>(R.id.tvStepTitle)
        val tvStepSubtitle = view.findViewById<TextView>(R.id.tvStepSubtitle)

        val layoutStep1 = view.findViewById<View>(R.id.layoutStep1)
        val layoutStep2 = view.findViewById<View>(R.id.layoutStep2)
        val layoutStep3 = view.findViewById<View>(R.id.layoutStep3)

        val tilUsuario = view.findViewById<TextInputLayout>(R.id.tilUsuarioRegistro)
        val etUsuario = view.findViewById<TextInputEditText>(R.id.etUsuarioRegistro)
        val tilCorreo = view.findViewById<TextInputLayout>(R.id.tilCorreoRegistro)
        val etCorreo = view.findViewById<TextInputEditText>(R.id.etCorreoRegistro)
        val tilContrasena = view.findViewById<TextInputLayout>(R.id.tilContrasenaRegistro)
        val etContrasena = view.findViewById<TextInputEditText>(R.id.etContrasenaRegistro)
        val tilConfirmarContrasena = view.findViewById<TextInputLayout>(R.id.tilConfirmarContrasenaRegistro)
        val etConfirmarContrasena = view.findViewById<TextInputEditText>(R.id.etConfirmarContrasenaRegistro)

        val tilNombre = view.findViewById<TextInputLayout>(R.id.tilNombreRegistro)
        val etNombre = view.findViewById<TextInputEditText>(R.id.etNombreRegistro)
        val tilApellidoPaterno = view.findViewById<TextInputLayout>(R.id.tilApellidoPaterno)
        val etApellidoPaterno = view.findViewById<TextInputEditText>(R.id.etApellidoPaterno)
        val tilApellidoMaterno = view.findViewById<TextInputLayout>(R.id.tilApellidoMaterno)
        val etApellidoMaterno = view.findViewById<TextInputEditText>(R.id.etApellidoMaterno)
        val tilFechaNacimiento = view.findViewById<TextInputLayout>(R.id.tilFechaNacimiento)
        val etFechaNacimiento = view.findViewById<TextInputEditText>(R.id.etFechaNacimiento)

        val tilTelefono = view.findViewById<TextInputLayout>(R.id.tilTelefonoRegistro)
        val etTelefono = view.findViewById<TextInputEditText>(R.id.etTelefonoRegistro)
        val tilDireccion = view.findViewById<TextInputLayout>(R.id.tilDireccionRegistro)
        val etDireccion = view.findViewById<TextInputEditText>(R.id.etDireccionRegistro)
        val tilEmergenciaNombre = view.findViewById<TextInputLayout>(R.id.tilEmergenciaNombre)
        val etEmergenciaNombre = view.findViewById<TextInputEditText>(R.id.etEmergenciaNombre)
        val tilEmergenciaTelefono = view.findViewById<TextInputLayout>(R.id.tilEmergenciaTelefono)
        val etEmergenciaTelefono = view.findViewById<TextInputEditText>(R.id.etEmergenciaTelefono)

        etUsuario.doAfterTextChanged { tilUsuario.error = null }
        etCorreo.doAfterTextChanged { tilCorreo.error = null }
        etContrasena.doAfterTextChanged { tilContrasena.error = null }
        etConfirmarContrasena.doAfterTextChanged { tilConfirmarContrasena.error = null }
        etNombre.doAfterTextChanged { tilNombre.error = null }
        etApellidoPaterno.doAfterTextChanged { tilApellidoPaterno.error = null }
        etApellidoMaterno.doAfterTextChanged { tilApellidoMaterno.error = null }
        etFechaNacimiento.doAfterTextChanged { tilFechaNacimiento.error = null }
        etTelefono.doAfterTextChanged { tilTelefono.error = null }
        etDireccion.doAfterTextChanged { tilDireccion.error = null }
        etEmergenciaNombre.doAfterTextChanged { tilEmergenciaNombre.error = null }
        etEmergenciaTelefono.doAfterTextChanged { tilEmergenciaTelefono.error = null }

        etFechaNacimiento.setOnClickListener {
            mostrarSelectorFecha(etFechaNacimiento)
        }

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

            viewStep1.setBackgroundResource(R.drawable.bg_step_active)
            viewStep2.setBackgroundResource(
                if (step >= 2) R.drawable.bg_step_active else R.drawable.bg_step_inactive
            )
            viewStep3.setBackgroundResource(
                if (step >= 3) R.drawable.bg_step_active else R.drawable.bg_step_inactive
            )

            layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
            layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
            layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE

            when (step) {
                1 -> {
                    tvBackLabel.setText(R.string.register_back_to_login)
                    tvStepTitle.setText(R.string.register_step1_title)
                    tvStepSubtitle.setText(R.string.register_step1_subtitle)
                    btnCrearCuenta.setText(R.string.register_btn_next)
                }
                2 -> {
                    tvBackLabel.setText(R.string.register_prev_step)
                    tvStepTitle.setText(R.string.register_step2_title)
                    tvStepSubtitle.setText(R.string.register_step2_subtitle)
                    btnCrearCuenta.setText(R.string.register_btn_next)
                }
                3 -> {
                    tvBackLabel.setText(R.string.register_prev_step)
                    tvStepTitle.setText(R.string.register_step3_title)
                    tvStepSubtitle.setText(R.string.register_step3_subtitle)
                    btnCrearCuenta.setText(R.string.register_btn_submit)
                }
            }

            scrollRegister.smoothScrollTo(0, 0)
        }

        val irALoginDirecto = View.OnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack(
                    null,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
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

        btnVolverLogin.setOnClickListener {
            if (procesandoPeticion) return@setOnClickListener
            if (currentStep > 1) {
                actualizarVistaPaso(currentStep - 1)
            } else {
                irALoginDirecto.onClick(it)
            }
        }

        btnCrearCuenta.setOnClickListener {
            if (procesandoPeticion) return@setOnClickListener

            when (currentStep) {
                1 -> {
                    val usuario = etUsuario.text?.toString()?.trim().orEmpty()
                    val correo = etCorreo.text?.toString()?.trim().orEmpty()
                    val contrasena = etContrasena.text?.toString().orEmpty()
                    val confirmar = etConfirmarContrasena.text?.toString().orEmpty()

                    var valido = true

                    if (usuario.length < 3) {
                        tilUsuario.error = "El usuario debe tener al menos 3 caracteres"
                        valido = false
                    }

                    if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                        tilCorreo.error = "Ingresa un correo electrónico válido"
                        valido = false
                    }

                    if (contrasena.length < 8) {
                        tilContrasena.error = "La contraseña debe tener al menos 8 caracteres"
                        valido = false
                    }

                    if (confirmar != contrasena) {
                        tilConfirmarContrasena.error = "Las contraseñas no coinciden"
                        valido = false
                    }

                    if (!valido) return@setOnClickListener

                    procesandoPeticion = true
                    btnCrearCuenta.isEnabled = false
                    btnCrearCuenta.text = "Verificando correo..."

                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val response = RetrofitClient.authService.verificarCorreo(
                                CorreoRequest(correo = correo)
                            )

                            if (response.isSuccessful) {
                                val usuarioExistente = response.body()?.data?.usuario
                                if (usuarioExistente != null) {
                                    tilCorreo.error = "Este correo electrónico ya está registrado"
                                } else {
                                    actualizarVistaPaso(2)
                                }
                            } else {
                                val msg = extraerMensajeError(response.errorBody()?.string())
                                    ?: "No se pudo verificar el correo (${response.code()})"
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                            }
                        } catch (_: IOException) {
                            Toast.makeText(
                                requireContext(),
                                "No se pudo conectar con el servidor. Verifica tu conexión.",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                "Error al verificar el correo: ${e.localizedMessage ?: "desconocido"}",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            procesandoPeticion = false
                            if (isAdded) {
                                btnCrearCuenta.isEnabled = true
                                btnCrearCuenta.setText(
                                    if (currentStep < 3) R.string.register_btn_next else R.string.register_btn_submit
                                )
                            }
                        }
                    }
                }

                2 -> {
                    val nombre = etNombre.text?.toString()?.trim().orEmpty()
                    val apellidoPaterno = etApellidoPaterno.text?.toString()?.trim().orEmpty()
                    val fechaNacimiento = etFechaNacimiento.text?.toString()?.trim().orEmpty()

                    var valido = true

                    if (nombre.isEmpty()) {
                        tilNombre.error = "Ingresa tu nombre"
                        valido = false
                    }

                    if (apellidoPaterno.isEmpty()) {
                        tilApellidoPaterno.error = "Ingresa tu apellido paterno"
                        valido = false
                    }

                    if (fechaNacimiento.isEmpty()) {
                        tilFechaNacimiento.error = "Selecciona tu fecha de nacimiento"
                        valido = false
                    }

                    if (!valido) return@setOnClickListener

                    actualizarVistaPaso(3)
                }

                3 -> {
                    val telefono = etTelefono.text?.toString()?.trim().orEmpty()
                    val direccion = etDireccion.text?.toString()?.trim().orEmpty()
                    val emergenciaNombre = etEmergenciaNombre.text?.toString()?.trim().orEmpty()
                    val emergenciaTelefono = etEmergenciaTelefono.text?.toString()?.trim().orEmpty()

                    var valido = true

                    if (!telefono.matches(Regex("^\\d{10}$"))) {
                        tilTelefono.error = "Ingresa un teléfono válido de 10 dígitos"
                        valido = false
                    }

                    if (emergenciaTelefono.isNotEmpty() && !emergenciaTelefono.matches(Regex("^\\d{10}$"))) {
                        tilEmergenciaTelefono.error = "El teléfono de emergencia debe tener 10 dígitos"
                        valido = false
                    }

                    if (!valido) return@setOnClickListener

                    val request = RegistroRequest(
                        usuario = etUsuario.text?.toString()?.trim().orEmpty(),
                        email = etCorreo.text?.toString()?.trim().orEmpty(),
                        contrasenia = etContrasena.text?.toString().orEmpty(),
                        fotoUrl = "",
                        nombre = etNombre.text?.toString()?.trim().orEmpty(),
                        apellidoPaterno = etApellidoPaterno.text?.toString()?.trim().orEmpty(),
                        apellidoMaterno = etApellidoMaterno.text?.toString()?.trim().orEmpty(),
                        fechaNacimiento = etFechaNacimiento.text?.toString()?.trim().orEmpty(),
                        genero = selectedGender,
                        telefono = telefono,
                        direccion = direccion,
                        contactoEmergenciaNombre = emergenciaNombre,
                        contactoEmergenciaTelefono = emergenciaTelefono
                    )

                    procesandoPeticion = true
                    btnCrearCuenta.isEnabled = false
                    btnCrearCuenta.text = "Registrando..."

                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val response = RetrofitClient.authService.registrarCliente(request)

                            if (response.isSuccessful) {
                                RetrofitClient.cookieJar.clear()

                                Toast.makeText(
                                    requireContext(),
                                    "Registro exitoso. Ahora puedes iniciar sesión.",
                                    Toast.LENGTH_LONG
                                ).show()

                                irALoginDirecto.onClick(btnCrearCuenta)
                            } else {
                                val msg = extraerMensajeError(response.errorBody()?.string())
                                    ?: "Error al registrar (${response.code()})"
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                            }
                        } catch (_: IOException) {
                            Toast.makeText(
                                requireContext(),
                                "No se pudo conectar con el servidor. Verifica tu conexión.",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                "Error al registrar: ${e.localizedMessage ?: "desconocido"}",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            procesandoPeticion = false
                            if (isAdded) {
                                btnCrearCuenta.isEnabled = true
                                btnCrearCuenta.setText(R.string.register_btn_submit)
                            }
                        }
                    }
                }
            }
        }

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

    private fun extraerMensajeError(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return try {
            JSONObject(rawJson).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}

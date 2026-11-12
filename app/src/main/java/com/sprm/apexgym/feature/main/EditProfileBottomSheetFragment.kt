package com.sprm.apexgym.feature.main

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.ActualizarMiPerfilRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.auth.LoginFragment
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class EditProfileBottomSheetFragment : BottomSheetDialogFragment() {

    private var selectedGender: String = "Masculino"
    private var guardandoCambios: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile_sheet, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCerrarEditProfile = view.findViewById<View>(R.id.btnCerrarEditProfile)
        val tvEditProfileLoading = view.findViewById<TextView>(R.id.tvEditProfileLoading)
        val llEditProfileFormContainer = view.findViewById<View>(R.id.llEditProfileFormContainer)

        val tilEditUsuario = view.findViewById<TextInputLayout>(R.id.tilEditUsuario)
        val etEditUsuario = view.findViewById<TextInputEditText>(R.id.etEditUsuario)
        val tilEditCorreo = view.findViewById<TextInputLayout>(R.id.tilEditCorreo)
        val etEditCorreo = view.findViewById<TextInputEditText>(R.id.etEditCorreo)

        val tilEditNombre = view.findViewById<TextInputLayout>(R.id.tilEditNombre)
        val etEditNombre = view.findViewById<TextInputEditText>(R.id.etEditNombre)
        val tilEditApellidoPaterno = view.findViewById<TextInputLayout>(R.id.tilEditApellidoPaterno)
        val etEditApellidoPaterno = view.findViewById<TextInputEditText>(R.id.etEditApellidoPaterno)
        val tilEditApellidoMaterno = view.findViewById<TextInputLayout>(R.id.tilEditApellidoMaterno)
        val etEditApellidoMaterno = view.findViewById<TextInputEditText>(R.id.etEditApellidoMaterno)
        val tilEditFechaNacimiento = view.findViewById<TextInputLayout>(R.id.tilEditFechaNacimiento)
        val etEditFechaNacimiento = view.findViewById<TextInputEditText>(R.id.etEditFechaNacimiento)

        val chipMasculino = view.findViewById<TextView>(R.id.chipEditGeneroMasculino)
        val chipFemenino = view.findViewById<TextView>(R.id.chipEditGeneroFemenino)
        val chipOtro = view.findViewById<TextView>(R.id.chipEditGeneroOtro)
        val genderChips = listOf(chipMasculino, chipFemenino, chipOtro)

        val tilEditTelefono = view.findViewById<TextInputLayout>(R.id.tilEditTelefono)
        val etEditTelefono = view.findViewById<TextInputEditText>(R.id.etEditTelefono)
        val tilEditDireccion = view.findViewById<TextInputLayout>(R.id.tilEditDireccion)
        val etEditDireccion = view.findViewById<TextInputEditText>(R.id.etEditDireccion)

        val tilEditEmergenciaNombre = view.findViewById<TextInputLayout>(R.id.tilEditEmergenciaNombre)
        val etEditEmergenciaNombre = view.findViewById<TextInputEditText>(R.id.etEditEmergenciaNombre)
        val tilEditEmergenciaTelefono = view.findViewById<TextInputLayout>(R.id.tilEditEmergenciaTelefono)
        val etEditEmergenciaTelefono = view.findViewById<TextInputEditText>(R.id.etEditEmergenciaTelefono)

        val tilEditContrasenaActual = view.findViewById<TextInputLayout>(R.id.tilEditContrasenaActual)
        val etEditContrasenaActual = view.findViewById<TextInputEditText>(R.id.etEditContrasenaActual)
        val tilEditNuevaContrasena = view.findViewById<TextInputLayout>(R.id.tilEditNuevaContrasena)
        val etEditNuevaContrasena = view.findViewById<TextInputEditText>(R.id.etEditNuevaContrasena)
        val tilEditConfirmarContrasena = view.findViewById<TextInputLayout>(R.id.tilEditConfirmarContrasena)
        val etEditConfirmarContrasena = view.findViewById<TextInputEditText>(R.id.etEditConfirmarContrasena)

        val btnGuardarMiPerfil = view.findViewById<MaterialButton>(R.id.btnGuardarMiPerfil)

        etEditUsuario.doAfterTextChanged { tilEditUsuario.error = null }
        etEditCorreo.doAfterTextChanged { tilEditCorreo.error = null }
        etEditNombre.doAfterTextChanged { tilEditNombre.error = null }
        etEditApellidoPaterno.doAfterTextChanged { tilEditApellidoPaterno.error = null }
        etEditApellidoMaterno.doAfterTextChanged { tilEditApellidoMaterno.error = null }
        etEditFechaNacimiento.doAfterTextChanged { tilEditFechaNacimiento.error = null }
        etEditTelefono.doAfterTextChanged { tilEditTelefono.error = null }
        etEditDireccion.doAfterTextChanged { tilEditDireccion.error = null }
        etEditEmergenciaNombre.doAfterTextChanged { tilEditEmergenciaNombre.error = null }
        etEditEmergenciaTelefono.doAfterTextChanged { tilEditEmergenciaTelefono.error = null }
        etEditContrasenaActual.doAfterTextChanged { tilEditContrasenaActual.error = null }
        etEditNuevaContrasena.doAfterTextChanged { tilEditNuevaContrasena.error = null }
        etEditConfirmarContrasena.doAfterTextChanged { tilEditConfirmarContrasena.error = null }

        btnCerrarEditProfile?.setOnClickListener {
            if (!guardandoCambios) dismiss()
        }

        genderChips.forEach { chip ->
            chip.setOnClickListener {
                selectedGender = chip.text.toString()
                actualizarChipsGenero(genderChips, chip)
            }
        }

        etEditFechaNacimiento.setOnClickListener {
            mostrarSelectorFecha(etEditFechaNacimiento)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.obtenerMiPerfil()
                if (!isAdded) return@launch

                if (response.code() == 401) {
                    manejarSesionExpirada()
                    return@launch
                }

                val body = response.body()
                if (response.isSuccessful && body?.responseFlag == 0 && body.data != null) {
                    val usuario = body.data.usuario
                    val cliente = body.data.cliente

                    etEditUsuario.setText(usuario?.nombre.orEmpty())
                    etEditCorreo.setText(usuario?.email ?: cliente?.correo.orEmpty())

                    etEditNombre.setText(cliente?.nombre.orEmpty())
                    etEditApellidoPaterno.setText(cliente?.apellidoPaterno.orEmpty())
                    etEditApellidoMaterno.setText(cliente?.apellidoMaterno.orEmpty())

                    val fechaLimpia = cliente?.fechaNacimiento
                        ?.takeIf { it.isNotBlank() }
                        ?.split("T")
                        ?.firstOrNull()
                        .orEmpty()
                    etEditFechaNacimiento.setText(fechaLimpia)

                    val generoCliente = cliente?.genero?.trim().orEmpty()
                    val chipSeleccionado = when {
                        generoCliente.equals("Femenino", ignoreCase = true) -> chipFemenino
                        generoCliente.equals("Otro", ignoreCase = true) -> chipOtro
                        else -> chipMasculino
                    }
                    selectedGender = chipSeleccionado.text.toString()
                    actualizarChipsGenero(genderChips, chipSeleccionado)

                    etEditTelefono.setText(cliente?.telefono.orEmpty())
                    etEditDireccion.setText(cliente?.direccion.orEmpty())
                    etEditEmergenciaNombre.setText(cliente?.contactoEmergenciaNombre.orEmpty())
                    etEditEmergenciaTelefono.setText(cliente?.contactoEmergenciaTelefono.orEmpty())

                    tvEditProfileLoading.visibility = View.GONE
                    llEditProfileFormContainer.visibility = View.VISIBLE
                } else {
                    val msg = extraerMensajeError(response.errorBody()?.string())
                        ?: body?.message
                        ?: "No se pudo cargar tu información de perfil"
                    tvEditProfileLoading.text = msg
                }
            } catch (_: IOException) {
                if (isAdded) {
                    tvEditProfileLoading.text = "No se pudo conectar con el servidor. Verifica tu conexión."
                }
            } catch (e: Exception) {
                if (isAdded) {
                    tvEditProfileLoading.text = "Error al cargar el perfil: ${e.localizedMessage ?: "desconocido"}"
                }
            }
        }

        btnGuardarMiPerfil.setOnClickListener {
            if (guardandoCambios) return@setOnClickListener

            val usuarioStr = etEditUsuario.text?.toString()?.trim().orEmpty()
            val correoStr = etEditCorreo.text?.toString()?.trim().orEmpty()
            val nombreStr = etEditNombre.text?.toString()?.trim().orEmpty()
            val apellidoPaternoStr = etEditApellidoPaterno.text?.toString()?.trim().orEmpty()
            val apellidoMaternoStr = etEditApellidoMaterno.text?.toString()?.trim().orEmpty()
            val fechaNacimientoStr = etEditFechaNacimiento.text?.toString()?.trim().orEmpty()
            val telefonoStr = etEditTelefono.text?.toString()?.trim().orEmpty()
            val direccionStr = etEditDireccion.text?.toString()?.trim().orEmpty()
            val emergenciaNombreStr = etEditEmergenciaNombre.text?.toString()?.trim().orEmpty()
            val emergenciaTelefonoStr = etEditEmergenciaTelefono.text?.toString()?.trim().orEmpty()

            val contrasenaActualStr = etEditContrasenaActual.text?.toString().orEmpty()
            val nuevaContrasenaStr = etEditNuevaContrasena.text?.toString().orEmpty()
            val confirmarContrasenaStr = etEditConfirmarContrasena.text?.toString().orEmpty()

            var valido = true

            if (usuarioStr.length < 3) {
                tilEditUsuario.error = "El usuario debe tener al menos 3 caracteres"
                valido = false
            }

            if (correoStr.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correoStr).matches()) {
                tilEditCorreo.error = "Ingresa un correo electrónico válido"
                valido = false
            }

            if (nombreStr.isEmpty()) {
                tilEditNombre.error = "Ingresa tu nombre"
                valido = false
            }

            if (apellidoPaternoStr.isEmpty()) {
                tilEditApellidoPaterno.error = "Ingresa tu apellido paterno"
                valido = false
            }

            if (fechaNacimientoStr.isEmpty()) {
                tilEditFechaNacimiento.error = "Selecciona tu fecha de nacimiento"
                valido = false
            }

            if (!telefonoStr.matches(Regex("^\\d{10}$"))) {
                tilEditTelefono.error = "Ingresa un teléfono válido de 10 dígitos"
                valido = false
            }

            if (emergenciaTelefonoStr.isNotEmpty() && !emergenciaTelefonoStr.matches(Regex("^\\d{10}$"))) {
                tilEditEmergenciaTelefono.error = "El teléfono de emergencia debe tener 10 dígitos"
                valido = false
            }

            val quiereCambiarContrasena = contrasenaActualStr.isNotEmpty() ||
                nuevaContrasenaStr.isNotEmpty() ||
                confirmarContrasenaStr.isNotEmpty()

            if (quiereCambiarContrasena) {
                if (contrasenaActualStr.isEmpty()) {
                    tilEditContrasenaActual.error = "Ingresa tu contraseña actual"
                    valido = false
                }
                if (nuevaContrasenaStr.length < 8) {
                    tilEditNuevaContrasena.error = "La nueva contraseña debe tener al menos 8 caracteres"
                    valido = false
                }
                if (confirmarContrasenaStr != nuevaContrasenaStr) {
                    tilEditConfirmarContrasena.error = "Las contraseñas nuevas no coinciden"
                    valido = false
                }
            }

            if (!valido) return@setOnClickListener

            val request = ActualizarMiPerfilRequest(
                usuario = usuarioStr,
                email = correoStr,
                contraseniaActual = if (quiereCambiarContrasena) contrasenaActualStr else null,
                nuevaContrasenia = if (quiereCambiarContrasena) nuevaContrasenaStr else null,
                nombre = nombreStr,
                apellidoPaterno = apellidoPaternoStr,
                apellidoMaterno = apellidoMaternoStr,
                fechaNacimiento = fechaNacimientoStr,
                genero = selectedGender,
                telefono = telefonoStr,
                direccion = direccionStr,
                contactoEmergenciaNombre = emergenciaNombreStr,
                contactoEmergenciaTelefono = emergenciaTelefonoStr
            )

            guardandoCambios = true
            btnGuardarMiPerfil.isEnabled = false
            btnGuardarMiPerfil.text = "Guardando cambios..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.authService.actualizarMiPerfil(request)
                    if (!isAdded) return@launch

                    if (response.code() == 401) {
                        manejarSesionExpirada()
                        return@launch
                    }

                    val body = response.body()
                    if (response.isSuccessful && body?.responseFlag == 0) {
                        val nombreCompleto = listOfNotNull(
                            nombreStr.takeIf { it.isNotBlank() },
                            apellidoPaternoStr.takeIf { it.isNotBlank() }
                        ).joinToString(" ")

                        parentFragmentManager.setFragmentResult(
                            RESULT_KEY_PERFIL_ACTUALIZADO,
                            bundleOf("nombreCompleto" to nombreCompleto)
                        )

                        Toast.makeText(
                            requireContext(),
                            body.message ?: "Perfil actualizado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        dismissAllowingStateLoss()
                    } else {
                        val msg = extraerMensajeError(response.errorBody()?.string())
                            ?: body?.message
                            ?: "No se pudo actualizar el perfil (${response.code()})"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                } catch (_: IOException) {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "No se pudo conectar con el servidor. Verifica tu conexión.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Error al actualizar: ${e.localizedMessage ?: "desconocido"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    guardandoCambios = false
                    if (isAdded) {
                        btnGuardarMiPerfil.isEnabled = true
                        btnGuardarMiPerfil.text = "Guardar Cambios"
                    }
                }
            }
        }
    }

    private fun mostrarSelectorFecha(targetInput: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val textoActual = targetInput.text?.toString()?.trim().orEmpty()
        val partes = textoActual.split("-")
        if (partes.size == 3) {
            val y = partes[0].toIntOrNull()
            val m = partes[1].toIntOrNull()
            val d = partes[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                calendar.set(y, m - 1, d)
            } else {
                calendar.set(2000, Calendar.JANUARY, 1)
            }
        } else {
            calendar.set(2000, Calendar.JANUARY, 1)
        }

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

    private fun manejarSesionExpirada() {
        RetrofitClient.cookieJar.clear()
        if (!isAdded) return
        val hostFragmentManager = requireActivity().supportFragmentManager
        Toast.makeText(
            requireContext(),
            "Tu sesión ha expirado. Inicia sesión nuevamente.",
            Toast.LENGTH_LONG
        ).show()
        dismissAllowingStateLoss()
        hostFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        hostFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    private fun extraerMensajeError(rawJson: String?): String? {
        if (rawJson.isNullOrBlank()) return null
        return try {
            JSONObject(rawJson).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val TAG = "EditProfileBottomSheet"
        const val RESULT_KEY_PERFIL_ACTUALIZADO = "perfil_actualizado"
    }
}

package com.sprm.apexgym.feature.main

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.client.ActualizarMiPerfilRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.session.SessionManager
import com.sprm.apexgym.core.ui.InputFilters
import com.sprm.apexgym.core.ui.InputValidationRules
import com.sprm.apexgym.databinding.FragmentMiPerfilBinding
import com.sprm.apexgym.feature.auth.LoginFragment
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.Locale

class MiPerfilFragment : BaseFragment<FragmentMiPerfilBinding>() {

    private var selectedGender: String = "Masculino"
    private var guardandoCambios: Boolean = false

    override fun getLayoutRes(): Int = R.layout.fragment_mi_perfil

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener {
            if (!guardandoCambios) {
                parentFragmentManager.popBackStack()
            }
        }

        val genderChips = listOf(
            binding.chipPerfilGeneroMasculino,
            binding.chipPerfilGeneroFemenino,
            binding.chipPerfilGeneroOtro
        )

        binding.etPerfilUsuario.filters = InputFilters.username(40)
        binding.etPerfilCorreo.filters = InputFilters.noWhitespace(100)
        binding.etPerfilNombre.filters = InputFilters.personName(60)
        binding.etPerfilApellidoPaterno.filters = InputFilters.personName(60)
        binding.etPerfilApellidoMaterno.filters = InputFilters.personName(60)
        binding.etPerfilTelefono.filters = InputFilters.digitsOnly(10)
        binding.etPerfilDireccion.filters = InputFilters.plainText(200)
        binding.etPerfilEmergenciaNombre.filters = InputFilters.personName(80)
        binding.etPerfilEmergenciaTelefono.filters = InputFilters.digitsOnly(10)

        binding.etPerfilUsuario.doAfterTextChanged { binding.tilPerfilUsuario.error = null }
        binding.etPerfilCorreo.doAfterTextChanged { binding.tilPerfilCorreo.error = null }
        binding.etPerfilNombre.doAfterTextChanged { binding.tilPerfilNombre.error = null }
        binding.etPerfilApellidoPaterno.doAfterTextChanged { binding.tilPerfilApellidoPaterno.error = null }
        binding.etPerfilApellidoMaterno.doAfterTextChanged { binding.tilPerfilApellidoMaterno.error = null }
        binding.etPerfilFechaNacimiento.doAfterTextChanged { binding.tilPerfilFechaNacimiento.error = null }
        binding.etPerfilTelefono.doAfterTextChanged { binding.tilPerfilTelefono.error = null }
        binding.etPerfilDireccion.doAfterTextChanged { binding.tilPerfilDireccion.error = null }
        binding.etPerfilEmergenciaNombre.doAfterTextChanged { binding.tilPerfilEmergenciaNombre.error = null }
        binding.etPerfilEmergenciaTelefono.doAfterTextChanged { binding.tilPerfilEmergenciaTelefono.error = null }

        genderChips.forEach { chip ->
            chip.setOnClickListener {
                selectedGender = chip.text.toString()
                actualizarChipsGenero(genderChips, chip)
            }
        }

        binding.etPerfilFechaNacimiento.setOnClickListener {
            mostrarSelectorFecha(binding.etPerfilFechaNacimiento)
        }

        binding.tvMiPerfilHeroNombre.text = SessionManager.obtenerNombre(requireContext())
            ?.takeIf { it.isNotBlank() } ?: "Socio ApexGym"
        binding.tvMiPerfilHeroCorreo.text = SessionManager.obtenerEmail(requireContext())
            ?.takeIf { it.isNotBlank() } ?: "Actualiza tus datos personales y de contacto"

        cargarPerfil(genderChips)

        binding.btnGuardarMiPerfil.setOnClickListener {
            guardarPerfil()
        }
    }

    private fun cargarPerfil(genderChips: List<TextView>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.obtenerMiPerfil()
                if (!isAdded || view == null) return@launch

                if (response.code() == 401) {
                    manejarSesionExpirada()
                    return@launch
                }

                val body = response.body()
                if (response.isSuccessful && body?.responseFlag == 0 && body.data != null) {
                    val usuario = body.data.usuario
                    val cliente = body.data.cliente

                    val nombreHero = listOfNotNull(
                        cliente?.nombre?.trim()?.takeIf { it.isNotEmpty() },
                        cliente?.apellidoPaterno?.trim()?.takeIf { it.isNotEmpty() },
                        cliente?.apellidoMaterno?.trim()?.takeIf { it.isNotEmpty() }
                    ).joinToString(" ").ifBlank { usuario?.nombre.orEmpty() }
                    if (nombreHero.isNotBlank()) {
                        binding.tvMiPerfilHeroNombre.text = nombreHero
                    }
                    val correoActual = usuario?.email ?: cliente?.correo.orEmpty()
                    if (correoActual.isNotBlank()) {
                        binding.tvMiPerfilHeroCorreo.text = correoActual
                    }

                    binding.etPerfilUsuario.setText(usuario?.nombre.orEmpty())
                    binding.etPerfilCorreo.setText(correoActual)

                    binding.etPerfilNombre.setText(cliente?.nombre.orEmpty())
                    binding.etPerfilApellidoPaterno.setText(cliente?.apellidoPaterno.orEmpty())
                    binding.etPerfilApellidoMaterno.setText(cliente?.apellidoMaterno.orEmpty())

                    val fechaLimpia = cliente?.fechaNacimiento
                        ?.takeIf { it.isNotBlank() }
                        ?.split("T")
                        ?.firstOrNull()
                        .orEmpty()
                    binding.etPerfilFechaNacimiento.setText(fechaLimpia)

                    val generoCliente = cliente?.genero?.trim().orEmpty()
                    val chipSeleccionado = when {
                        generoCliente.equals("Femenino", ignoreCase = true) -> binding.chipPerfilGeneroFemenino
                        generoCliente.equals("Otro", ignoreCase = true) -> binding.chipPerfilGeneroOtro
                        else -> binding.chipPerfilGeneroMasculino
                    }
                    selectedGender = chipSeleccionado.text.toString()
                    actualizarChipsGenero(genderChips, chipSeleccionado)

                    binding.etPerfilTelefono.setText(cliente?.telefono.orEmpty())
                    binding.etPerfilDireccion.setText(cliente?.direccion.orEmpty())
                    binding.etPerfilEmergenciaNombre.setText(cliente?.contactoEmergenciaNombre.orEmpty())
                    binding.etPerfilEmergenciaTelefono.setText(cliente?.contactoEmergenciaTelefono.orEmpty())

                    binding.tvMiPerfilLoading.visibility = View.GONE
                    binding.llMiPerfilFormContainer.visibility = View.VISIBLE
                } else {
                    val msg = extraerMensajeError(response.errorBody()?.string())
                        ?: body?.message
                        ?: "No se pudo cargar tu información de perfil"
                    binding.tvMiPerfilLoading.text = msg
                }
            } catch (_: IOException) {
                if (isAdded && view != null) {
                    binding.tvMiPerfilLoading.text =
                        "No se pudo conectar con el servidor. Verifica tu conexión."
                }
            } catch (e: Exception) {
                if (isAdded && view != null) {
                    binding.tvMiPerfilLoading.text =
                        "Error al cargar el perfil: ${e.localizedMessage ?: "desconocido"}"
                }
            }
        }
    }

    private fun guardarPerfil() {
        if (guardandoCambios) return

        val usuarioStr = binding.etPerfilUsuario.text?.toString()?.trim().orEmpty()
        val correoStr = binding.etPerfilCorreo.text?.toString()?.trim().orEmpty()
        val nombreStr = binding.etPerfilNombre.text?.toString()?.trim().orEmpty()
        val apellidoPaternoStr = binding.etPerfilApellidoPaterno.text?.toString()?.trim().orEmpty()
        val apellidoMaternoStr = binding.etPerfilApellidoMaterno.text?.toString()?.trim().orEmpty()
        val fechaNacimientoStr = binding.etPerfilFechaNacimiento.text?.toString()?.trim().orEmpty()
        val telefonoStr = binding.etPerfilTelefono.text?.toString()?.trim().orEmpty()
        val direccionStr = binding.etPerfilDireccion.text?.toString()?.trim().orEmpty()
        val emergenciaNombreStr = binding.etPerfilEmergenciaNombre.text?.toString()?.trim().orEmpty()
        val emergenciaTelefonoStr = binding.etPerfilEmergenciaTelefono.text?.toString()?.trim().orEmpty()

        var valido = true

        if (usuarioStr.length < 3) {
            binding.tilPerfilUsuario.error = "El usuario debe tener al menos 3 caracteres"
            valido = false
        }
        if (correoStr.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correoStr).matches()) {
            binding.tilPerfilCorreo.error = "Ingresa un correo electrónico válido"
            valido = false
        }
        if (!InputValidationRules.esNombrePersonaValido(nombreStr)) {
            binding.tilPerfilNombre.error = "Ingresa un nombre válido (solo letras)"
            valido = false
        }
        if (!InputValidationRules.esNombrePersonaValido(apellidoPaternoStr)) {
            binding.tilPerfilApellidoPaterno.error = "Ingresa un apellido paterno válido (solo letras)"
            valido = false
        }
        if (apellidoMaternoStr.isNotEmpty() && !InputValidationRules.esNombrePersonaValido(apellidoMaternoStr)) {
            binding.tilPerfilApellidoMaterno.error = "El apellido materno solo puede contener letras"
            valido = false
        }
        if (fechaNacimientoStr.isEmpty()) {
            binding.tilPerfilFechaNacimiento.error = "Selecciona tu fecha de nacimiento"
            valido = false
        }
        if (!telefonoStr.matches(Regex("^\\d{10}$"))) {
            binding.tilPerfilTelefono.error = "Ingresa un teléfono válido de 10 dígitos"
            valido = false
        }
        if (emergenciaNombreStr.isNotEmpty() && !InputValidationRules.esNombrePersonaValido(emergenciaNombreStr)) {
            binding.tilPerfilEmergenciaNombre.error = "El nombre de contacto solo puede contener letras"
            valido = false
        }
        if (emergenciaTelefonoStr.isNotEmpty() && !emergenciaTelefonoStr.matches(Regex("^\\d{10}$"))) {
            binding.tilPerfilEmergenciaTelefono.error = "El teléfono de emergencia debe tener 10 dígitos"
            valido = false
        }

        if (!valido) return

        val request = ActualizarMiPerfilRequest(
            usuario = usuarioStr,
            email = correoStr,
            contraseniaActual = null,
            nuevaContrasenia = null,
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
        binding.btnGuardarMiPerfil.isEnabled = false
        binding.btnGuardarMiPerfil.text = "Guardando cambios..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.actualizarMiPerfil(request)
                if (!isAdded || view == null) return@launch

                if (response.code() == 401) {
                    manejarSesionExpirada()
                    return@launch
                }

                val body = response.body()
                if (response.isSuccessful && body?.responseFlag == 0) {
                    val nombreCompleto = listOfNotNull(
                        nombreStr.takeIf { it.isNotBlank() },
                        apellidoPaternoStr.takeIf { it.isNotBlank() },
                        apellidoMaternoStr.takeIf { it.isNotBlank() }
                    ).joinToString(" ")

                    val ctx = requireContext()
                    SessionManager.guardar(
                        context = ctx,
                        usuarioId = SessionManager.obtenerUsuarioId(ctx),
                        nombre = nombreCompleto.ifBlank { SessionManager.obtenerNombre(ctx) },
                        email = correoStr.ifBlank { SessionManager.obtenerEmail(ctx) },
                        rol = SessionManager.obtenerRol(ctx) ?: SessionManager.ROL_CLIENTE,
                        empleadoId = SessionManager.obtenerEmpleadoId(ctx),
                        sucursalId = SessionManager.obtenerSucursalId(ctx)
                    )

                    Toast.makeText(
                        ctx,
                        body.message ?: "Perfil actualizado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()
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
                if (isAdded && view != null) {
                    binding.btnGuardarMiPerfil.isEnabled = true
                    binding.btnGuardarMiPerfil.text = "Guardar Cambios"
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
        val ctx = requireContext()
        SessionManager.limpiar(ctx)
        CartManager.vaciarCarrito(ctx)
        Toast.makeText(
            ctx,
            "Tu sesión ha expirado. Inicia sesión nuevamente.",
            Toast.LENGTH_LONG
        ).show()
        val fm = requireActivity().supportFragmentManager
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fm.beginTransaction()
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
}

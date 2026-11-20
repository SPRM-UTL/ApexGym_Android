package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.ClienteAsistenciaDto
import com.sprm.apexgym.core.network.api.ClienteMembresiaAsistenciaDto
import com.sprm.apexgym.core.network.api.ValidarAccesoRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentAccesoManualBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AccesoManualFragment : BaseFragment<FragmentAccesoManualBinding>() {

    private var clienteActual: ClienteAsistenciaDto? = null
    private var membresiaActual: ClienteMembresiaAsistenciaDto? = null

    override fun getLayoutRes(): Int = R.layout.fragment_acceso_manual

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.cvClientFound.visibility = View.GONE
        binding.cvClientNotFound.visibility = View.GONE
        binding.llLoading.visibility = View.GONE

        val dispararBusqueda = {
            val query = binding.etManualSearch.text?.toString()?.trim().orEmpty()
            if (query.length < 2) {
                Toast.makeText(
                    requireContext(),
                    "Ingresa al menos 2 caracteres (nombre, correo o teléfono)",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                buscarCliente(query)
            }
        }

        binding.btnBuscar.setOnClickListener { dispararBusqueda() }
        binding.etManualSearch.setOnEditorActionListener { _, _, _ ->
            dispararBusqueda()
            true
        }

        binding.btnGrantAccess.setOnClickListener { confirmarPermitirAcceso() }
        binding.btnDenyAccess.setOnClickListener { confirmarDenegarAcceso() }

        val clienteId = arguments?.getInt(ARG_CLIENTE_ID, 0) ?: 0
        if (clienteId > 0) {
            cargarClientePorId(clienteId)
        }
    }

    private fun buscarCliente(termino: String) {
        mostrarCargando(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.buscarCliente(termino)
                val encontrados = response.dataExitosa()
                if (!isAdded) return@launch

                if (encontrados.isNullOrEmpty()) {
                    mostrarNoEncontrado()
                    return@launch
                }

                mostrarCliente(encontrados.first())
            } catch (_: Exception) {
                if (isAdded) mostrarNoEncontrado()
            }
        }
    }

    private fun cargarClientePorId(clienteId: Int) {
        mostrarCargando(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerClientes()
                val cliente = response.dataExitosa()?.firstOrNull { it.id == clienteId }
                if (!isAdded) return@launch
                if (cliente == null) {
                    mostrarNoEncontrado()
                } else {
                    mostrarCliente(cliente)
                }
            } catch (_: Exception) {
                if (isAdded) mostrarNoEncontrado()
            }
        }
    }

    private suspend fun cargarMembresia(clienteId: Int) {
        membresiaActual = try {
            RetrofitClient.empleadoService.obtenerMembresiaActiva(clienteId).dataExitosa()
        } catch (_: Exception) {
            null
        }
    }

    private fun mostrarCliente(cliente: ClienteAsistenciaDto) {
        clienteActual = cliente

        viewLifecycleOwner.lifecycleScope.launch {
            cargarMembresia(cliente.id)
            if (!isAdded) return@launch

            val membresia = membresiaActual
            val vigente = membresia != null &&
                membresia.estadoMembresia?.nombre.equals("Vigente", ignoreCase = true)

            binding.cvClientFound.visibility = View.VISIBLE
            binding.cvClientNotFound.visibility = View.GONE
            binding.llLoading.visibility = View.GONE

            binding.tvClientName.text = cliente.nombreCompleto.ifBlank { "Socio" }
            val contacto = listOfNotNull(
                cliente.correo?.takeIf { it.isNotBlank() },
                cliente.telefono?.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
            binding.tvClientCode.text = contacto.ifBlank { "Sin datos de contacto" }

            binding.tvClientStatus.text = if (vigente) "VIGENTE" else "SIN PLAN"
            binding.tvClientStatus.setBackgroundResource(
                if (vigente) R.drawable.bg_badge_orange else R.drawable.bg_chip_unselected
            )
            binding.tvClientStatus.setTextColor(
                requireContext().getColor(
                    if (vigente) R.color.white else R.color.apex_text_secondary
                )
            )

            binding.tvClientPlan.text = membresia?.tipoMembresia?.nombre ?: "Sin membresía"
            binding.tvClientExpiry.text = membresia?.fechaFin
                ?.let { formatearFecha(it, "dd/MM/yyyy") }
                ?: "—"
            binding.tvClientDaysLeft.text = calcularDiasRestantes(membresia?.fechaFin)

            binding.btnGrantAccess.isEnabled = vigente
            binding.btnGrantAccess.alpha = if (vigente) 1f else 0.45f
            binding.btnDenyAccess.isEnabled = true
        }
    }

    private fun confirmarPermitirAcceso() {
        val cliente = clienteActual ?: return
        val plan = membresiaActual?.tipoMembresia?.nombre ?: "Membresía vigente"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar entrada")
            .setMessage("¿Registrar asistencia para ${cliente.nombreCompleto}?\nPlan: $plan")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                registrarAcceso()
            }
            .show()
    }

    private fun confirmarDenegarAcceso() {
        val cliente = clienteActual ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Denegar entrada")
            .setMessage("¿Deseas cancelar el acceso para ${cliente.nombreCompleto}?")
            .setNegativeButton("Volver", null)
            .setPositiveButton("Denegar") { _, _ ->
                limpiarResultado()
                Toast.makeText(
                    requireContext(),
                    "Acceso denegado para ${cliente.nombreCompleto}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun limpiarResultado() {
        clienteActual = null
        membresiaActual = null
        binding.cvClientFound.visibility = View.GONE
        binding.cvClientNotFound.visibility = View.GONE
        binding.etManualSearch.setText("")
    }

    private fun mostrarNoEncontrado() {
        clienteActual = null
        membresiaActual = null
        binding.cvClientFound.visibility = View.GONE
        binding.cvClientNotFound.visibility = View.VISIBLE
        binding.llLoading.visibility = View.GONE
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.llLoading.visibility = if (cargando) View.VISIBLE else View.GONE
        if (cargando) {
            binding.cvClientFound.visibility = View.GONE
            binding.cvClientNotFound.visibility = View.GONE
        }
    }

    private fun registrarAcceso() {
        val cliente = clienteActual ?: return
        binding.btnGrantAccess.isEnabled = false
        binding.btnGrantAccess.text = "Registrando…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.validarYRegistrarAcceso(
                    ValidarAccesoRequest(clienteId = cliente.id)
                )
                val resultado = response.dataExitosa()
                if (!isAdded) return@launch

                val nombre = cliente.nombreCompleto
                val plan = membresiaActual?.tipoMembresia?.nombre.orEmpty()

                if (resultado == null) {
                    val errorMsg = response.mensajeDeError() ?: "No se pudo registrar el acceso"
                    AccesoResultadoDialogFragment.newInstance(
                        success = false,
                        mensaje = errorMsg,
                        nombre = nombre,
                        plan = plan,
                        botonPrimario = "Entendido"
                    ).show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                    return@launch
                }

                if (resultado.acceso) {
                    val dialog = AccesoResultadoDialogFragment.newInstance(
                        success = true,
                        mensaje = "Entrada registrada correctamente",
                        nombre = nombre,
                        plan = plan,
                        botonPrimario = "Listo"
                    )
                    dialog.onDismissAction = { limpiarResultado() }
                    dialog.show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                } else {
                    AccesoResultadoDialogFragment.newInstance(
                        success = false,
                        mensaje = resultado.mensaje,
                        nombre = nombre,
                        plan = plan,
                        botonPrimario = "Entendido"
                    ).show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo conectar con el servidor",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                if (isAdded) {
                    binding.btnGrantAccess.isEnabled = true
                    binding.btnGrantAccess.text = "Permitir Acceso"
                }
            }
        }
    }

    private fun calcularDiasRestantes(fechaFin: String?): String {
        if (fechaFin.isNullOrBlank()) return "—"
        return try {
            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fechaFin.take(10))
                ?: return "—"
            val dias = ((fecha.time - System.currentTimeMillis()) / 86_400_000L).toInt()
            when {
                dias < 0 -> "Vencida"
                dias == 0 -> "Vence hoy"
                dias == 1 -> "1 día"
                else -> "$dias días"
            }
        } catch (_: Exception) {
            "—"
        }
    }

    companion object {
        private const val ARG_CLIENTE_ID = "arg_cliente_id"

        fun newInstance(clienteId: Int): AccesoManualFragment {
            return AccesoManualFragment().apply {
                arguments = Bundle().apply { putInt(ARG_CLIENTE_ID, clienteId) }
            }
        }
    }
}

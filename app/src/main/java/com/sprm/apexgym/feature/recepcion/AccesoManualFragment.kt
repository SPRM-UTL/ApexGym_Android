package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
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

        binding.btnBuscar.setOnClickListener {
            val query = binding.etManualSearch.text?.toString()?.trim().orEmpty()
            if (query.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa nombre, correo o teléfono", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buscarCliente(query)
        }

        binding.btnGrantAccess.setOnClickListener { registrarAcceso() }

        binding.btnDenyAccess.setOnClickListener {
            AccesoResultadoDialogFragment.newInstance(false)
                .show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
        }

        binding.btnNuevoCliente.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "El alta de clientes desde recepción aún no está disponible en la app.",
                Toast.LENGTH_LONG
            ).show()
        }

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

                if (encontrados.isNullOrEmpty()) {
                    mostrarNoEncontrado(
                        response.mensajeDeError() ?: "No se encontró ningún cliente"
                    )
                    return@launch
                }

                mostrarCliente(encontrados.first())
            } catch (_: Exception) {
                mostrarNoEncontrado("No se pudo conectar con el servidor")
            }
        }
    }

    private fun cargarClientePorId(clienteId: Int) {
        mostrarCargando(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerClientes()
                val cliente = response.dataExitosa()?.firstOrNull { it.id == clienteId }
                if (cliente == null) {
                    mostrarNoEncontrado("No se encontró el cliente seleccionado")
                } else {
                    mostrarCliente(cliente)
                }
            } catch (_: Exception) {
                mostrarNoEncontrado("No se pudo conectar con el servidor")
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
            binding.cvClientFound.visibility = View.VISIBLE
            binding.cvClientNotFound.visibility = View.GONE
            binding.llLoading.visibility = View.GONE

            binding.tvClientName.text = cliente.nombreCompleto.ifBlank { "Cliente #${cliente.id}" }
            binding.tvClientCode.text = "Cliente #${cliente.id} • ${cliente.correo ?: "sin correo"}"
            binding.tvClientStatus.text = cliente.estadoCliente?.nombre ?: "Estado no disponible"
            binding.tvClientPlan.text = membresia?.tipoMembresia?.nombre
                ?.let { "Plan: $it" }
                ?: "Sin membresía activa"
            binding.tvClientExpiry.text = membresia?.fechaFin
                ?.let { "Vence: ${formatearFecha(it, "dd/MM/yyyy")}" }
                ?: "Sin fecha de vencimiento"
            binding.tvClientDaysLeft.text = calcularDiasRestantes(membresia?.fechaFin)

            binding.btnGrantAccess.isEnabled = true
            binding.btnDenyAccess.isEnabled = true
        }
    }

    private fun mostrarNoEncontrado(mensaje: String) {
        clienteActual = null
        membresiaActual = null
        binding.cvClientFound.visibility = View.GONE
        binding.cvClientNotFound.visibility = View.VISIBLE
        binding.llLoading.visibility = View.GONE
        binding.tvClientName.text = mensaje
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.validarYRegistrarAcceso(
                    ValidarAccesoRequest(clienteId = cliente.id)
                )
                val resultado = response.dataExitosa()

                if (resultado == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo registrar el acceso",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnGrantAccess.isEnabled = true
                    return@launch
                }

                val nombre = cliente.nombreCompleto
                val plan = membresiaActual?.tipoMembresia?.nombre.orEmpty()

                if (resultado.acceso) {
                    AccesoResultadoDialogFragment.newInstance(
                        success = true,
                        nombre = nombre,
                        plan = plan
                    ).show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                } else {
                    AccesoResultadoDialogFragment.newInstance(
                        success = false,
                        mensaje = resultado.mensaje,
                        nombre = nombre,
                        plan = plan
                    ).show(parentFragmentManager, AccesoResultadoDialogFragment.TAG)
                }
                binding.btnGrantAccess.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al registrar acceso: ${e.localizedMessage ?: "desconocido"}",
                    Toast.LENGTH_LONG
                ).show()
                binding.btnGrantAccess.isEnabled = true
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
                dias < 0 -> "Membresía vencida"
                dias == 0 -> "Vence hoy"
                dias == 1 -> "1 día restante"
                else -> "$dias días restantes"
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

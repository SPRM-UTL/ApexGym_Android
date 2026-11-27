package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.sprm.apexgym.databinding.ItemClienteCoincidenciaBinding
import com.sprm.apexgym.feature.main.MembresiaVigenciaHelper
import kotlinx.coroutines.launch

class AccesoManualFragment : BaseFragment<FragmentAccesoManualBinding>() {

    private var clienteActual: ClienteAsistenciaDto? = null
    private var membresiaActual: ClienteMembresiaAsistenciaDto? = null
    private var coincidencias: List<ClienteAsistenciaDto> = emptyList()
    private val coincidenciasAdapter = CoincidenciasAdapter { seleccionado ->
        mostrarCliente(seleccionado)
    }

    override fun getLayoutRes(): Int = R.layout.fragment_acceso_manual

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.llResultadosBusqueda.visibility = View.GONE
        binding.cvClientFound.visibility = View.GONE
        binding.cvClientNotFound.visibility = View.GONE
        binding.llLoading.visibility = View.GONE

        binding.rvResultadosClientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResultadosClientes.adapter = coincidenciasAdapter

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
                if (!isAdded || view == null) return@launch

                if (encontrados.isNullOrEmpty()) {
                    coincidencias = emptyList()
                    binding.llResultadosBusqueda.visibility = View.GONE
                    mostrarNoEncontrado()
                    return@launch
                }

                coincidencias = encontrados
                if (encontrados.size == 1) {
                    binding.llResultadosBusqueda.visibility = View.GONE
                    mostrarCliente(encontrados.first())
                } else {
                    binding.llLoading.visibility = View.GONE
                    binding.cvClientNotFound.visibility = View.GONE
                    binding.cvClientFound.visibility = View.GONE
                    binding.llResultadosBusqueda.visibility = View.VISIBLE
                    binding.tvTituloCoincidencias.text =
                        "Coincidencias encontradas (${encontrados.size}) — Toca para revisar"
                    coincidenciasAdapter.submitList(encontrados, null)
                }
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
        if (coincidencias.size > 1) {
            coincidenciasAdapter.submitList(coincidencias, cliente.id)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            cargarMembresia(cliente.id)
            if (!isAdded || view == null) return@launch

            val membresia = membresiaActual
            val diasRestantes = MembresiaVigenciaHelper.calcularDiasRestantes(membresia?.fechaFin)
            val vigente = membresia != null &&
                membresia.estadoMembresia?.nombre.equals("Vigente", ignoreCase = true) &&
                (diasRestantes == null || diasRestantes >= 0L)

            binding.cvClientFound.visibility = View.VISIBLE
            binding.cvClientNotFound.visibility = View.GONE
            binding.llLoading.visibility = View.GONE

            binding.tvClientName.text = cliente.nombreCompleto.ifBlank { "Socio" }
            val contacto = listOfNotNull(
                cliente.correo?.takeIf { it.isNotBlank() },
                cliente.telefono?.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { "Sin correo ni teléfono registrados" }
            binding.tvClientCode.text = contacto

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
        }
    }

    private fun confirmarPermitirAcceso() {
        val cliente = clienteActual ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar entrada")
            .setMessage("¿Registrar asistencia para ${cliente.nombreCompleto}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                registrarAcceso()
            }
            .show()
    }

    private fun limpiarResultado() {
        clienteActual = null
        membresiaActual = null
        coincidencias = emptyList()
        binding.llResultadosBusqueda.visibility = View.GONE
        binding.cvClientFound.visibility = View.GONE
        binding.cvClientNotFound.visibility = View.GONE
        binding.etManualSearch.setText("")
    }

    private fun mostrarNoEncontrado() {
        clienteActual = null
        membresiaActual = null
        coincidencias = emptyList()
        binding.llResultadosBusqueda.visibility = View.GONE
        binding.cvClientFound.visibility = View.GONE
        binding.cvClientNotFound.visibility = View.VISIBLE
        binding.llLoading.visibility = View.GONE
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.llLoading.visibility = if (cargando) View.VISIBLE else View.GONE
        if (cargando) {
            binding.llResultadosBusqueda.visibility = View.GONE
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
        val dias = MembresiaVigenciaHelper.calcularDiasRestantes(fechaFin) ?: return "—"
        return when {
            dias < 0L -> "Vencida"
            dias == 0L -> "Vence hoy"
            dias == 1L -> "1 día"
            else -> "$dias días"
        }
    }

    private class CoincidenciasAdapter(
        private val onSelect: (ClienteAsistenciaDto) -> Unit
    ) : RecyclerView.Adapter<CoincidenciasAdapter.ViewHolder>() {

        private var items: List<ClienteAsistenciaDto> = emptyList()
        private var seleccionadoId: Int? = null

        fun submitList(nuevos: List<ClienteAsistenciaDto>, seleccionado: Int?) {
            items = nuevos
            seleccionadoId = seleccionado
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemClienteCoincidenciaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding, onSelect)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item, item.id == seleccionadoId)
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(
            private val itemBinding: ItemClienteCoincidenciaBinding,
            private val onSelect: (ClienteAsistenciaDto) -> Unit
        ) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(cliente: ClienteAsistenciaDto, seleccionado: Boolean) {
                val context = itemBinding.root.context
                itemBinding.tvCoincidenciaNombre.text = cliente.nombreCompleto.ifBlank { "Socio" }

                val contacto = listOfNotNull(
                    cliente.correo?.takeIf { it.isNotBlank() },
                    cliente.telefono?.takeIf { it.isNotBlank() }
                ).joinToString(" • ").ifBlank { "Sin correo ni teléfono registrados" }
                itemBinding.tvCoincidenciaIdentificador.text = contacto

                val estadoNombre = cliente.estadoCliente?.nombre?.takeIf { it.isNotBlank() } ?: "Socio"
                itemBinding.tvCoincidenciaEstado.text = estadoNombre
                val activo = estadoNombre.contains("ACTIV", ignoreCase = true)
                itemBinding.tvCoincidenciaEstado.setBackgroundResource(
                    if (activo) R.drawable.bg_badge_green else R.drawable.bg_badge_soft_neutral
                )
                itemBinding.tvCoincidenciaEstado.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (activo) R.color.white else R.color.apex_text_secondary
                    )
                )

                val strokePx = ((if (seleccionado) 2f else 1f) * context.resources.displayMetrics.density).toInt()
                itemBinding.cardCoincidenciaCliente.strokeWidth = strokePx
                itemBinding.cardCoincidenciaCliente.strokeColor = ContextCompat.getColor(
                    context,
                    if (seleccionado) R.color.apex_orange else R.color.apex_stroke_light
                )

                itemBinding.cardCoincidenciaCliente.setOnClickListener {
                    onSelect(cliente)
                }
            }
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

package com.sprm.apexgym.feature.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.client.ClienteMembresiaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.databinding.FragmentHistorialMembresiasBinding
import com.sprm.apexgym.databinding.ItemHistorialMembresiaBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

class HistorialMembresiasFragment : BaseFragment<FragmentHistorialMembresiasBinding>() {

    private val adapter = HistorialMembresiasAdapter()

    override fun getLayoutRes(): Int = R.layout.fragment_historial_membresias

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.rvMembershipTimeline.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembershipTimeline.adapter = adapter

        binding.swipeRefreshHistorialMembresias.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefreshHistorialMembresias.setOnRefreshListener {
            cargarHistorial()
        }

        cargarHistorial()
    }

    private fun cargarHistorial() {
        binding.swipeRefreshHistorialMembresias.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.obtenerMiMembresia()
                if (!isAdded || view == null) return@launch

                val body = response.body()
                if (response.isSuccessful && body?.responseFlag == 0 && body.data != null) {
                    val data = body.data
                    val combinadas = buildList {
                        data.membresiaActiva?.let { add(it) }
                        data.historial.orEmpty().forEach { item ->
                            if (none { it.id == item.id && item.id != 0 }) {
                                add(item)
                            }
                        }
                    }

                    actualizarResumen(combinadas)
                    adapter.submitList(combinadas)

                    val vacio = combinadas.isEmpty()
                    binding.rvMembershipTimeline.visibility = if (vacio) View.GONE else View.VISIBLE
                    binding.llEmptyMembresias.visibility = if (vacio) View.VISIBLE else View.GONE
                    if (vacio) {
                        binding.tvEmptyMembresiasTitulo.text = "No hay historial de membresías"
                        binding.tvEmptyMembresiasSubtitulo.text = "Tus planes contratados aparecerán aquí"
                    }
                } else {
                    mostrarEstadoError("No se pudo cargar tu historial")
                }
            } catch (_: Exception) {
                if (isAdded && view != null) {
                    mostrarEstadoError("Sin conexión para consultar tus membresías")
                }
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefreshHistorialMembresias.isRefreshing = false
                }
            }
        }
    }

    private fun mostrarEstadoError(mensaje: String) {
        if (adapter.itemCount == 0) {
            binding.rvMembershipTimeline.visibility = View.GONE
            binding.llEmptyMembresias.visibility = View.VISIBLE
            binding.tvEmptyMembresiasTitulo.text = mensaje
            binding.tvEmptyMembresiasSubtitulo.text = "Desliza hacia abajo para reintentar"
        }
    }

    private fun actualizarResumen(lista: List<ClienteMembresiaDto>) {
        binding.tvTotalPlanes.text = lista.size.toString()

        val diasTotales = lista.sumOf { item ->
            val inicio = parseFecha(item.fechaInicio)
            val fin = parseFecha(item.fechaFin)
            if (inicio != null && fin != null && !fin.isBefore(inicio)) {
                (ChronoUnit.DAYS.between(inicio, fin) + 1L).coerceAtLeast(1L)
            } else {
                (item.tipoMembresia?.duracionDias ?: 0).toLong()
            }
        }
        val fmtEntero = NumberFormat.getIntegerInstance(Locale.US)
        binding.tvTotalDias.text = fmtEntero.format(diasTotales)

        val totalInvertido = lista.sumOf { item ->
            val estado = item.estadoMembresia?.nombre?.uppercase(Locale.getDefault()).orEmpty()
            if (estado.contains("CANCEL")) 0.0 else (item.tipoMembresia?.precio ?: 0.0)
        }
        val fmtMoneda = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = if (totalInvertido % 1.0 == 0.0) 0 else 2
        }
        binding.tvTotalInvertido.text = "$${fmtMoneda.format(totalInvertido)}"
    }

    private fun parseFecha(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return try {
            LocalDate.parse(iso.trim().take(10))
        } catch (_: Exception) {
            null
        }
    }

    private class HistorialMembresiasAdapter :
        RecyclerView.Adapter<HistorialMembresiasAdapter.ViewHolder>() {

        private var items: List<ClienteMembresiaDto> = emptyList()

        fun submitList(nuevos: List<ClienteMembresiaDto>) {
            items = nuevos
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHistorialMembresiaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(
            private val itemBinding: ItemHistorialMembresiaBinding
        ) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(item: ClienteMembresiaDto) {
                val context = itemBinding.root.context
                itemBinding.tvItemMembresiaPlanNombre.text =
                    item.tipoMembresia?.nombre ?: "Membresía ApexGym"

                val inicio = MembresiaVigenciaHelper.formatearFechaCorta(item.fechaInicio)
                val fin = MembresiaVigenciaHelper.formatearFechaCorta(item.fechaFin)
                itemBinding.tvItemMembresiaFechas.text = "$inicio al $fin"

                val duracion = item.tipoMembresia?.duracionDias
                val renovacion = if (item.renovacionAutomatica) "Automática" else "Manual"
                itemBinding.tvItemMembresiaDuracionRenovacion.text = if (duracion != null) {
                    "$duracion días • Renovación $renovacion"
                } else {
                    "Renovación $renovacion"
                }

                itemBinding.tvItemMembresiaPrecio.text = item.tipoMembresia?.let {
                    MembresiaVigenciaHelper.formatearPrecioMxn(it.precio)
                } ?: "—"

                val estadoStr = (item.estadoMembresia?.nombre ?: "Registrada")
                    .uppercase(Locale.getDefault())
                itemBinding.tvItemMembresiaEstado.text = estadoStr

                when {
                    estadoStr.contains("VIGENTE") || estadoStr.contains("ACTIV") -> {
                        itemBinding.tvItemMembresiaEstado.setBackgroundResource(R.drawable.bg_badge_green)
                        itemBinding.tvItemMembresiaEstado.setTextColor(
                            ContextCompat.getColor(context, R.color.white)
                        )
                    }
                    estadoStr.contains("PEND") -> {
                        itemBinding.tvItemMembresiaEstado.setBackgroundResource(R.drawable.bg_badge_orange)
                        itemBinding.tvItemMembresiaEstado.setTextColor(
                            ContextCompat.getColor(context, R.color.white)
                        )
                    }
                    else -> {
                        itemBinding.tvItemMembresiaEstado.setBackgroundResource(R.drawable.bg_badge_soft_neutral)
                        itemBinding.tvItemMembresiaEstado.setTextColor(
                            ContextCompat.getColor(context, R.color.apex_text_secondary)
                        )
                    }
                }
            }
        }
    }
}

package com.sprm.apexgym.feature.notificaciones

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.NotificacionDto
import com.sprm.apexgym.core.network.formatearFecha
import com.sprm.apexgym.core.network.formatearHora
import com.sprm.apexgym.core.network.repository.NotificacionRepository
import com.sprm.apexgym.core.network.repository.ResultadoNotificaciones
import com.sprm.apexgym.databinding.FragmentNotificacionDetalleBinding
import kotlinx.coroutines.launch

class NotificacionDetalleFragment : BaseFragment<FragmentNotificacionDetalleBinding>() {

    private val repositorio = NotificacionRepository()

    private var notificacionId: Int = 0
    private var notificacion: NotificacionDto? = null

    override fun getLayoutRes(): Int = R.layout.fragment_notificacion_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notificacionId = arguments?.getInt(ARG_ID) ?: 0

        setupClickListeners()
        cargarNotificacion()
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flShare.setOnClickListener { compartirNotificacion() }

        // La API no expone borrado, respuesta ni regreso a PENDIENTE: no se muestran accesos sin función.
        binding.flDelete.visibility = View.GONE
        binding.btnMarcarNoLeida.visibility = View.GONE
        binding.btnResponder.visibility = View.GONE
        binding.llNotifActions.visibility = View.GONE
        binding.cvEntidadRelacionada.visibility = View.GONE

        binding.btnMarcarLeida.setOnClickListener { marcarLeida() }

        // "Archivar" del diseño equivale a IGNORADA en el contrato.
        binding.btnArchivar.text = "Ignorar"
        binding.btnArchivar.setOnClickListener { marcarIgnorada() }
    }

    private fun cargarNotificacion() {
        if (notificacionId <= 0) {
            mostrarError("No se recibió la notificación solicitada")
            return
        }

        binding.tvNotifTitulo.text = "Cargando notificación…"
        binding.tvNotifMensaje.text = "Consultando el detalle."
        binding.btnMarcarLeida.isEnabled = false
        binding.btnArchivar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            when (val resultado = repositorio.listar()) {
                is ResultadoNotificaciones.Exito -> {
                    if (!isAdded) return@launch
                    val encontrada = resultado.datos.firstOrNull { it.id == notificacionId }
                    if (encontrada == null) {
                        mostrarError("La notificación ya no está disponible")
                    } else {
                        bind(encontrada)
                    }
                }

                is ResultadoNotificaciones.Error -> {
                    if (!isAdded) return@launch
                    mostrarError(resultado.mensaje)
                }
            }
        }
    }

    private fun bind(datos: NotificacionDto) {
        notificacion = datos

        binding.tvNotifTitulo.text = datos.titulo.ifBlank { "Notificación" }
        binding.tvNotifMensaje.text = datos.mensaje
        binding.tvNotifTipo.text = formatearTipoNotificacion(datos.tipo)
        binding.tvNotifHora.text = formatearHora(datos.createdAt)
        binding.tvNotifRemitente.text = if (datos.remitenteId != null) {
            "Equipo ApexGym"
        } else {
            "Sistema ApexGym"
        }
        binding.tvNotifFechaCompleta.text = formatearFecha(
            datos.createdAt,
            "dd 'de' MMMM, yyyy • HH:mm"
        )

        val (estadoTexto, estadoFondo) = when (datos.estado) {
            NotificacionDto.ESTADO_LEIDA -> "Leída" to R.drawable.bg_badge_green
            NotificacionDto.ESTADO_IGNORADA -> "Ignorada" to R.drawable.bg_badge_blue
            else -> "Pendiente" to R.drawable.bg_badge_orange
        }
        binding.tvNotifPrioridad.visibility = View.VISIBLE
        binding.tvNotifPrioridad.text = estadoTexto
        binding.tvNotifPrioridad.setBackgroundResource(estadoFondo)
        binding.tvNotifPrioridad.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )

        binding.btnMarcarLeida.visibility = if (datos.estaLeida) View.GONE else View.VISIBLE
        binding.btnMarcarLeida.isEnabled = !datos.estaLeida
        binding.btnMarcarLeida.text = "Marcar como leída"

        binding.btnArchivar.isEnabled = !datos.estaIgnorada
        binding.btnArchivar.text = if (datos.estaIgnorada) "Ignorada" else "Ignorar"
    }

    private fun formatearTipoNotificacion(tipo: String): String {
        val norm = tipo.trim().uppercase()
        return when (norm) {
            "ACCESO_QR" -> "Acceso QR"
            "ACTIVIDAD_EMPLEADO" -> "Actividad"
            "MEMBRESIA" -> "Membresía"
            "PAGO", "VENTA" -> "Pagos"
            "GENERAL", "" -> "General"
            else -> norm.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun marcarLeida() {
        val actual = notificacion ?: return
        if (actual.estaLeida) return
        ejecutarAccion(
            exito = "Notificación marcada como leída",
            accion = { repositorio.marcarLeida(actual.id) }
        )
    }

    private fun marcarIgnorada() {
        val actual = notificacion ?: return
        if (actual.estaIgnorada) return
        ejecutarAccion(
            exito = "Notificación ignorada",
            accion = { repositorio.marcarIgnorada(actual.id) }
        )
    }

    private fun ejecutarAccion(
        exito: String,
        accion: suspend () -> ResultadoNotificaciones<NotificacionDto>
    ) {
        binding.btnMarcarLeida.isEnabled = false
        binding.btnArchivar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            when (val resultado = accion()) {
                is ResultadoNotificaciones.Exito -> {
                    if (!isAdded) return@launch
                    bind(resultado.datos)
                    Toast.makeText(requireContext(), exito, Toast.LENGTH_SHORT).show()
                }

                is ResultadoNotificaciones.Error -> {
                    if (!isAdded) return@launch
                    notificacion?.let { bind(it) }
                    Toast.makeText(requireContext(), resultado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.tvNotifTitulo.text = "No se pudo cargar la notificación"
        binding.tvNotifMensaje.text = mensaje
        binding.tvNotifTipo.text = "—"
        binding.tvNotifHora.text = "—"
        binding.tvNotifRemitente.text = "—"
        binding.tvNotifFechaCompleta.text = "—"
        binding.tvNotifPrioridad.visibility = View.GONE
        binding.btnMarcarLeida.isEnabled = false
        binding.btnArchivar.isEnabled = false
    }

    private fun compartirNotificacion() {
        val actual = notificacion ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, actual.titulo)
            putExtra(Intent.EXTRA_TEXT, "${actual.titulo}\n\n${actual.mensaje}")
        }
        startActivity(Intent.createChooser(intent, "Compartir notificación"))
    }

    companion object {
        private const val ARG_ID = "notificacion_id"

        fun newInstance(id: Int): NotificacionDetalleFragment =
            NotificacionDetalleFragment().apply {
                arguments = Bundle().apply { putInt(ARG_ID, id) }
            }
    }
}

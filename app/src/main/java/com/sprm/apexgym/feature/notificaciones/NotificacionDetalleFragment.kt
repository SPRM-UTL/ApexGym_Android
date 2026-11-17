package com.sprm.apexgym.feature.notificaciones

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentNotificacionDetalleBinding

class NotificacionDetalleFragment : BaseFragment<FragmentNotificacionDetalleBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_notificacion_detalle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        binding.tvNotifTitulo.text = "¡Renovación de Membresía!"
        binding.tvNotifMensaje.text = "Tu membresía vence en 15 días. Renuévala desde la aplicación o en la recepción de tu sucursal para continuar disfrutando de acceso ilimitado a todas nuestras áreas."
        binding.tvNotifHora.text = "Hoy, 10:30 AM"
        binding.tvNotifRemitente.text = "ApexGym Notificaciones"
        binding.tvNotifFechaCompleta.text = "11 de Noviembre, 2026"
        binding.tvEntidadTitulo.text = "Plan Premium Anual"
        binding.tvEntidadDescripcion.text = "Membresía activa con acceso a pesas, cardio y área VIP."
    }

    private fun setupClickListeners() {
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flShare.setOnClickListener {
            Toast.makeText(requireContext(), "Enlace de notificación copiado", Toast.LENGTH_SHORT).show()
        }
        binding.flDelete.setOnClickListener {
            Toast.makeText(requireContext(), "Notificación eliminada", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        binding.btnNotifActionPrimary.setOnClickListener {
            Toast.makeText(requireContext(), "Redirigiendo a tu membresía...", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        binding.btnNotifActionSecondary.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnMarcarLeida.setOnClickListener {
            Toast.makeText(requireContext(), "Notificación marcada como leída", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        binding.btnMarcarNoLeida.setOnClickListener {
            Toast.makeText(requireContext(), "Marcada como no leída", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        binding.btnResponder.setOnClickListener {
            Toast.makeText(requireContext(), "Respuesta enviada al equipo de soporte", Toast.LENGTH_SHORT).show()
        }
        binding.btnArchivar.setOnClickListener {
            Toast.makeText(requireContext(), "Notificación archivada con éxito", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}

package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.databinding.FragmentRecepcionDashboardBinding
import java.text.SimpleDateFormat
import java.util.*

class RecepcionDashboardFragment : BaseFragment<FragmentRecepcionDashboardBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_recepcion_dashboard

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mostrar fecha actual
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("es", "MX"))
        binding.tvTodayDate.text = sdf.format(Date())

        // Datos de ejemplo
        binding.tvEntradasHoy.text = "147"
        binding.tvEnGimnasio.text = "89"
        binding.tvAlertas.text = "3"

        // Navegación básica
        binding.flNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, com.sprm.apexgym.feature.notificaciones.NotificacionesListFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.cvScanQR.setOnClickListener { navegarAQr() }
        binding.ivScanQR.setOnClickListener { navegarAQr() }
        binding.cvManualAccess.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, AccesoManualFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.tvVerTodosAccesos.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, HistorialAsistenciasFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.cvAlerts.setOnClickListener {
            Toast.makeText(requireContext(), "3 alertas de acceso pendientes", Toast.LENGTH_SHORT).show()
        }
        binding.etSearchClient.setOnEditorActionListener { _, _, _ ->
            val sheet = ClienteBusquedaSheetFragment()
            sheet.show(parentFragmentManager, ClienteBusquedaSheetFragment.TAG)
            true
        }
    }

    private fun navegarAQr() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, QrScannerFragment())
            .addToBackStack(null)
            .commit()
    }
}

package com.sprm.apexgym.feature.tienda

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sprm.apexgym.R

class PagoTarjetaFragment : Fragment(R.layout.fragment_pago_tarjeta) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvMontoTarjeta)?.text = "$1,334.00 MXN"

        view.findViewById<View>(R.id.flBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnCancelarTarjeta)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val confirmarPago = View.OnClickListener {
            Toast.makeText(requireContext(), "Pago con tarjeta aprobado exitosamente", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, TicketCompraFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnFinalizarTarjeta)?.setOnClickListener(confirmarPago)
        view.findViewById<View>(R.id.btnReintentarTarjeta)?.setOnClickListener(confirmarPago)
        view.findViewById<View>(R.id.tvManualEntryLink)?.setOnClickListener {
            Toast.makeText(requireContext(), "Modo de captura manual activado", Toast.LENGTH_SHORT).show()
        }
    }
}

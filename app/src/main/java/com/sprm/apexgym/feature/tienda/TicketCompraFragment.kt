package com.sprm.apexgym.feature.tienda

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sprm.apexgym.R

class TicketCompraFragment : Fragment(R.layout.fragment_ticket_compra) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvTicketNumber)?.text = "#APEX-89421"
        view.findViewById<TextView>(R.id.tvTicketDate)?.text = "11 Nov 2026, 13:30"
        view.findViewById<TextView>(R.id.tvTicketClient)?.text = "Juan Pérez"
        view.findViewById<TextView>(R.id.tvTicketMembership)?.text = "Socio #12345 • Premium"
        view.findViewById<TextView>(R.id.tvTicketTotal)?.text = "$1,334.00 MXN"

        view.findViewById<View>(R.id.flClose)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.flShare)?.setOnClickListener {
            Toast.makeText(requireContext(), "Comprobante digital compartido", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnDownloadTicket)?.setOnClickListener {
            Toast.makeText(requireContext(), "Ticket guardado en PDF en tu dispositivo", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnPrintTicket)?.setOnClickListener {
            Toast.makeText(requireContext(), "Enviando ticket a impresora térmica...", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.sprm.apexgym.feature.tienda

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sprm.apexgym.R

class PagoMixtoFragment : Fragment(R.layout.fragment_pago_mixto) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvTotalMixto)?.text = "$1,334.00 MXN"
        view.findViewById<TextView>(R.id.tvTotalAsignado)?.text = "$1,334.00 MXN"
        view.findViewById<TextView>(R.id.tvFaltante)?.text = "$0.00 MXN"

        view.findViewById<View>(R.id.flBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnAddPaymentMethod)?.setOnClickListener {
            Toast.makeText(requireContext(), "Método de pago adicional agregado", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnContinuarMixto)?.setOnClickListener {
            Toast.makeText(requireContext(), "Pago mixto validado y aprobado", Toast.LENGTH_SHORT).show()
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
    }
}

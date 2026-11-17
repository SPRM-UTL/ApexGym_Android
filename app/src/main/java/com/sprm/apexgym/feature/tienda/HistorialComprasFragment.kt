package com.sprm.apexgym.feature.tienda

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sprm.apexgym.R

class HistorialComprasFragment : Fragment(R.layout.fragment_historial_compras) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvTotalGastado)?.text = "$3,850.00 MXN"
        view.findViewById<TextView>(R.id.tvTotalCompras)?.text = "6 pedidos"
        view.findViewById<TextView>(R.id.tvEsteMesGasto)?.text = "$1,334.00 MXN"

        view.findViewById<View>(R.id.flBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.flFilter)?.setOnClickListener {
            Toast.makeText(requireContext(), "Filtros de historial de compras", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.rvHistorialCompras)?.setOnClickListener {
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

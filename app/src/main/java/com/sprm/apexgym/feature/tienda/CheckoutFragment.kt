package com.sprm.apexgym.feature.tienda

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sprm.apexgym.R

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvTotalPagar)?.text = "$1,334.00 MXN"

        view.findViewById<View>(R.id.flBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnContinuar)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, PagoTarjetaFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.rvPaymentMethods)?.setOnClickListener {
            Toast.makeText(requireContext(), "Método de pago seleccionado", Toast.LENGTH_SHORT).show()
        }
    }
}

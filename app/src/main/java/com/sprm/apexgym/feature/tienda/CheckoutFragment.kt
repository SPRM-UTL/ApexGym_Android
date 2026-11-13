package com.sprm.apexgym.feature.tienda

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sprm.apexgym.R

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvTotalPagar)?.text = "$1,334.00 MXN"

        view.findViewById<View>(R.id.flBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnPagoTarjeta)?.setOnClickListener {
            navegar(PagoTarjetaFragment())
        }

        view.findViewById<View>(R.id.btnPagoEfectivo)?.setOnClickListener {
            PagoEfectivoDialogFragment().show(
                parentFragmentManager,
                PagoEfectivoDialogFragment.TAG
            )
        }

        view.findViewById<View>(R.id.btnPagoMixto)?.setOnClickListener {
            navegar(PagoMixtoFragment())
        }

        view.findViewById<View>(R.id.btnContinuar)?.setOnClickListener {
            navegar(PagoTarjetaFragment())
        }
    }

    private fun navegar(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

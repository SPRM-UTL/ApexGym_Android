package com.sprm.apexgym.feature.tienda

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.sprm.apexgym.R

class PagoEfectivoDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.dialog_pago_efectivo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvTotalPagarEfectivo)?.text = "$1,334.00 MXN"
        view.findViewById<TextView>(R.id.tvVuelto)?.text = "$166.00 MXN"

        view.findViewById<View>(R.id.btnCancelarEfectivo)?.setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btnConfirmarEfectivo)?.setOnClickListener {
            val hostFm = requireActivity().supportFragmentManager
            dismissAllowingStateLoss()
            hostFm.beginTransaction()
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

    companion object {
        const val TAG = "PagoEfectivoDialog"
    }
}

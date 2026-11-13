package com.sprm.apexgym.feature.tienda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.R

class CarritoBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_carrito_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvCartItemCount)?.text = "2 productos"
        view.findViewById<TextView>(R.id.tvTotal)?.text = "$1,334.00 MXN"

        view.findViewById<View>(R.id.btnCloseCart)?.setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btnProcederPago)?.setOnClickListener {
            val hostFm = requireActivity().supportFragmentManager
            dismissAllowingStateLoss()
            hostFm.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, CheckoutFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        const val TAG = "CarritoBottomSheet"
    }
}

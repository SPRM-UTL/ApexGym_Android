package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.auth.LoginFragment
import kotlinx.coroutines.launch

class ProfileBottomSheetFragment : BottomSheetDialogFragment() {

    private var cerrandoSesion = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_sheet, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCerrarProfileSheet = view.findViewById<View>(R.id.btnCerrarProfileSheet)
        val btnOpcionMiPerfil = view.findViewById<View>(R.id.btnOpcionMiPerfil)
        val btnOpcionCerrarSesion = view.findViewById<View>(R.id.btnOpcionCerrarSesion)
        val tvCerrarSesionTitulo = view.findViewById<TextView>(R.id.tvCerrarSesionTitulo)

        btnCerrarProfileSheet?.setOnClickListener {
            if (!cerrandoSesion) dismiss()
        }

        btnOpcionMiPerfil?.setOnClickListener {
            if (cerrandoSesion) return@setOnClickListener
            Toast.makeText(
                requireContext(),
                "Proximamente",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }

        btnOpcionCerrarSesion?.setOnClickListener {
            if (cerrandoSesion) return@setOnClickListener
            cerrandoSesion = true
            btnOpcionCerrarSesion.isEnabled = false
            tvCerrarSesionTitulo?.text = "Cerrando sesión..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    RetrofitClient.authService.logout()
                } catch (_: Exception) {
                } finally {
                    RetrofitClient.cookieJar.clear()

                    if (isAdded) {
                        val hostFragmentManager = requireActivity().supportFragmentManager
                        Toast.makeText(
                            requireContext(),
                            "Sesión cerrada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        dismissAllowingStateLoss()

                        hostFragmentManager.popBackStack(
                            null,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        hostFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                            .replace(R.id.fragment_container, LoginFragment())
                            .commit()
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "ProfileBottomSheet"
    }
}

package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import kotlinx.coroutines.launch

class ProfileBottomSheetFragment : BottomSheetDialogFragment() {

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
        val btnOpcionAjustes = view.findViewById<View>(R.id.btnOpcionCerrarSesion)
        val tvProfileSheetName = view.findViewById<TextView>(R.id.tvProfileSheetName)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.authService.obtenerMiPerfil()
                if (!isAdded) return@launch
                if (response.isSuccessful && response.body()?.responseFlag == 0) {
                    val data = response.body()?.data
                    val soloNombre = data?.cliente?.nombre?.trim()?.takeIf { it.isNotBlank() }
                        ?: data?.usuario?.nombre?.trim().orEmpty()

                    if (soloNombre.isNotBlank()) {
                        tvProfileSheetName?.text = soloNombre
                    }
                }
            } catch (_: Exception) {
            }
        }

        btnCerrarProfileSheet?.setOnClickListener {
            dismiss()
        }

        btnOpcionMiPerfil?.setOnClickListener {
            val fm = parentFragmentManager
            dismissAllowingStateLoss()
            val existing = fm.findFragmentByTag(EditProfileBottomSheetFragment.TAG)
            if (existing == null) {
                EditProfileBottomSheetFragment().show(
                    fm,
                    EditProfileBottomSheetFragment.TAG
                )
            }
        }

        btnOpcionAjustes?.setOnClickListener {
            val hostFragmentManager = requireActivity().supportFragmentManager
            dismissAllowingStateLoss()
            hostFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack("settings")
                .commit()
        }
    }

    companion object {
        const val TAG = "ProfileBottomSheet"
    }
}

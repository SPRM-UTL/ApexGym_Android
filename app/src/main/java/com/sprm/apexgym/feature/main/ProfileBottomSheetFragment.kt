package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.feature.auth.LoginFragment
import com.sprm.apexgym.feature.caja.ResumenAperturaFragment
import com.sprm.apexgym.feature.empleados.EmpleadosListFragment
import com.sprm.apexgym.feature.notificaciones.ConfiguracionNotificacionesFragment
import com.sprm.apexgym.feature.notificaciones.NotificacionesListFragment
import com.sprm.apexgym.feature.recepcion.RecepcionDashboardFragment
import com.sprm.apexgym.feature.tienda.HistorialComprasFragment
import com.sprm.apexgym.feature.visitas.VisitasListFragment
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
        val btnOpcionNotificaciones = view.findViewById<View>(R.id.btnOpcionNotificaciones)
        val btnOpcionConfigNotificaciones = view.findViewById<View>(R.id.btnOpcionConfigNotificaciones)
        val btnOpcionHistorialCompras = view.findViewById<View>(R.id.btnOpcionHistorialCompras)
        val btnOpcionEmpleados = view.findViewById<View>(R.id.btnOpcionEmpleados)
        val btnOpcionVisitas = view.findViewById<View>(R.id.btnOpcionVisitas)
        val btnOpcionRecepcion = view.findViewById<View>(R.id.btnOpcionRecepcion)
        val btnOpcionCaja = view.findViewById<View>(R.id.btnOpcionCaja)
        val btnOpcionCerrarSesion = view.findViewById<View>(R.id.btnOpcionCerrarSesion)
        val tvCerrarSesionTitulo = view.findViewById<TextView>(R.id.tvCerrarSesionTitulo)
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

        fun navegarAFragment(fragmento: Fragment) {
            if (cerrandoSesion || !isAdded) return

            val hostFm = requireActivity().supportFragmentManager

            dismissAllowingStateLoss()

            hostFm.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragmento)
                .addToBackStack(null)
                .commit()
        }

        btnOpcionNotificaciones?.setOnClickListener {
            navegarAFragment(NotificacionesListFragment())
        }

        btnOpcionConfigNotificaciones?.setOnClickListener {
            navegarAFragment(ConfiguracionNotificacionesFragment())
        }

        btnOpcionHistorialCompras?.setOnClickListener {
            navegarAFragment(HistorialComprasFragment())
        }

        btnOpcionEmpleados?.setOnClickListener {
            navegarAFragment(EmpleadosListFragment())
        }

        btnOpcionVisitas?.setOnClickListener {
            navegarAFragment(VisitasListFragment())
        }

        btnOpcionRecepcion?.setOnClickListener {
            navegarAFragment(RecepcionDashboardFragment())
        }

        btnOpcionCaja?.setOnClickListener {
            navegarAFragment(ResumenAperturaFragment())
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

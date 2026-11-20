package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.repository.NotificacionRepository
import com.sprm.apexgym.core.network.repository.ResultadoNotificaciones
import com.sprm.apexgym.feature.notificaciones.NotificacionesListFragment
import kotlinx.coroutines.launch

/**
 * Pantalla de Notificaciones en pantalla completa que se desliza de abajo hacia arriba.
 */
class NotificationsBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications_sheet, container, false)
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

                // Forzar altura de pantalla completa en el contenedor del BottomSheet
                val layoutParams = sheet.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                sheet.layoutParams = layoutParams

                val behavior = BottomSheetBehavior.from(sheet)
                behavior.peekHeight = resources.displayMetrics.heightPixels
                behavior.isFitToContents = false
                behavior.expandedOffset = 0
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asegurar que la vista raíz ocupe toda la altura de la pantalla
        view.layoutParams = view.layoutParams?.apply {
            height = resources.displayMetrics.heightPixels
        }

        view.findViewById<View>(R.id.btnCerrarNotificaciones)?.setOnClickListener {
            dismiss()
        }

        fun navegarAFragment(fragmento: Fragment) {
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

        view.findViewById<View>(R.id.btnVerTodasNotificaciones)?.setOnClickListener {
            navegarAFragment(NotificacionesListFragment())
        }

        // La API no expone preferencias de notificaciones: no se muestra un acceso sin función.
        view.findViewById<View>(R.id.btnConfigurarNotificacionesSheet)?.visibility = View.GONE

        cargarResumenNotificaciones(view)
    }

    private fun cargarResumenNotificaciones(rootView: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = NotificacionRepository().listar()
            if (!isAdded || resultado !is ResultadoNotificaciones.Exito) return@launch

            val notificaciones = resultado.datos
            val pendientes = notificaciones.count { it.estaPendiente }
            val titulo = rootView.findViewById<android.widget.TextView>(R.id.tvNotificationsEmptyTitle)
            val subtitulo =
                rootView.findViewById<android.widget.TextView>(R.id.tvNotificationsEmptySubtitle)

            when {
                pendientes > 0 -> {
                    titulo?.text = if (pendientes == 1) {
                        "Tienes 1 notificación sin leer"
                    } else {
                        "Tienes $pendientes notificaciones sin leer"
                    }
                    subtitulo?.text = "Abre el centro de notificaciones para verlas y marcar su estado."
                }

                notificaciones.isNotEmpty() -> {
                    titulo?.text = "No tienes notificaciones sin leer"
                    subtitulo?.text = "Tienes ${notificaciones.size} notificaciones en tu historial."
                }
            }
        }
    }

    companion object {
        const val TAG = "NotificationsBottomSheet"
    }
}

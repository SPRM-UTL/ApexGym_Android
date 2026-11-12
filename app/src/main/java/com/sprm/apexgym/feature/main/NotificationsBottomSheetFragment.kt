package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sprm.apexgym.R

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
    }

    companion object {
        const val TAG = "NotificationsBottomSheet"
    }
}

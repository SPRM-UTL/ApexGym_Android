package com.sprm.apexgym.feature.main

import android.app.Dialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.client.ServicioClienteDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

class ServiceDetailBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_service_detail_sheet, container, false)
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

        val args = arguments ?: Bundle()
        val nombre = args.getString(ARG_NOMBRE, "Servicio ApexGym")
        val descripcion = args.getString(ARG_DESCRIPCION, "")
        val precio = args.getDouble(ARG_PRECIO, 0.0)
        val imagenUrl = args.getString(ARG_IMAGEN_URL, null)

        val btnCerrarServiceDetail = view.findViewById<View>(R.id.btnCerrarServiceDetail)
        val flDetailServImageContainer = view.findViewById<FrameLayout>(R.id.flDetailServImageContainer)
        val tvDetailServNombre = view.findViewById<TextView>(R.id.tvDetailServNombre)
        val tvDetailServPrecioUnitario = view.findViewById<TextView>(R.id.tvDetailServPrecioUnitario)
        val tvDetailServDescripcion = view.findViewById<TextView>(R.id.tvDetailServDescripcion)
        val btnConfirmarServicio = view.findViewById<MaterialButton>(R.id.btnConfirmarServicio)

        tvDetailServNombre?.text = nombre
        tvDetailServPrecioUnitario?.text = "${formatearPrecio(precio)} MXN"
        tvDetailServDescripcion?.text = if (!descripcion.isNullOrBlank()) {
            descripcion
        } else {
            "Servicio especializado disponible para socios en sucursal ApexGym. Acércate a recepción para agendar tu horario."
        }

        flDetailServImageContainer?.clipToOutline = true
        if (!imagenUrl.isNullOrBlank() && flDetailServImageContainer != null) {
            cargarImagenServicio(imagenUrl, flDetailServImageContainer)
        }

        btnConfirmarServicio?.text = "Adquirir ${formatearPrecio(precio)} MXN"

        btnCerrarServiceDetail?.setOnClickListener {
            dismiss()
        }

        btnConfirmarServicio?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Solicitud lista: $nombre (${formatearPrecio(precio)} MXN). Agenda en recepción.",
                Toast.LENGTH_LONG
            ).show()
            dismiss()
        }
    }

    private fun cargarImagenServicio(rutaImagen: String, contenedor: FrameLayout) {
        val urlCompleta = RetrofitClient.resolverUrlImagen(rutaImagen) ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = try {
                if (urlCompleta.startsWith("data:image")) {
                    val base64Part = urlCompleta.substringAfter("base64,", "")
                    if (base64Part.isNotEmpty()) {
                        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                } else {
                    URL(urlCompleta).openStream().use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }
            } catch (_: Exception) {
                null
            }

            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        contenedor.removeAllViews()
                        val imageView = ImageView(requireContext()).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageBitmap(bitmap)
                        }
                        contenedor.addView(imageView)
                    }
                }
            }
        }
    }

    private fun formatearPrecio(monto: Double): String {
        val formato = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (monto % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }
        return "$${formato.format(monto)}"
    }

    companion object {
        const val TAG = "ServiceDetailBottomSheet"
        private const val ARG_ID = "arg_id"
        private const val ARG_NOMBRE = "arg_nombre"
        private const val ARG_DESCRIPCION = "arg_descripcion"
        private const val ARG_PRECIO = "arg_precio"
        private const val ARG_IMAGEN_URL = "arg_imagen_url"

        fun newInstance(servicio: ServicioClienteDto): ServiceDetailBottomSheetFragment {
            return ServiceDetailBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, servicio.id)
                    putString(ARG_NOMBRE, servicio.nombre)
                    putString(ARG_DESCRIPCION, servicio.descripcion)
                    putDouble(ARG_PRECIO, servicio.precio)
                    putString(ARG_IMAGEN_URL, servicio.imagenUrl)
                }
            }
        }
    }
}

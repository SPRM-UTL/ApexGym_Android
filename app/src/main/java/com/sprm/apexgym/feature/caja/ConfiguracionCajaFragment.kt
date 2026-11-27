package com.sprm.apexgym.feature.caja

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.ActualizarCajaRequest
import com.sprm.apexgym.core.network.api.CajaDto
import com.sprm.apexgym.core.network.api.CrearCajaRequest
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.core.ui.InputFilters
import com.sprm.apexgym.databinding.FragmentConfiguracionCajaBinding
import kotlinx.coroutines.launch

class ConfiguracionCajaFragment : BaseFragment<FragmentConfiguracionCajaBinding>() {

    private var cajas: List<CajaDto> = emptyList()

    override fun getLayoutRes(): Int = R.layout.fragment_configuracion_caja

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvCajas.layoutManager = LinearLayoutManager(requireContext())
        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnAgregarCaja.setOnClickListener { mostrarDialogoCaja(cajaExistente = null) }
        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarCajas() }

        cargarCajas()
    }

    private fun cargarCajas() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val res = RetrofitClient.empleadoService.obtenerCajas()
                if (!isAdded || view == null) return@launch
                cajas = res.dataExitosa().orEmpty()
                renderizarCajas()
            } catch (_: Exception) {
                if (!isAdded || view == null) return@launch
                cajas = emptyList()
                renderizarCajas()
                Toast.makeText(
                    requireContext(),
                    "No se pudo conectar con el servidor",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun renderizarCajas() {
        binding.rvCajas.adapter = object : RecyclerView.Adapter<CajaHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CajaHolder {
                return crearFila(parent.context)
            }

            override fun onBindViewHolder(holder: CajaHolder, position: Int) {
                if (cajas.isEmpty()) {
                    holder.tvTitulo.text = "No hay cajas registradas"
                    holder.tvDetalle.text = "Usa el botón para crear la primera caja"
                    holder.tvEstado.text = ""
                    holder.tvEstado.visibility = View.GONE
                    holder.itemView.setOnClickListener(null)
                } else {
                    val caja = cajas[position]
                    val activa = CajaValidacionLogic.esEstadoCajaActivo(caja.estado)
                    holder.tvTitulo.text = caja.nombre ?: "Caja #${caja.id}"
                    holder.tvDetalle.text = caja.ubicacion
                        ?.takeIf { it.isNotBlank() }
                        ?: "Sin ubicación registrada"
                    holder.tvEstado.text = if (activa) "ACTIVO" else (caja.estado?.uppercase() ?: "INACTIVO")
                    holder.tvEstado.visibility = View.VISIBLE
                    holder.tvEstado.setBackgroundResource(
                        if (activa) R.drawable.bg_badge_orange else R.drawable.bg_chip_unselected
                    )
                    holder.tvEstado.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (activa) R.color.white else R.color.apex_text_secondary
                        )
                    )
                    holder.itemView.setOnClickListener { mostrarDialogoCaja(cajaExistente = caja) }
                }
            }

            override fun getItemCount(): Int = cajas.size.coerceAtLeast(1)
        }
    }

    private fun mostrarDialogoCaja(cajaExistente: CajaDto?) {
        val context = requireContext()
        val themedCtx = androidx.appcompat.view.ContextThemeWrapper(
            context,
            R.style.Widget_Apex_TextInputLayout
        )
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(4))
        }

        val tilNombre = com.google.android.material.textfield.TextInputLayout(themedCtx).apply {
            hint = "Nombre de la caja"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dpToPx(14).toFloat(), dpToPx(14).toFloat(), dpToPx(14).toFloat(), dpToPx(14).toFloat())
            boxBackgroundColor = ContextCompat.getColor(context, R.color.apex_bg_screen)
        }
        val etNombre = com.google.android.material.textfield.TextInputEditText(tilNombre.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            maxLines = 1
            filters = InputFilters.plainText(80)
            if (cajaExistente != null) setText(cajaExistente.nombre.orEmpty())
        }
        tilNombre.addView(etNombre)
        container.addView(tilNombre)

        val tilUbicacion = com.google.android.material.textfield.TextInputLayout(themedCtx).apply {
            hint = "Ubicación (opcional)"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dpToPx(14).toFloat(), dpToPx(14).toFloat(), dpToPx(14).toFloat(), dpToPx(14).toFloat())
            boxBackgroundColor = ContextCompat.getColor(context, R.color.apex_bg_screen)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(12) }
        }
        val etUbicacion = com.google.android.material.textfield.TextInputEditText(tilUbicacion.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            maxLines = 1
            filters = InputFilters.plainText(150)
            if (cajaExistente != null) setText(cajaExistente.ubicacion.orEmpty())
        }
        tilUbicacion.addView(etUbicacion)
        container.addView(tilUbicacion)

        val swActiva = if (cajaExistente != null) {
            SwitchMaterial(context).apply {
                text = "Caja activa"
                isChecked = CajaValidacionLogic.esEstadoCajaActivo(cajaExistente.estado)
                setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dpToPx(12) }
            }.also { container.addView(it) }
        } else {
            null
        }

        val dialogo = MaterialAlertDialogBuilder(context)
            .setTitle(if (cajaExistente == null) "Nueva Caja" else "Editar Caja")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(if (cajaExistente == null) "Crear" else "Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val nombre = etNombre.text?.toString()?.trim().orEmpty()
                    if (nombre.isEmpty()) {
                        tilNombre.error = "Ingresa el nombre de la caja"
                        return@setOnClickListener
                    }
                    val ubicacion = etUbicacion.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
                    dialogo.dismiss()
                    if (cajaExistente == null) {
                        crearCaja(nombre, ubicacion)
                    } else {
                        val nuevoEstado = if (swActiva?.isChecked != false) "ACTIVO" else "INACTIVO"
                        actualizarCaja(cajaExistente.id, nombre, ubicacion, nuevoEstado)
                    }
                }
        }
        dialogo.show()
    }

    private fun crearCaja(nombre: String, ubicacion: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.crearCaja(
                    CrearCajaRequest(nombre = nombre, ubicacion = ubicacion, estado = "ACTIVO")
                )
                if (!isAdded || view == null) return@launch
                if (response.dataExitosa() == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo crear la caja",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Caja creada correctamente", Toast.LENGTH_SHORT).show()
                    cargarCajas()
                }
            } catch (_: Exception) {
                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun actualizarCaja(id: Int, nombre: String, ubicacion: String?, estado: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.actualizarCaja(
                    id,
                    ActualizarCajaRequest(nombre = nombre, ubicacion = ubicacion, estado = estado)
                )
                if (!isAdded || view == null) return@launch
                if (response.dataExitosa() == null) {
                    Toast.makeText(
                        requireContext(),
                        response.mensajeDeError() ?: "No se pudo actualizar la caja",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Caja actualizada", Toast.LENGTH_SHORT).show()
                    cargarCajas()
                }
            } catch (_: Exception) {
                if (isAdded && view != null) {
                    Toast.makeText(requireContext(), "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private class CajaHolder(
        itemView: View,
        val tvTitulo: TextView,
        val tvDetalle: TextView,
        val tvEstado: TextView
    ) : RecyclerView.ViewHolder(itemView)

    private fun crearFila(context: android.content.Context): CajaHolder {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(10) }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            radius = dpToPx(16).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(1)
            strokeColor = ContextCompat.getColor(context, R.color.apex_stroke_light)
            isClickable = true
            isFocusable = true
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
        }

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitulo = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        col.addView(tvTitulo)
        val tvDetalle = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        col.addView(tvDetalle)
        row.addView(col)

        val tvEstado = TextView(context).apply {
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        row.addView(tvEstado)
        card.addView(row)
        return CajaHolder(card, tvTitulo, tvDetalle, tvEstado)
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

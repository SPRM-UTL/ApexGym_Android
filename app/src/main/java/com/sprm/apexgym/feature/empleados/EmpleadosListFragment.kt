package com.sprm.apexgym.feature.empleados

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.base.BaseFragment
import com.sprm.apexgym.core.network.api.EmpleadoDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentEmpleadosListBinding
import kotlinx.coroutines.launch

class EmpleadosListFragment : BaseFragment<FragmentEmpleadosListBinding>() {

    private var empleados: List<EmpleadoDto> = emptyList()
    private var filtrados: List<EmpleadoDto> = emptyList()
    private var textoBusqueda: String = ""
    private var filtroEstado: String = FILTRO_TODOS
    private val chipsEstado = mutableListOf<Pair<String, TextView>>()

    override fun getLayoutRes(): Int = R.layout.fragment_empleados_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flClose.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flAddEmpleado.setOnClickListener { navegar(CrearEditarEmpleadoFragment()) }
        binding.swipeRefresh.setColorSchemeResources(R.color.apex_orange)
        binding.swipeRefresh.setOnRefreshListener { cargarEmpleados() }

        binding.etSearchEmpleados.doAfterTextChanged { editable ->
            textoBusqueda = editable?.toString()?.trim().orEmpty()
            aplicarFiltros()
        }

        binding.rvEmpleados.layoutManager = LinearLayoutManager(requireContext())
        construirChipsEstado()
        cargarEmpleados()
    }

    private fun cargarEmpleados() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.obtenerEmpleados()
                if (!isAdded || view == null) return@launch
                val data = response.dataExitosa()

                if (data == null) {
                    empleados = emptyList()
                    construirChipsEstado()
                    binding.rvEmpleados.visibility = View.GONE
                    binding.llEmptyEmpleados.visibility = View.VISIBLE
                    mostrarMensajeVacio(
                        response.mensajeDeError() ?: "No se pudieron cargar los empleados"
                    )
                    return@launch
                }

                empleados = data
                construirChipsEstado()
                aplicarFiltros()
            } catch (_: Exception) {
                if (!isAdded || view == null) return@launch
                empleados = emptyList()
                construirChipsEstado()
                binding.rvEmpleados.visibility = View.GONE
                binding.llEmptyEmpleados.visibility = View.VISIBLE
                mostrarMensajeVacio("No se pudo conectar con el servidor")
            } finally {
                if (isAdded && view != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun categoriaEstado(empleado: EmpleadoDto): String {
        val estado = empleado.estadoEmpleado?.nombre.orEmpty().uppercase()
        return when {
            estado.contains("VACACION") || estado.contains("PERMISO") || estado.contains("INCAPAC") ->
                FILTRO_VACACIONES
            estado.contains("INACTIV") || estado.contains("BAJA") || estado.contains("SUSPEND") ->
                FILTRO_INACTIVOS
            else -> FILTRO_ACTIVOS
        }
    }

    private fun construirChipsEstado() {
        val context = requireContext()
        val container = binding.llFilterChipsEmpleados
        container.removeAllViews()
        chipsEstado.clear()

        val total = empleados.size
        val activos = empleados.count { categoriaEstado(it) == FILTRO_ACTIVOS }
        val vacaciones = empleados.count { categoriaEstado(it) == FILTRO_VACACIONES }
        val inactivos = empleados.count { categoriaEstado(it) == FILTRO_INACTIVOS }

        val opciones = listOf(
            FILTRO_TODOS to "Todos ($total)",
            FILTRO_ACTIVOS to "Activos ($activos)",
            FILTRO_VACACIONES to "Vacaciones ($vacaciones)",
            FILTRO_INACTIVOS to "Inactivos ($inactivos)"
        )

        opciones.forEachIndexed { indice, (clave, etiqueta) ->
            val chip = TextView(context).apply {
                text = etiqueta
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dpToPx(16), 0, dpToPx(16), 0)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(38)
                ).apply {
                    if (indice > 0) marginStart = dpToPx(8)
                }
                setOnClickListener {
                    filtroEstado = clave
                    actualizarEstiloChips()
                    aplicarFiltros()
                }
            }
            chipsEstado.add(clave to chip)
            container.addView(chip)
        }

        actualizarEstiloChips()
    }

    private fun actualizarEstiloChips() {
        val blanco = ContextCompat.getColor(requireContext(), R.color.white)
        val primario = ContextCompat.getColor(requireContext(), R.color.apex_text_primary)
        chipsEstado.forEach { (clave, chip) ->
            val activo = clave == filtroEstado
            chip.setBackgroundResource(if (activo) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
            chip.setTextColor(if (activo) blanco else primario)
        }
    }

    private fun aplicarFiltros() {
        filtrados = empleados.filter { empleado ->
            val coincideEstado = filtroEstado == FILTRO_TODOS || categoriaEstado(empleado) == filtroEstado
            val coincideTexto = textoBusqueda.isBlank() ||
                empleado.nombreCompleto.contains(textoBusqueda, ignoreCase = true) ||
                empleado.correo?.contains(textoBusqueda, ignoreCase = true) == true ||
                empleado.telefono?.contains(textoBusqueda, ignoreCase = true) == true ||
                empleado.puesto?.nombre?.contains(textoBusqueda, ignoreCase = true) == true
            coincideEstado && coincideTexto
        }

        if (filtrados.isEmpty()) {
            binding.llEmptyEmpleados.visibility = View.VISIBLE
            binding.rvEmpleados.visibility = View.GONE
            mostrarMensajeVacio(
                when {
                    empleados.isEmpty() -> "No hay empleados registrados"
                    textoBusqueda.isNotBlank() -> "No hay empleados que coincidan con la búsqueda"
                    else -> "No hay empleados en esta categoría"
                }
            )
            return
        }

        binding.llEmptyEmpleados.visibility = View.GONE
        binding.rvEmpleados.visibility = View.VISIBLE
        renderizar()
    }

    private fun mostrarMensajeVacio(mensaje: String) {
        val grupo = binding.llEmptyEmpleados
        for (i in 0 until grupo.childCount) {
            val hijo = grupo.getChildAt(i)
            if (hijo is TextView && hijo.textSize >= 14f) {
                hijo.text = mensaje
                break
            }
        }
    }

    private fun renderizar() {
        binding.rvEmpleados.adapter = object : RecyclerView.Adapter<EmpleadoViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpleadoViewHolder {
                val card = MaterialCardView(parent.context).apply {
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

                val row = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
                }

                val avatar = FrameLayout(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44))
                    setBackgroundResource(R.drawable.bg_chip_unselected)
                    addView(ImageView(parent.context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            dpToPx(22),
                            dpToPx(22),
                            Gravity.CENTER
                        )
                        setImageResource(R.drawable.ic_user_profile)
                        setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                    })
                }
                row.addView(avatar)

                val col = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply { marginStart = dpToPx(14) }
                }
                val tvNombre = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTypeface(typeface, Typeface.BOLD)
                }
                col.addView(tvNombre)
                val tvRol = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_orange))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                }
                col.addView(tvRol)
                val tvEmail = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                col.addView(tvEmail)
                row.addView(col)

                val chevron = ImageView(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20))
                    setImageResource(R.drawable.ic_chevron_right)
                    setColorFilter(
                        ContextCompat.getColor(context, R.color.apex_text_secondary)
                    )
                }
                row.addView(chevron)
                card.addView(row)

                return EmpleadoViewHolder(card, tvNombre, tvRol, tvEmail)
            }

            override fun onBindViewHolder(holder: EmpleadoViewHolder, position: Int) {
                holder.bind(filtrados[position])
            }

            override fun getItemCount(): Int = filtrados.size
        }
    }

    private inner class EmpleadoViewHolder(
        itemView: View,
        private val tvNombre: TextView,
        private val tvRol: TextView,
        private val tvEmail: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(empleado: EmpleadoDto) {
            tvNombre.text = empleado.nombreCompleto.ifBlank { "Empleado #${empleado.id}" }
            tvRol.text = listOfNotNull(
                empleado.puesto?.nombre,
                empleado.estadoEmpleado?.nombre
            ).joinToString(" • ").ifBlank { "Puesto no asignado" }
            tvEmail.text = empleado.correo
                ?: empleado.usuario?.email
                ?: empleado.telefono
                ?: "Sin datos de contacto"

            itemView.setOnClickListener {
                navegar(EmpleadoDetalleFragment.newInstance(empleado.id))
            }
        }
    }

    private fun navegar(fragmento: androidx.fragment.app.Fragment) {
        parentFragmentManager.beginTransaction()
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

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()

    companion object {
        private const val FILTRO_TODOS = "TODOS"
        private const val FILTRO_ACTIVOS = "ACTIVOS"
        private const val FILTRO_VACACIONES = "VACACIONES"
        private const val FILTRO_INACTIVOS = "INACTIVOS"
    }
}

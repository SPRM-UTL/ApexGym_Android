package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.api.ClienteAsistenciaDto
import com.sprm.apexgym.core.network.client.RetrofitClient
import com.sprm.apexgym.core.network.dataExitosa
import com.sprm.apexgym.core.network.mensajeDeError
import com.sprm.apexgym.databinding.FragmentClienteBusquedaSheetBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClienteBusquedaSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentClienteBusquedaSheetBinding? = null
    private val binding get() = _binding!!
    private var resultados: List<ClienteAsistenciaDto> = emptyList()
    private var jobBusqueda: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteBusquedaSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseSearch.setOnClickListener { dismiss() }
        binding.rvClientSearchResults.layoutManager = LinearLayoutManager(requireContext())

        binding.etSearchClientSheet.doAfterTextChanged { editable ->
            val q = editable?.toString()?.trim().orEmpty()
            jobBusqueda?.cancel()
            if (q.length < 2) {
                resultados = emptyList()
                mostrarMensaje("Escribe al menos 2 caracteres (nombre, correo o teléfono)")
                return@doAfterTextChanged
            }
            jobBusqueda = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                buscar(q)
            }
        }

        binding.etSearchClientSheet.setOnEditorActionListener { _, _, _ ->
            val q = binding.etSearchClientSheet.text?.toString()?.trim().orEmpty()
            jobBusqueda?.cancel()
            buscar(q)
            true
        }

        val terminoInicial = arguments?.getString(ARG_TERMINO).orEmpty()
        if (terminoInicial.isNotBlank()) {
            binding.etSearchClientSheet.setText(terminoInicial)
        } else {
            mostrarMensaje("Escribe al menos 2 caracteres (nombre, correo o teléfono)")
            binding.etSearchClientSheet.requestFocus()
        }
    }

    private fun buscar(termino: String) {
        if (termino.length < 2) {
            mostrarMensaje("Escribe al menos 2 caracteres (nombre, correo o teléfono)")
            return
        }

        mostrarMensaje("Buscando socios…")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.buscarCliente(termino)
                val data = response.dataExitosa()
                if (!isAdded) return@launch
                if (data == null) {
                    resultados = emptyList()
                    mostrarMensaje(
                        response.mensajeDeError() ?: "No se pudo realizar la búsqueda"
                    )
                } else {
                    resultados = data
                    if (data.isEmpty()) {
                        mostrarMensaje("No se encontraron socios con \"$termino\"")
                    } else {
                        renderizar()
                    }
                }
            } catch (_: Exception) {
                if (isAdded) {
                    resultados = emptyList()
                    mostrarMensaje("No se pudo conectar con el servidor")
                }
            }
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        binding.llEmptyState.visibility = View.VISIBLE
        binding.rvClientSearchResults.visibility = View.GONE
        binding.tvEmptySearchMessage.text = mensaje
    }

    private fun renderizar() {
        if (resultados.isEmpty()) return
        binding.llEmptyState.visibility = View.GONE
        binding.rvClientSearchResults.visibility = View.VISIBLE

        binding.rvClientSearchResults.adapter = object :
            RecyclerView.Adapter<ClienteViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
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
                    setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
                }

                val col = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                val tvNombre = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                col.addView(tvNombre)
                val tvContacto = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                }
                col.addView(tvContacto)
                row.addView(col)

                val chevron = ImageView(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18))
                    setImageResource(R.drawable.ic_chevron_right)
                    setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                }
                row.addView(chevron)
                card.addView(row)

                return ClienteViewHolder(card, tvNombre, tvContacto)
            }

            override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
                holder.bind(resultados[position])
            }

            override fun getItemCount(): Int = resultados.size
        }
    }

    private inner class ClienteViewHolder(
        itemView: View,
        private val tvNombre: TextView,
        private val tvContacto: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(cliente: ClienteAsistenciaDto) {
            tvNombre.text = cliente.nombreCompleto.ifBlank { "Socio" }
            tvContacto.text = listOfNotNull(
                cliente.correo?.takeIf { it.isNotBlank() },
                cliente.telefono?.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { "Sin datos de contacto" }

            itemView.setOnClickListener {
                val hostFm = requireActivity().supportFragmentManager
                dismissAllowingStateLoss()
                hostFm.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(
                        R.id.fragment_container,
                        AccesoManualFragment.newInstance(cliente.id)
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        jobBusqueda?.cancel()
        _binding = null
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()

    companion object {
        const val TAG = "ClienteBusquedaSheet"
        private const val ARG_TERMINO = "arg_termino"

        fun newInstance(termino: String = ""): ClienteBusquedaSheetFragment {
            return ClienteBusquedaSheetFragment().apply {
                arguments = Bundle().apply { putString(ARG_TERMINO, termino) }
            }
        }
    }
}

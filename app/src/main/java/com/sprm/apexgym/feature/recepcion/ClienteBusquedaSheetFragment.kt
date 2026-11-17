package com.sprm.apexgym.feature.recepcion

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.launch

class ClienteBusquedaSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentClienteBusquedaSheetBinding? = null
    private val binding get() = _binding!!
    private var resultados: List<ClienteAsistenciaDto> = emptyList()

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

        binding.etSearchClientSheet.setOnEditorActionListener { _, _, _ ->
            buscar(binding.etSearchClientSheet.text?.toString()?.trim().orEmpty())
            true
        }

        val terminoInicial = arguments?.getString(ARG_TERMINO).orEmpty()
        if (terminoInicial.isNotBlank()) {
            binding.etSearchClientSheet.setText(terminoInicial)
            buscar(terminoInicial)
        }
    }

    private fun buscar(termino: String) {
        if (termino.isBlank()) {
            mostrarMensaje("Ingresa nombre, correo o teléfono del cliente")
            return
        }

        mostrarMensaje("Buscando clientes…")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.empleadoService.buscarCliente(termino)
                val data = response.dataExitosa()
                if (data == null) {
                    resultados = emptyList()
                    mostrarMensaje(
                        response.mensajeDeError() ?: "No se pudo realizar la búsqueda"
                    )
                } else {
                    resultados = data
                    renderizar()
                    if (data.isEmpty()) {
                        mostrarMensaje("No se encontraron clientes con ese criterio")
                    }
                }
            } catch (_: Exception) {
                resultados = emptyList()
                mostrarMensaje("No se pudo conectar con el servidor")
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
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.apex_card_dark))
                    radius = dpToPx(16).toFloat()
                    cardElevation = dpToPx(2).toFloat()
                    strokeWidth = 0
                    isClickable = true
                    isFocusable = true
                }

                val row = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
                }

                val iconBox = FrameLayout(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
                    setBackgroundResource(R.drawable.bg_icon_circle_orange_outline)
                    addView(ImageView(parent.context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            dpToPx(20),
                            dpToPx(20),
                            Gravity.CENTER
                        )
                        setImageResource(R.drawable.ic_user_profile)
                        setColorFilter(ContextCompat.getColor(context, R.color.apex_orange))
                    })
                }
                row.addView(iconBox)

                val col = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply { marginStart = dpToPx(12) }
                }
                val tvNombre = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                col.addView(tvNombre)
                val tvContacto = TextView(parent.context).apply {
                    setTextColor(ContextCompat.getColor(context, R.color.apex_text_on_dark_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                col.addView(tvContacto)
                row.addView(col)
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
            tvNombre.text = cliente.nombreCompleto.ifBlank { "Cliente #${cliente.id}" }
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

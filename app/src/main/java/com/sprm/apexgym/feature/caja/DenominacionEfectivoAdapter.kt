package com.sprm.apexgym.feature.caja

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sprm.apexgym.R
import com.sprm.apexgym.core.network.formatearMoneda
import com.sprm.apexgym.core.ui.InputFilters

class DenominacionEfectivoAdapter(
    private val items: List<DenominacionEfectivo>,
    private val conteo: MutableMap<String, Int>,
    private val onCambioCantidad: () -> Unit
) : RecyclerView.Adapter<DenominacionEfectivoAdapter.DenominacionHolder>() {

    class DenominacionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDenominacion: TextView = itemView.findViewById(R.id.tvDenominacion)
        val tvTipoDenominacion: TextView = itemView.findViewById(R.id.tvTipoDenominacion)
        val btnMenos: TextView = itemView.findViewById(R.id.btnMenos)
        val etCantidad: EditText = itemView.findViewById(R.id.etCantidad)
        val btnMas: TextView = itemView.findViewById(R.id.btnMas)
        val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
        val vDivider: View = itemView.findViewById(R.id.vDivider)
        var watcher: TextWatcher? = null
        var actualizandoTexto = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DenominacionHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_denominacion_efectivo, parent, false)
        return DenominacionHolder(vista)
    }

    override fun onBindViewHolder(holder: DenominacionHolder, position: Int) {
        val item = items[position]
        val cantidadInicial = DesgloseEfectivo.normalizarCantidad(conteo[item.clave] ?: 0)

        holder.tvDenominacion.text = item.etiqueta
        holder.tvTipoDenominacion.text = item.tipo.etiqueta
        holder.vDivider.visibility = if (position == items.lastIndex) View.GONE else View.VISIBLE

        holder.watcher?.let { holder.etCantidad.removeTextChangedListener(it) }
        holder.etCantidad.filters = InputFilters.digitsOnly(4)
        holder.actualizandoTexto = true
        holder.etCantidad.setText(if (cantidadInicial > 0) cantidadInicial.toString() else "")
        holder.actualizandoTexto = false
        actualizarSubtotalFila(holder, item, cantidadInicial)

        val nuevoWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (holder.actualizandoTexto) return
                val texto = s?.toString()?.trim().orEmpty()
                val crudo = texto.toIntOrNull() ?: 0
                val normalizada = DesgloseEfectivo.normalizarCantidad(crudo)
                if (crudo > DesgloseEfectivo.MAX_CANTIDAD_POR_DENOMINACION) {
                    holder.actualizandoTexto = true
                    val textoLimite = normalizada.toString()
                    holder.etCantidad.setText(textoLimite)
                    holder.etCantidad.setSelection(textoLimite.length)
                    holder.actualizandoTexto = false
                }
                conteo[item.clave] = normalizada
                actualizarSubtotalFila(holder, item, normalizada)
                onCambioCantidad()
            }
        }
        holder.watcher = nuevoWatcher
        holder.etCantidad.addTextChangedListener(nuevoWatcher)

        holder.btnMenos.setOnClickListener {
            val actual = conteo[item.clave] ?: 0
            aplicarCantidadDesdeBoton(holder, item, actual - 1)
        }

        holder.btnMas.setOnClickListener {
            val actual = conteo[item.clave] ?: 0
            aplicarCantidadDesdeBoton(holder, item, actual + 1)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun aplicarCantidadDesdeBoton(
        holder: DenominacionHolder,
        item: DenominacionEfectivo,
        candidato: Int
    ) {
        val nuevaCantidad = DesgloseEfectivo.normalizarCantidad(candidato)
        conteo[item.clave] = nuevaCantidad
        holder.actualizandoTexto = true
        val texto = if (nuevaCantidad > 0) nuevaCantidad.toString() else ""
        holder.etCantidad.setText(texto)
        holder.etCantidad.setSelection(texto.length)
        holder.actualizandoTexto = false
        actualizarSubtotalFila(holder, item, nuevaCantidad)
        onCambioCantidad()
    }

    private fun actualizarSubtotalFila(
        holder: DenominacionHolder,
        item: DenominacionEfectivo,
        cantidad: Int
    ) {
        holder.tvSubtotal.text = (item.valor * cantidad).formatearMoneda()
    }
}

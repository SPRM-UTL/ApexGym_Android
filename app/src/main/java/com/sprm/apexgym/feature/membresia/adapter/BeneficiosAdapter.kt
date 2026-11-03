package com.sprm.apexgym.feature.membresia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sprm.apexgym.databinding.ItemBeneficioBinding
import com.sprm.apexgym.feature.membresia.model.BeneficioItem

class BeneficiosAdapter(
    private var items: List<BeneficioItem> = emptyList()
) : RecyclerView.Adapter<BeneficiosAdapter.BeneficioViewHolder>() {

    fun submitList(nuevosItems: List<BeneficioItem>) {
        items = nuevosItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeneficioViewHolder {
        val binding = ItemBeneficioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BeneficioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BeneficioViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class BeneficioViewHolder(
        private val binding: ItemBeneficioBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BeneficioItem) {
            binding.tvTituloBeneficio.text = item.titulo
            binding.tvDescripcionBeneficio.text = item.descripcion ?: "Beneficio exclusivo para socios activos"
        }
    }
}

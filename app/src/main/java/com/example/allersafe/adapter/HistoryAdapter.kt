package com.example.allersafe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.allersafe.R
import com.example.allersafe.data.model.ScanResult
import com.example.allersafe.data.model.ScanStatus

class HistoryAdapter(
    private var historyList: List<ScanResult>,
    private val onItemClick: (ScanResult) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvAllergen: TextView = view.findViewById(R.id.tvAllergenWarning)
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = historyList[position]

        // Baris 1: Nama produk (dan brand jika tersedia)
        holder.tvName.text = if (result.productBrand.isNotBlank()) {
            "${result.productName} · ${result.productBrand}"
        } else {
            result.productName
        }

        // Memuat Gambar Produk menggunakan Glide
        Glide.with(holder.itemView.context)
            .load(result.imageUrl.ifEmpty { null })
            .placeholder(R.drawable.ic_placeholder_product) // Pastikan drawable ini ada
            .error(R.drawable.ic_placeholder_product)
            .into(holder.imgProduct)

        // Reset tint jika sebelumnya ada tint pada placeholder
        if (result.imageUrl.isNotEmpty()) {
            holder.imgProduct.imageTintList = null
        }

        // Baris 2: Alergen terdeteksi ATAU status scan
        when {
            result.detectedAllergens.isNotEmpty() -> {
                val allergenNames = result.detectedAllergens
                    .joinToString(", ") { it.allergenType.indonesianName }
                holder.tvAllergen.text = "⚠ Mengandung: $allergenNames"
                holder.tvAllergen.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.alert_danger)
                )
            }
            result.crossContaminationWarnings.isNotEmpty() -> {
                holder.tvAllergen.text = "⚡ Potensi kontaminasi silang"
                holder.tvAllergen.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.accent_primary)
                )
            }
            result.scanStatus == ScanStatus.SAFE -> {
                holder.tvAllergen.text = "✓ Aman dikonsumsi"
                holder.tvAllergen.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.accent_primary)
                )
            }
            else -> {
                holder.tvAllergen.text = result.scanStatus.name
                holder.tvAllergen.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
                )
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(result)
        }
    }

    override fun getItemCount() = historyList.size

    fun updateData(newList: List<ScanResult>) {
        historyList = newList
        notifyDataSetChanged()
    }
}

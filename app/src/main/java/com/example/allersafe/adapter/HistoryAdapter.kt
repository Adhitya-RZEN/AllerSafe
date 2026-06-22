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

    // 1. TAMBAHKAN tvBrand dan tvScanTime di sini agar terhubung dengan XML baru
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvBrand: TextView = view.findViewById(R.id.tvProductBrand)
        val tvScanTime: TextView = view.findViewById(R.id.tvScanTime)
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

        // 2. SET NAMA PRODUK
        holder.tvName.text = result.productName

        // 3. LOGIC BRAND: Sembunyikan jika kosong, tampilkan jika ada
        if (result.productBrand.isBlank()) {
            holder.tvBrand.visibility = View.GONE
        } else {
            holder.tvBrand.visibility = View.VISIBLE
            holder.tvBrand.text = result.productBrand
        }

        // 4. SET WAKTU SCAN (Sesuai instruksi: Tampilkan "Baru Dipindai" jika tidak ada data waktu)
        // Jika model ScanResult Anda punya data waktu, Anda bisa menggantinya di sini.
        holder.tvScanTime.text = "Baru Dipindai"

        // 5. MEMUAT GAMBAR
        Glide.with(holder.itemView.context)
            .load(result.imageUrl.ifEmpty { null })
            .placeholder(R.drawable.ic_placeholder_product)
            .error(R.drawable.ic_placeholder_product)
            .into(holder.imgProduct)

        if (result.imageUrl.isNotEmpty()) {
            holder.imgProduct.imageTintList = null
        }

        // 6. LOGIC ALERGEN
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
                holder.tvAllergen.text = "⚡ Potensi kontaminasi"
                holder.tvAllergen.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.accent_primary)
                )
            }
            result.scanStatus == ScanStatus.SAFE -> {
                holder.tvAllergen.text = "✓ Aman dikonsumsi"
                holder.tvAllergen.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.alergen_safe)
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
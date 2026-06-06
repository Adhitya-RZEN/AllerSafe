package com.example.allersafe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.allersafe.R
import com.example.allersafe.data.model.ScanResult

class HistoryAdapter(
    private var historyList: List<ScanResult>,
    // Ini adalah listener baru yang akan mengirimkan data saat item diklik
    private val onItemClick: (ScanResult) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // PERBAIKAN: Menggunakan ID yang benar dari item_history.xml
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvAllergen: TextView = view.findViewById(R.id.tvAllergenType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = historyList[position]

        holder.tvName.text = result.productName
        
        // Menampilkan brand produk di baris kedua
        holder.tvAllergen.text = result.productBrand

        // Memberikan aksi klik pada keseluruhan Card/Item
        holder.itemView.setOnClickListener {
            onItemClick(result) // Mengirimkan data produk yang diklik kembali ke MainActivity
        }
    }

    override fun getItemCount() = historyList.size

    // Fungsi untuk memperbarui data saat ada scan baru / ditarik dari database
    fun updateData(newList: List<ScanResult>) {
        historyList = newList
        notifyDataSetChanged()
    }
}
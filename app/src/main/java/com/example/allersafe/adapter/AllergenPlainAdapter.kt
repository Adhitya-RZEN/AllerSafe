package com.example.allersafe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.allersafe.R

class AllergenPlainAdapter(private var allergens: List<String>) : RecyclerView.Adapter<AllergenPlainAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAllergenName: TextView = view.findViewById(R.id.tvAllergenName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_allergen_plain, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvAllergenName.text = allergens[position]
    }

    override fun getItemCount() = allergens.size

    fun updateData(newData: List<String>) {
        allergens = newData
        notifyDataSetChanged()
    }
}
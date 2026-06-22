package com.example.allersafe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.allersafe.R
import com.example.allersafe.data.model.ProductDBModel

class SearchProductAdapter(
    private val productList: List<ProductDBModel>,
    private val onItemClick: (ProductDBModel) -> Unit
) : RecyclerView.Adapter<SearchProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvProductName)
        val tvBrand: TextView = view.findViewById(R.id.tvProductBrand)
        val imgProduct: ImageView = view.findViewById(R.id.imgSearchProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]
        holder.tvName.text = product.name
        holder.tvBrand.text = product.brand

        Glide.with(holder.itemView.context)
            .load(product.imageUrl.ifEmpty { R.drawable.ic_placeholder_product })
            .placeholder(R.drawable.ic_placeholder_product)
            .error(R.drawable.ic_placeholder_product)
            .circleCrop()
            .into(holder.imgProduct)

        holder.itemView.setOnClickListener {
            onItemClick(product)
        }
    }

    override fun getItemCount() = productList.size
}

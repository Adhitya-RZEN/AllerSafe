package com.example.allersafe.data.model

import com.google.gson.annotations.SerializedName

// Ini adalah wadah utama dari hasil pencarian
data class OffSearchResponse(
    val count: Int,
    val products: List<OffProduct>
)

// Ini adalah wadah untuk masing-masing produk
data class OffProduct(
    @SerializedName("product_name")
    val productName: String?,

    @SerializedName("brands")
    val brands: String?,

    // Ini adalah data paling krusial untuk mesin AllerSafe Anda!
    @SerializedName("ingredients_text")
    val ingredientsText: String?,

    @SerializedName("image_url")
    val imageUrl: String?
)
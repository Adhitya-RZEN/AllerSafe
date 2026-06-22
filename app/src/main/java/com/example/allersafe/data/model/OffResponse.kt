package com.example.allersafe.data.model

import com.google.gson.annotations.SerializedName

data class OffSearchResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("products")
    val products: List<OffProduct>?
)

data class OffProduct(
    @SerializedName("product_name")
    val productName: String?,

    @SerializedName("product_name_id")
    val productNameId: String?,

    @SerializedName("product_name_en")
    val productNameEn: String?,

    @SerializedName("brands")
    val brands: String?,

    @SerializedName("ingredients_text")
    val ingredientsText: String?,

    @SerializedName("ingredients_text_id")
    val ingredientsTextId: String?,

    @SerializedName("ingredients_text_en")
    val ingredientsTextEn: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("image_front_url")
    val imageFrontUrl: String?,

    @SerializedName("image_small_url")
    val imageSmallUrl: String?
) {
    fun getBestName(): String? {
        return productName?.takeIf { it.isNotBlank() }
            ?: productNameId?.takeIf { it.isNotBlank() }
            ?: productNameEn?.takeIf { it.isNotBlank() }
    }

    fun getBestIngredients(): String? {
        return ingredientsText?.takeIf { it.isNotBlank() }
            ?: ingredientsTextId?.takeIf { it.isNotBlank() }
            ?: ingredientsTextEn?.takeIf { it.isNotBlank() }
    }

    fun getBestImage(): String? {
        return imageUrl?.takeIf { it.isNotBlank() }
            ?: imageFrontUrl?.takeIf { it.isNotBlank() }
            ?: imageSmallUrl?.takeIf { it.isNotBlank() }
    }
}

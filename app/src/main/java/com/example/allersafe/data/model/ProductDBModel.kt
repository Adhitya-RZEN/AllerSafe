package com.example.allersafe.data.model

data class ProductDBModel(
    val id: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val brand: String = "",
    val ingredients: List<String> = emptyList(),
    val rawIngredientsText: String = "",
    val mayContainText: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        fun normalizeName(name: String): String =
            name.lowercase().trim().replace(Regex("[^a-z0-9_]"), "_")
    }
}
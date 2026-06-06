package com.example.allersafe.data.model

enum class ScanStatus { SAFE, WARNING, DANGER }

data class ScanResult(
    val id: String = "",
    val userId: String = "",
    val productName: String = "",
    val productBrand: String = "",
    val scanStatus: ScanStatus = ScanStatus.SAFE,
    val detectedAllergens: List<DetectedAllergen> = emptyList(),
    val crossContaminationWarnings: List<String> = emptyList(),
    val analyzedIngredients: List<IngredientItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id, "userId" to userId, "productName" to productName,
        "productBrand" to productBrand, "scanStatus" to scanStatus.name,
        "detectedAllergens" to detectedAllergens.map { it.toMap() },
        "crossContaminationWarnings" to crossContaminationWarnings,
        "analyzedIngredients" to analyzedIngredients.map { it.toMap() },
        "timestamp" to timestamp
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any>): ScanResult = ScanResult(
            id = id,
            userId = map["userId"] as? String ?: "",
            productName = map["productName"] as? String ?: "",
            productBrand = map["productBrand"] as? String ?: "",
            scanStatus = runCatching { ScanStatus.valueOf(map["scanStatus"] as? String ?: "SAFE") }.getOrDefault(ScanStatus.SAFE),
            detectedAllergens = (map["detectedAllergens"] as? List<Map<String, Any>>)?.map { DetectedAllergen.fromMap(it) } ?: emptyList(),
            crossContaminationWarnings = (map["crossContaminationWarnings"] as? List<String>) ?: emptyList(),
            analyzedIngredients = (map["analyzedIngredients"] as? List<Map<String, Any>>)?.map { IngredientItem.fromMap(it) } ?: emptyList(),
            timestamp = map["timestamp"] as? Long ?: System.currentTimeMillis()
        )
    }
}

data class DetectedAllergen(
    val allergenType: AllergenType,
    val matchedIngredient: String,
    val synonym: String,
    val isDirect: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "allergenType" to allergenType.name, "matchedIngredient" to matchedIngredient,
        "synonym" to synonym, "isDirect" to isDirect
    )

    companion object {
        fun fromMap(map: Map<String, Any>): DetectedAllergen = DetectedAllergen(
            allergenType = runCatching { AllergenType.valueOf(map["allergenType"] as? String ?: "MILK") }.getOrDefault(AllergenType.MILK),
            matchedIngredient = map["matchedIngredient"] as? String ?: "",
            synonym = map["synonym"] as? String ?: "",
            isDirect = map["isDirect"] as? Boolean ?: true
        )
    }
}

data class IngredientItem(
    val name: String,
    val isAllergen: Boolean,
    val allergenType: AllergenType? = null,
    val isCrossContamination: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name, "isAllergen" to isAllergen,
        "allergenType" to allergenType?.name, "isCrossContamination" to isCrossContamination
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): IngredientItem = IngredientItem(
            name = map["name"] as? String ?: "",
            isAllergen = map["isAllergen"] as? Boolean ?: false,
            allergenType = (map["allergenType"] as? String)?.let { runCatching { AllergenType.valueOf(it) }.getOrNull() },
            isCrossContamination = map["isCrossContamination"] as? Boolean ?: false
        )
    }
}
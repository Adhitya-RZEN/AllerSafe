package com.example.allersafe.engine

import com.example.allersafe.data.model.AllergenProfile
import com.example.allersafe.data.model.AllergenType
import com.example.allersafe.data.model.DetectedAllergen
import com.example.allersafe.data.model.IngredientItem
import com.example.allersafe.data.model.ScanResult
import com.example.allersafe.data.model.ScanStatus

object AllergenEngine {

    fun analyze(
        productName: String,
        productBrand: String,
        imageUrl: String, // Tambahkan parameter imageUrl
        rawIngredients: String,
        userProfile: AllergenProfile,
        userId: String = ""
    ): ScanResult {
        val activeAllergens = userProfile.activeAllergens()
        val lowerIngredients = rawIngredients.lowercase()

        val directMatches = detectAllergens(lowerIngredients, activeAllergens, isDirect = true)
        val crossContaminationWarnings = detectCrossContaminationPhrases(lowerIngredients)

        val status = when {
            directMatches.isNotEmpty() -> ScanStatus.DANGER
            crossContaminationWarnings.isNotEmpty() -> ScanStatus.WARNING
            else -> ScanStatus.SAFE
        }

        val analyzedIngredients = buildAnalyzedIngredients(
            rawIngredients = rawIngredients,
            activeAllergens = activeAllergens
        )

        return ScanResult(
            id = "scan_${System.currentTimeMillis()}",
            userId = userId,
            productName = productName,
            productBrand = productBrand,
            imageUrl = imageUrl, // Pass imageUrl ke ScanResult
            scanStatus = status,
            detectedAllergens = directMatches,
            crossContaminationWarnings = crossContaminationWarnings,
            analyzedIngredients = analyzedIngredients
        )
    }

    private fun buildAnalyzedIngredients(
        rawIngredients: String,
        activeAllergens: List<AllergenType>
    ): List<IngredientItem> {
        if (rawIngredients.isBlank()) return emptyList()

        val tokens = rawIngredients
            .split(",", ";")
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() }

        return tokens.map { ingredient ->
            val lowerIngredient = ingredient.lowercase()

            val matchedEntry = SynonymMap.synonyms.entries.firstOrNull { (term, allergenType) ->
                activeAllergens.contains(allergenType) &&
                        lowerIngredient.contains(term.lowercase())
            }

            IngredientItem(
                name = ingredient,
                isAllergen = matchedEntry != null,
                allergenType = matchedEntry?.value,
                isCrossContamination = false
            )
        }
    }

    private fun detectAllergens(
        lowerText: String,
        activeAllergens: List<AllergenType>,
        isDirect: Boolean
    ): List<DetectedAllergen> {
        val detected = mutableListOf<DetectedAllergen>()

        for ((term, allergenType) in SynonymMap.synonyms) {
            if (activeAllergens.contains(allergenType) && lowerText.contains(term.lowercase())) {
                detected.add(DetectedAllergen(allergenType, term, term, isDirect))
            }
        }

        return detected.distinctBy { it.allergenType }
    }

    private fun detectCrossContaminationPhrases(lowerText: String): List<String> {
        return SynonymMap.crossContaminationPhrases.filter { lowerText.contains(it.lowercase()) }
    }
}
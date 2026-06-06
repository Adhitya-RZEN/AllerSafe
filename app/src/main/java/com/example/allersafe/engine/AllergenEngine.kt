package com.example.allersafe.engine

import com.example.allersafe.data.model.AllergenProfile
import com.example.allersafe.data.model.AllergenType
import com.example.allersafe.data.model.DetectedAllergen
import com.example.allersafe.data.model.ScanResult
import com.example.allersafe.data.model.ScanStatus

object AllergenEngine {
    fun analyze(
        productName: String, 
        productBrand: String, 
        rawIngredients: String,
        userProfile: AllergenProfile, 
        userId: String = ""
    ): ScanResult {
        val activeAllergens = userProfile.activeAllergens()
        val lowerIngredients = rawIngredients.lowercase()
        
        // PERBAIKAN: Deteksi langsung ke teks asli agar bisa menangkap frasa majemuk (contoh: "kacang tanah")
        val directMatches = detectAllergens(lowerIngredients, activeAllergens, true)
        val crossContaminationWarnings = detectCrossContaminationPhrases(lowerIngredients)

        val status = when {
            directMatches.isNotEmpty() -> ScanStatus.DANGER
            crossContaminationWarnings.isNotEmpty() -> ScanStatus.WARNING
            else -> ScanStatus.SAFE
        }

        return ScanResult(
            id = "scan_${System.currentTimeMillis()}", 
            userId = userId,
            productName = productName, 
            productBrand = productBrand,
            scanStatus = status, 
            detectedAllergens = directMatches,
            crossContaminationWarnings = crossContaminationWarnings
        )
    }

    private fun detectAllergens(lowerText: String, activeAllergens: List<AllergenType>, isDirect: Boolean): List<DetectedAllergen> {
        val detected = mutableListOf<DetectedAllergen>()
        
        // Mencocokkan setiap frasa dari SynonymMap langsung ke dalam teks komposisi
        for ((term, allergenType) in SynonymMap.synonyms) {
            if (activeAllergens.contains(allergenType) && lowerText.contains(term.lowercase())) {
                detected.add(DetectedAllergen(allergenType, term, term, isDirect))
            }
        }
        
        // Pastikan tidak ada duplikasi kategori alergen yang sama
        return detected.distinctBy { it.allergenType }
    }

    private fun detectCrossContaminationPhrases(lowerText: String): List<String> {
        return SynonymMap.crossContaminationPhrases.filter { lowerText.contains(it.lowercase()) }
    }
}

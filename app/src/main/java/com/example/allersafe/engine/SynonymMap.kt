package com.example.allersafe.engine

import com.example.allersafe.data.model.AllergenType

object SynonymMap {
    // Mapping kata kunci ke tipe alergen (Semua dalam lowercase)
    val synonyms: Map<String, AllergenType> = buildMap {
        val milkTerms = listOf("milk", "susu", "dairy", "casein", "whey", "lactose", "keju", "cheese", "butter", "mentega", "laktosa", "krim", "cream")
        milkTerms.forEach { put(it, AllergenType.MILK) }
        
        val eggTerms = listOf("egg", "telur", "eggs", "albumin", "mayones", "mayonnaise")
        eggTerms.forEach { put(it, AllergenType.EGG) }
        
        val wheatTerms = listOf("wheat", "gandum", "gluten", "flour", "tepung terigu", "barley", "malt", "tepung")
        wheatTerms.forEach { put(it, AllergenType.WHEAT) }
        
        val peanutTerms = listOf("peanut", "kacang tanah", "kacang", "peanut butter")
        peanutTerms.forEach { put(it, AllergenType.PEANUT) }
        
        val soyTerms = listOf("kedelai", "soy", "soya", "tahu", "tempe", "kecap", "lecithin", "lesitin")
        soyTerms.forEach { put(it, AllergenType.SOY) }
        
        val treeNutTerms = listOf("almond", "mete", "cashew", "walnut", "hazelnut", "kacang pohon", "tree nut", "pistachio")
        treeNutTerms.forEach { put(it, AllergenType.TREE_NUT) }
        
        val fishTerms = listOf("ikan", "fish", "salmon", "tuna", "teri", "tenggiri")
        fishTerms.forEach { put(it, AllergenType.FISH) }
        
        val shellfishTerms = listOf("seafood", "udang", "kerang", "kepiting", "shellfish", "cumi", "squid", "lobster", "prawn")
        shellfishTerms.forEach { put(it, AllergenType.SHELLFISH) }
    }

    // Fungsi untuk mendapatkan semua kata kunci berdasarkan tipe alergen yang aktif
    fun getKeywordsFor(activeTypes: List<AllergenType>): List<String> {
        return synonyms.filter { activeTypes.contains(it.value) }.keys.toList()
    }

    val crossContaminationPhrases: List<String> = listOf(
        "mengandung jejak", "mungkin mengandung", "diproses di fasilitas yang", "may contain", "traces of"
    )
}

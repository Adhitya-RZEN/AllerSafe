package com.example.allersafe.data.model

data class AllergenProfile(
    val milk: Boolean = false,
    val egg: Boolean = false,
    val wheat: Boolean = false,
    val soy: Boolean = false,
    val peanut: Boolean = false,
    val treeNut: Boolean = false,
    val fish: Boolean = false,
    val shellfish: Boolean = false
) {
    fun activeAllergens(): List<AllergenType> {
        val list = mutableListOf<AllergenType>()
        if (milk) list.add(AllergenType.MILK)
        if (egg) list.add(AllergenType.EGG)
        if (wheat) list.add(AllergenType.WHEAT)
        if (soy) list.add(AllergenType.SOY)
        if (peanut) list.add(AllergenType.PEANUT)
        if (treeNut) list.add(AllergenType.TREE_NUT)
        if (fish) list.add(AllergenType.FISH)
        if (shellfish) list.add(AllergenType.SHELLFISH)
        return list
    }

    fun toMap(): Map<String, Any> = mapOf(
        "milk" to milk, "egg" to egg, "wheat" to wheat, "soy" to soy,
        "peanut" to peanut, "treeNut" to treeNut, "fish" to fish, "shellfish" to shellfish
    )

    companion object {
        fun fromMap(map: Map<String, Any>): AllergenProfile = AllergenProfile(
            milk = map["milk"] as? Boolean ?: false,
            egg = map["egg"] as? Boolean ?: false,
            wheat = map["wheat"] as? Boolean ?: false,
            soy = map["soy"] as? Boolean ?: false,
            peanut = map["peanut"] as? Boolean ?: false,
            treeNut = map["treeNut"] as? Boolean ?: false,
            fish = map["fish"] as? Boolean ?: false,
            shellfish = map["shellfish"] as? Boolean ?: false
        )
    }
}

enum class AllergenType(val displayName: String, val indonesianName: String) {
    MILK("Milk", "Susu"),
    EGG("Egg", "Telur"),
    WHEAT("Wheat", "Gandum/Gluten"),
    SOY("Soy", "Kedelai"),
    PEANUT("Peanut", "Kacang Tanah"),
    TREE_NUT("Tree Nut", "Kacang Pohon"),
    FISH("Fish", "Ikan"),
    SHELLFISH("Shellfish", "Seafood/Kerang")
}
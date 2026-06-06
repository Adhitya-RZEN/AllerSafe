package com.example.allersafe.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val fullName: String = "",
    val dob: String = "",
    val gender: String = "",
    val location: String = "",
    val profileImageUrl: String = "",
    val allergenProfile: AllergenProfile = AllergenProfile(),
    val createdAt: Long = System.currentTimeMillis()
)
package com.example.allersafe.data.repository

import com.example.allersafe.data.model.AllergenProfile
import com.example.allersafe.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val TIMEOUT_DURATION = 10000L
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun register(email: String, password: String, displayName: String, profile: AllergenProfile): Result<User> = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Registrasi Auth gagal")

            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName,
                allergenProfile = profile
            )
            db.collection(COLLECTION_USERS).document(firebaseUser.uid).set(userToMap(user)).await()
            user
        }
    }

    suspend fun getEmailByUsername(username: String): String? = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            // Pencarian case-sensitive sesuai permintaan
            val snapshot = db.collection(COLLECTION_USERS)
                .whereEqualTo("displayName", username)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) snapshot.documents[0].getString("email") else null
        }
    }.getOrNull()

    suspend fun login(email: String, password: String): Result<User> = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login Auth gagal")

            val doc = db.collection(COLLECTION_USERS).document(firebaseUser.uid).get().await()
            if (doc.exists() && doc.data != null) userFromMap(doc.id, doc.data!!)
            else User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<User> = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Autentikasi Google gagal")

            val doc = db.collection(COLLECTION_USERS).document(firebaseUser.uid).get().await()

            if (!doc.exists() || doc.data == null) {
                val name = firebaseUser.displayName ?: "Google User"
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = name,
                    profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
                    allergenProfile = AllergenProfile()
                )
                db.collection(COLLECTION_USERS).document(firebaseUser.uid).set(userToMap(newUser)).await()
                newUser
            } else {
                userFromMap(doc.id, doc.data!!)
            }
        }
    }

    fun logout() { auth.signOut() }

    suspend fun getUserProfile(uid: String): User? = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            val doc = db.collection(COLLECTION_USERS).document(uid).get().await()
            if (doc.exists() && doc.data != null) userFromMap(doc.id, doc.data!!) else null
        }
    }.getOrNull()

    suspend fun updateUserDetails(
        uid: String, displayName: String, dob: String,
        gender: String, profileImageUrl: String, allergenProfile: AllergenProfile
    ): Result<Unit> = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            val updates = mapOf(
                "displayName" to displayName,
                "dob" to dob,
                "gender" to gender,
                "profileImageUrl" to profileImageUrl,
                "allergenProfile" to allergenProfile.toMap()
            )
            db.collection(COLLECTION_USERS).document(uid).set(updates, SetOptions.merge()).await()
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getMasterAllergens(): List<String> = runCatching {
        withTimeout(5000L) {
            val doc = db.collection("settings").document("allergens").get().await()
            doc.get("list") as? List<String> ?: listOf("Susu", "Telur", "Gluten", "Seafood", "Kacang", "Keju")
        }
    }.getOrDefault(listOf("Susu", "Telur", "Gluten", "Seafood", "Kacang", "Keju"))

    private fun userToMap(user: User): Map<String, Any> = mapOf(
        "uid" to user.uid, "email" to user.email, "displayName" to user.displayName,
        "fullName" to user.fullName, "dob" to user.dob, "gender" to user.gender,
        "location" to user.location, "profileImageUrl" to user.profileImageUrl,
        "allergenProfile" to user.allergenProfile.toMap(), "createdAt" to user.createdAt
    )

    private fun userFromMap(uid: String, map: Map<String, Any>): User {
        val profileMap = map["allergenProfile"] as? Map<String, Any>
        return User(
            uid = uid,
            email = map["email"] as? String ?: "",
            displayName = map["displayName"] as? String ?: "",
            fullName = map["fullName"] as? String ?: "",
            dob = map["dob"] as? String ?: "",
            gender = map["gender"] as? String ?: "",
            location = map["location"] as? String ?: "",
            profileImageUrl = map["profileImageUrl"] as? String ?: "",
            allergenProfile = profileMap?.let { AllergenProfile.fromMap(it) } ?: AllergenProfile()
        )
    }
}
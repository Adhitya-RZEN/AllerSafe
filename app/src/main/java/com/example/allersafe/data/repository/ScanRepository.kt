package com.example.allersafe.data.repository

import com.example.allersafe.data.api.OpenFoodFactsApi
import com.example.allersafe.data.model.ProductDBModel
import com.example.allersafe.data.model.ScanResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

class ScanRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val SUBCOLLECTION_HISTORY = "history"
        private const val TIMEOUT_DURATION = 15000L // Increased timeout

        private const val PAGE_SIZE_SINGLE = 20
        private const val PAGE_SIZE_MULTIPLE = 50
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenFoodFactsApi::class.java)

    suspend fun findProductFromAPI(queryName: String): ProductDBModel? = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            val cleanQuery = queryName.trim().lowercase()

            val response = api.searchProducts(
                searchQuery = cleanQuery,
                pageSize = PAGE_SIZE_SINGLE
            )

            if (response.isSuccessful && response.body() != null) {
                val results = response.body()?.products ?: emptyList()

                // Filter items that have at least a name and ingredients
                val validProducts = results.mapNotNull { item ->
                    val name = item.getBestName()
                    val ingredients = item.getBestIngredients()
                    if (name != null && ingredients != null) {
                        ProductDBModel(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            normalizedName = ProductDBModel.normalizeName(name),
                            brand = item.brands ?: "Tidak diketahui",
                            imageUrl = item.getBestImage() ?: "",
                            rawIngredientsText = ingredients
                        )
                    } else null
                }

                // Return best match or first valid result
                return@withTimeout validProducts.firstOrNull {
                    it.name.lowercase().contains(cleanQuery)
                } ?: validProducts.firstOrNull()
            }
            return@withTimeout null
        }
    }.getOrNull()

    suspend fun saveScanResult(scanResult: ScanResult): Result<String> = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            val docRef = db.collection(COLLECTION_USERS).document(scanResult.userId)
                .collection(SUBCOLLECTION_HISTORY).document(scanResult.id)
            docRef.set(scanResult.toMap()).await()
            scanResult.id
        }
    }

    suspend fun searchMultipleProducts(queryName: String): List<ProductDBModel> {
        return try {
            withTimeout(TIMEOUT_DURATION) {
                val cleanQuery = queryName.trim().lowercase()

                val response = api.searchProducts(
                    searchQuery = cleanQuery,
                    pageSize = PAGE_SIZE_MULTIPLE
                )

                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()?.products ?: emptyList()
                    results.mapNotNull { item ->
                        val validName = item.getBestName()
                        // Some products might not have ingredients text in the search list,
                        // but we still want to show them so the user can try to select them.
                        // However, AllergenEngine needs ingredients.
                        if (validName != null) {
                            ProductDBModel(
                                name = validName,
                                brand = item.brands ?: "Tidak diketahui",
                                imageUrl = item.getBestImage() ?: "",
                                rawIngredientsText = item.getBestIngredients() ?: ""
                            )
                        } else null
                    }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ScanRepository", "Search failed", e)
            emptyList()
        }
    }
}

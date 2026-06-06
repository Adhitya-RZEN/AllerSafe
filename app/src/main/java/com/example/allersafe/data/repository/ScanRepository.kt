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
        private const val TIMEOUT_DURATION = 10000L
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenFoodFactsApi::class.java)

    suspend fun findProductFromAPI(queryName: String): ProductDBModel? = runCatching {
        withTimeout(TIMEOUT_DURATION) {
            val cleanQuery = queryName.trim().lowercase()
            val response = api.searchProducts(searchQuery = cleanQuery)

            if (response.isSuccessful && response.body() != null) {
                val results = response.body()!!.products

                val validProduct = results.firstOrNull {
                    !it.ingredientsText.isNullOrEmpty() &&
                            (it.productName?.lowercase()?.contains(cleanQuery) == true)
                }

                if (validProduct != null) {
                    val finalName = validProduct.productName ?: queryName
                    return@withTimeout ProductDBModel(
                        id = UUID.randomUUID().toString(),
                        name = finalName,
                        normalizedName = ProductDBModel.normalizeName(finalName),
                        brand = validProduct.brands ?: "Tidak diketahui",
                        rawIngredientsText = validProduct.ingredientsText!!
                    )
                }
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
        return withTimeout(TIMEOUT_DURATION) {
            // FIXED: Selalu lowercase agar "Indomilk" dan "indomilk" hasilnya sama
            val cleanQuery = queryName.trim().lowercase()
            val response = api.searchProducts(searchQuery = cleanQuery, pageSize = 30)

            if (response.isSuccessful && response.body() != null) {
                val results = response.body()!!.products
                results.mapNotNull { item ->
                    val validName = item.productName?.takeIf { it.isNotBlank() }
                        ?: item.brands?.takeIf { it.isNotBlank() }

                    if (validName != null) {
                        ProductDBModel(
                            name = validName,
                            brand = item.brands ?: "Tidak diketahui",
                            rawIngredientsText = item.ingredientsText ?: ""
                        )
                    } else null
                }
            } else {
                throw Exception("Gagal mendapat data dari server")
            }
        }
    }
}

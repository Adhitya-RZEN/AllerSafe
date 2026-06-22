package com.example.allersafe.data.api

import com.example.allersafe.data.model.OffSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFoodFactsApi {
    // Endpoint pencarian OpenFoodFacts
    // pageSize tidak lagi memiliki nilai default agar setiap pemanggil
    // wajib menentukan jumlahnya secara eksplisit — menghilangkan konflik
    // antara findProductFromAPI() dan searchMultipleProducts()
    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchQuery: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int
    ): Response<OffSearchResponse>
}
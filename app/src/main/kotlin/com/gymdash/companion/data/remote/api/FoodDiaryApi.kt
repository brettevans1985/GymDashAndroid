package com.gymdash.companion.data.remote.api

import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FoodDiaryApi {

    @GET("food/lookup/{barcode}")
    suspend fun lookupBarcode(
        @Header("Authorization") token: String,
        @Path("barcode") barcode: String
    ): FoodLookupResponse

    @GET("food/search")
    suspend fun searchFood(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): List<FoodLookupResponse>

    @POST("food-diary")
    suspend fun createEntry(
        @Header("Authorization") token: String,
        @Body request: CreateFoodDiaryEntryRequest
    )
}

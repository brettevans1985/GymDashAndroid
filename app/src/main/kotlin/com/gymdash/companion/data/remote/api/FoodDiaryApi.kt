package com.gymdash.companion.data.remote.api

import com.gymdash.companion.data.remote.dto.CreateBuilderEntriesRequest
import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.CreateRecipeRequest
import com.gymdash.companion.data.remote.dto.FoodDiaryEntryDto
import com.gymdash.companion.data.remote.dto.FoodDiaryResponse
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import com.gymdash.companion.data.remote.dto.FoodSearchResponse
import com.gymdash.companion.data.remote.dto.RecipeDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FoodDiaryApi {

    @GET("food-diary/recent")
    suspend fun getRecentItems(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 20
    ): List<FoodDiaryEntryDto>

    @GET("food-diary")
    suspend fun getDiary(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): FoodDiaryResponse

    @DELETE("food-diary/{id}")
    suspend fun deleteEntry(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    )

    @PUT("food-diary/{id}")
    suspend fun updateEntry(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    )

    @GET("food/lookup/{barcode}")
    suspend fun lookupBarcode(
        @Header("Authorization") token: String,
        @Path("barcode") barcode: String
    ): FoodLookupResponse

    @GET("food/search")
    suspend fun searchFood(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): FoodSearchResponse

    @POST("food-diary")
    suspend fun createEntry(
        @Header("Authorization") token: String,
        @Body request: CreateFoodDiaryEntryRequest
    )

    @GET("recipes")
    suspend fun getRecipes(
        @Header("Authorization") token: String
    ): List<RecipeDto>

    @GET("recipes/{id}")
    suspend fun getRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): RecipeDto

    @POST("recipes")
    suspend fun createRecipe(
        @Header("Authorization") token: String,
        @Body request: CreateRecipeRequest
    ): RecipeDto

    @PUT("recipes/{id}")
    suspend fun updateRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CreateRecipeRequest
    ): RecipeDto

    @DELETE("recipes/{id}")
    suspend fun deleteRecipe(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    )

    @POST("recipes/add-to-diary")
    suspend fun addBuilderEntriesToDiary(
        @Header("Authorization") token: String,
        @Body request: CreateBuilderEntriesRequest
    )
}

package com.gymdash.companion.domain.repository

import com.gymdash.companion.data.remote.dto.CreateBuilderEntriesRequest
import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.CreateRecipeRequest
import com.gymdash.companion.data.remote.dto.FoodDiaryResponse
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import com.gymdash.companion.data.remote.dto.RecipeDto

interface FoodDiaryRepository {
    suspend fun getDiary(date: String): FoodDiaryResponse
    suspend fun deleteEntry(id: Int)
    suspend fun updateEntry(id: Int, updates: Map<String, Any>)
    suspend fun lookupBarcode(barcode: String): FoodLookupResponse?
    suspend fun searchFood(query: String): List<FoodLookupResponse>
    suspend fun createEntry(request: CreateFoodDiaryEntryRequest)
    suspend fun getRecipes(): List<RecipeDto>
    suspend fun getRecipe(id: Int): RecipeDto
    suspend fun createRecipe(request: CreateRecipeRequest): RecipeDto
    suspend fun deleteRecipe(id: Int)
    suspend fun addBuilderEntriesToDiary(request: CreateBuilderEntriesRequest)
}

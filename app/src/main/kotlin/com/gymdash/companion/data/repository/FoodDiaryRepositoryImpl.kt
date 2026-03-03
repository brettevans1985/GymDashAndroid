package com.gymdash.companion.data.repository

import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.remote.api.FoodDiaryApi
import com.gymdash.companion.data.remote.dto.CreateBuilderEntriesRequest
import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.CreateRecipeRequest
import com.gymdash.companion.data.remote.dto.FoodDiaryResponse
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import com.gymdash.companion.data.remote.dto.RecipeDto
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import javax.inject.Inject

class FoodDiaryRepositoryImpl @Inject constructor(
    private val foodDiaryApi: FoodDiaryApi,
    private val preferences: SyncPreferences
) : FoodDiaryRepository {

    override suspend fun getDiary(date: String): FoodDiaryResponse {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        return foodDiaryApi.getDiary(token, date)
    }

    override suspend fun deleteEntry(id: Int) {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        foodDiaryApi.deleteEntry(token, id)
    }

    override suspend fun updateEntry(id: Int, updates: Map<String, Any>) {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        foodDiaryApi.updateEntry(token, id, updates)
    }

    override suspend fun lookupBarcode(barcode: String): FoodLookupResponse? {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        return try {
            foodDiaryApi.lookupBarcode(token, barcode)
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        }
    }

    override suspend fun searchFood(query: String): List<FoodLookupResponse> {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        return foodDiaryApi.searchFood(token, query).results
    }

    override suspend fun createEntry(request: CreateFoodDiaryEntryRequest) {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        foodDiaryApi.createEntry(token, request)
    }

    override suspend fun getRecipes(): List<RecipeDto> {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        return foodDiaryApi.getRecipes(token)
    }

    override suspend fun getRecipe(id: Int): RecipeDto {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        return foodDiaryApi.getRecipe(token, id)
    }

    override suspend fun createRecipe(request: CreateRecipeRequest): RecipeDto {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        return foodDiaryApi.createRecipe(token, request)
    }

    override suspend fun deleteRecipe(id: Int) {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        foodDiaryApi.deleteRecipe(token, id)
    }

    override suspend fun addBuilderEntriesToDiary(request: CreateBuilderEntriesRequest) {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        foodDiaryApi.addBuilderEntriesToDiary(token, request)
    }
}

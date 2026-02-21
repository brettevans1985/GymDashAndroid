package com.gymdash.companion.data.repository

import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.remote.api.FoodDiaryApi
import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import javax.inject.Inject

class FoodDiaryRepositoryImpl @Inject constructor(
    private val foodDiaryApi: FoodDiaryApi,
    private val preferences: SyncPreferences
) : FoodDiaryRepository {

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

        return foodDiaryApi.searchFood(token, query)
    }

    override suspend fun createEntry(request: CreateFoodDiaryEntryRequest) {
        val token = preferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")

        foodDiaryApi.createEntry(token, request)
    }
}

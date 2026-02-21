package com.gymdash.companion.domain.repository

import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.FoodLookupResponse

interface FoodDiaryRepository {
    suspend fun lookupBarcode(barcode: String): FoodLookupResponse?
    suspend fun searchFood(query: String): List<FoodLookupResponse>
    suspend fun createEntry(request: CreateFoodDiaryEntryRequest)
}

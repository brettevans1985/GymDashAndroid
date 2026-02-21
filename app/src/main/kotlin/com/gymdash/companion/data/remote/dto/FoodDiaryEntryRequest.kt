package com.gymdash.companion.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateFoodDiaryEntryRequest(
    val foodProductId: Int?,
    val calendarDate: String,        // yyyy-MM-dd
    val mealCategory: Int,           // 0=Breakfast, 1=Lunch, 2=Dinner, 3=Snack
    val quantity: Double,
    val productName: String,
    val barcode: String?,
    val servingSize: Double,
    val servingUnit: String?,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
    val fibrePerServing: Double,
    val saltPerServing: Double,
    val entrySource: Int             // 0=BarcodeScan, 1=ManualSearch
)

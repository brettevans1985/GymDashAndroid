package com.gymdash.companion.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FoodLookupResponse(
    val id: Int,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val servingSize: Double?,
    val servingUnit: String?,
    val caloriesPerServing: Double?,
    val proteinPerServing: Double?,
    val carbsPerServing: Double?,
    val fatPerServing: Double?,
    val fibrePerServing: Double?,
    val saltPerServing: Double?,
    val imageUrl: String?,
    val source: String
)

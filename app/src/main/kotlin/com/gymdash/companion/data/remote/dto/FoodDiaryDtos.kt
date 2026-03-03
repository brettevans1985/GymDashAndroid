package com.gymdash.companion.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FoodDiaryResponse(
    val date: String,
    val meals: List<MealGroupDto>,
    val dailyTotals: MacroTotalsDto,
    val targets: DailyTargetsDto?,
    val waterMl: Int
)

@JsonClass(generateAdapter = true)
data class MealGroupDto(
    val mealCategory: String,
    val entries: List<FoodDiaryEntryDto>,
    val subtotals: MacroTotalsDto
)

@JsonClass(generateAdapter = true)
data class FoodDiaryEntryDto(
    val id: Int,
    val productName: String,
    val barcode: String?,
    val servingSize: Double,
    val servingUnit: String?,
    val quantity: Double,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
    val fibrePerServing: Double,
    val saltPerServing: Double,
    val mealCategory: String,
    val entrySource: String,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class DailyTargetsDto(
    val calorieGoal: Double?,
    val proteinTarget: Double?,
    val carbsTarget: Double?,
    val fatTarget: Double?,
    val fibreTarget: Double?,
    val saltTarget: Double?,
    val waterGoalMl: Int?
)

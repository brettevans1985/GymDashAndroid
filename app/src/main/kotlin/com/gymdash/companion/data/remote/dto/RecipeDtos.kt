package com.gymdash.companion.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecipeDto(
    val id: Int,
    val name: String,
    val ingredients: List<RecipeIngredientDto>,
    val totals: MacroTotalsDto,
    val createdAt: String,
    val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class RecipeIngredientDto(
    val id: Int,
    val foodProductId: Int?,
    val productName: String,
    val barcode: String?,
    val servingSize: Double,
    val servingUnit: String?,
    val quantity: Double,
    val divisor: Double,
    val caloriesPerServing: Double?,
    val proteinPerServing: Double?,
    val carbsPerServing: Double?,
    val fatPerServing: Double?,
    val fibrePerServing: Double?,
    val saltPerServing: Double?,
    val sortOrder: Int,
    val effectiveCalories: Double,
    val effectiveProtein: Double,
    val effectiveCarbs: Double,
    val effectiveFat: Double,
    val effectiveFibre: Double,
    val effectiveSalt: Double
)

@JsonClass(generateAdapter = true)
data class MacroTotalsDto(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fibre: Double,
    val salt: Double
)

@JsonClass(generateAdapter = true)
data class CreateRecipeRequest(
    val name: String,
    val ingredients: List<CreateRecipeIngredientRequest>
)

@JsonClass(generateAdapter = true)
data class CreateRecipeIngredientRequest(
    val foodProductId: Int?,
    val productName: String,
    val barcode: String?,
    val servingSize: Double,
    val servingUnit: String?,
    val quantity: Double,
    val divisor: Double,
    val caloriesPerServing: Double?,
    val proteinPerServing: Double?,
    val carbsPerServing: Double?,
    val fatPerServing: Double?,
    val fibrePerServing: Double?,
    val saltPerServing: Double?
)

@JsonClass(generateAdapter = true)
data class CreateBuilderEntriesRequest(
    val calendarDate: String,
    val mealCategory: Int,
    val asCombined: Boolean,
    val combinedName: String?,
    val ingredients: List<CreateRecipeIngredientRequest>,
    val recipeId: Int?
)

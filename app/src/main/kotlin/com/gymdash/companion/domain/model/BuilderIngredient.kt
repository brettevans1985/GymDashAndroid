package com.gymdash.companion.domain.model

import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import java.util.UUID

data class BuilderIngredient(
    val localId: String = UUID.randomUUID().toString(),
    val foodProduct: FoodLookupResponse,
    val quantity: Double = 1.0,
    val divisor: Double = 1.0
) {
    val effectiveCalories: Double
        get() = ((foodProduct.caloriesPerServing ?: 0.0) * quantity) / divisor

    val effectiveProtein: Double
        get() = ((foodProduct.proteinPerServing ?: 0.0) * quantity) / divisor

    val effectiveCarbs: Double
        get() = ((foodProduct.carbsPerServing ?: 0.0) * quantity) / divisor

    val effectiveFat: Double
        get() = ((foodProduct.fatPerServing ?: 0.0) * quantity) / divisor

    val effectiveFibre: Double
        get() = ((foodProduct.fibrePerServing ?: 0.0) * quantity) / divisor

    val effectiveSalt: Double
        get() = ((foodProduct.saltPerServing ?: 0.0) * quantity) / divisor
}

package com.gymdash.companion.ui.fooddiary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.remote.dto.CreateBuilderEntriesRequest
import com.gymdash.companion.data.remote.dto.CreateRecipeIngredientRequest
import com.gymdash.companion.data.remote.dto.CreateRecipeRequest
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import com.gymdash.companion.data.remote.dto.RecipeDto
import com.gymdash.companion.domain.model.BuilderIngredient
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodBuilderUiState(
    val ingredients: List<BuilderIngredient> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<FoodLookupResponse> = emptyList(),
    val isSearching: Boolean = false,
    val showSearch: Boolean = false,
    val showSaveDialog: Boolean = false,
    val asCombined: Boolean = true,
    val combinedName: String = "",
    val selectedMeal: Int = 0,
    val saveAsRecipe: Boolean = false,
    val recipeName: String = "",
    val recipes: List<RecipeDto> = emptyList(),
    val showRecipeList: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val totalCalories: Double get() = ingredients.sumOf { it.effectiveCalories }
    val totalProtein: Double get() = ingredients.sumOf { it.effectiveProtein }
    val totalCarbs: Double get() = ingredients.sumOf { it.effectiveCarbs }
    val totalFat: Double get() = ingredients.sumOf { it.effectiveFat }
}

@HiltViewModel
class FoodBuilderViewModel @Inject constructor(
    private val repository: FoodDiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodBuilderUiState())
    val uiState: StateFlow<FoodBuilderUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search() {
        val query = _uiState.value.searchQuery
        if (query.length < 3) return

        _uiState.update { it.copy(isSearching = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val results = repository.searchFood(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Search failed: ${e.message}",
                    isSearching = false
                ) }
            }
        }
    }

    fun addIngredient(product: FoodLookupResponse) {
        _uiState.update { state ->
            state.copy(
                ingredients = state.ingredients + BuilderIngredient(foodProduct = product),
                showSearch = false,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun removeIngredient(localId: String) {
        _uiState.update { state ->
            state.copy(ingredients = state.ingredients.filter { it.localId != localId })
        }
    }

    fun updateQuantity(localId: String, quantity: Double) {
        _uiState.update { state ->
            state.copy(ingredients = state.ingredients.map {
                if (it.localId == localId) it.copy(quantity = quantity) else it
            })
        }
    }

    fun updateDivisor(localId: String, divisor: Double) {
        if (divisor <= 0) return
        _uiState.update { state ->
            state.copy(ingredients = state.ingredients.map {
                if (it.localId == localId) it.copy(divisor = divisor) else it
            })
        }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(showSearch = !it.showSearch) }
    }

    fun showSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = true, errorMessage = null) }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false) }
    }

    fun updateAsCombined(value: Boolean) {
        _uiState.update { it.copy(asCombined = value) }
    }

    fun updateCombinedName(name: String) {
        _uiState.update { it.copy(combinedName = name) }
    }

    fun updateSelectedMeal(meal: Int) {
        _uiState.update { it.copy(selectedMeal = meal) }
    }

    fun updateSaveAsRecipe(value: Boolean) {
        _uiState.update { it.copy(saveAsRecipe = value) }
    }

    fun updateRecipeName(name: String) {
        _uiState.update { it.copy(recipeName = name) }
    }

    fun loadRecipes() {
        _uiState.update { it.copy(showRecipeList = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val recipes = repository.getRecipes()
                _uiState.update { it.copy(recipes = recipes) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load recipes: ${e.message}") }
            }
        }
    }

    fun dismissRecipeList() {
        _uiState.update { it.copy(showRecipeList = false) }
    }

    fun loadRecipe(id: Int) {
        viewModelScope.launch {
            try {
                val recipe = repository.getRecipe(id)
                val ingredients = recipe.ingredients.map { ing ->
                    BuilderIngredient(
                        foodProduct = FoodLookupResponse(
                            id = ing.foodProductId ?: 0,
                            barcode = ing.barcode,
                            name = ing.productName,
                            brand = null,
                            servingSize = ing.servingSize,
                            servingUnit = ing.servingUnit,
                            caloriesPerServing = ing.caloriesPerServing,
                            proteinPerServing = ing.proteinPerServing,
                            carbsPerServing = ing.carbsPerServing,
                            fatPerServing = ing.fatPerServing,
                            fibrePerServing = ing.fibrePerServing,
                            saltPerServing = ing.saltPerServing,
                            imageUrl = null,
                            source = "recipe"
                        ),
                        quantity = ing.quantity,
                        divisor = ing.divisor
                    )
                }
                _uiState.update { it.copy(
                    ingredients = ingredients,
                    showRecipeList = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load recipe: ${e.message}") }
            }
        }
    }

    fun deleteRecipe(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(id)
                _uiState.update { state ->
                    state.copy(recipes = state.recipes.filter { it.id != id })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete recipe: ${e.message}") }
            }
        }
    }

    fun saveToDiary(date: String, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.ingredients.isEmpty()) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val ingredientRequests = state.ingredients.map { ing ->
                    CreateRecipeIngredientRequest(
                        foodProductId = if (ing.foodProduct.id != 0) ing.foodProduct.id else null,
                        productName = ing.foodProduct.name,
                        barcode = ing.foodProduct.barcode,
                        servingSize = ing.foodProduct.servingSize ?: 100.0,
                        servingUnit = ing.foodProduct.servingUnit,
                        quantity = ing.quantity,
                        divisor = ing.divisor,
                        caloriesPerServing = ing.foodProduct.caloriesPerServing,
                        proteinPerServing = ing.foodProduct.proteinPerServing,
                        carbsPerServing = ing.foodProduct.carbsPerServing,
                        fatPerServing = ing.foodProduct.fatPerServing,
                        fibrePerServing = ing.foodProduct.fibrePerServing,
                        saltPerServing = ing.foodProduct.saltPerServing
                    )
                }

                if (state.saveAsRecipe && state.recipeName.isNotBlank()) {
                    repository.createRecipe(CreateRecipeRequest(
                        name = state.recipeName.trim(),
                        ingredients = ingredientRequests
                    ))
                }

                repository.addBuilderEntriesToDiary(CreateBuilderEntriesRequest(
                    calendarDate = date,
                    mealCategory = state.selectedMeal,
                    asCombined = state.asCombined,
                    combinedName = if (state.asCombined) state.combinedName.ifBlank { null } else null,
                    ingredients = ingredientRequests,
                    recipeId = null
                ))

                _uiState.update { it.copy(isSaving = false, showSaveDialog = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSaving = false,
                    errorMessage = "Failed to save: ${e.message}"
                ) }
            }
        }
    }

    fun scanBarcode(barcode: String) {
        _uiState.update { it.copy(isSearching = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val product = repository.lookupBarcode(barcode)
                if (product != null) {
                    _uiState.update { state ->
                        state.copy(
                            ingredients = state.ingredients + BuilderIngredient(foodProduct = product),
                            isSearching = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(
                        errorMessage = "Product not found for barcode $barcode",
                        isSearching = false
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Barcode lookup failed: ${e.message}",
                    isSearching = false
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

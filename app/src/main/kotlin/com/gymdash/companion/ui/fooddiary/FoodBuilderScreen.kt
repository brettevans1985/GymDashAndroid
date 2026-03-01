package com.gymdash.companion.ui.fooddiary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodBuilderScreen(
    viewModel: FoodBuilderViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val mealNames = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Builder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("<")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Running totals card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MacroColumn("Cal", "%.0f".format(state.totalCalories))
                    MacroColumn("P", "%.1f".format(state.totalProtein))
                    MacroColumn("C", "%.1f".format(state.totalCarbs))
                    MacroColumn("F", "%.1f".format(state.totalFat))
                }
            }

            // Action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleSearch() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Ingredient")
                }
                OutlinedButton(
                    onClick = { viewModel.loadRecipes() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Recipes")
                }
            }

            if (state.errorMessage != null) {
                Text(
                    state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Inline search panel
            if (state.showSearch) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Search food (min 3 chars)") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.search() },
                                    enabled = state.searchQuery.length >= 3 && !state.isSearching
                                ) {
                                    Text("Go")
                                }
                            }
                        )

                        if (state.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(8.dp)
                            )
                        }

                        state.searchResults.take(5).forEach { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { viewModel.addIngredient(product) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(product.name, style = MaterialTheme.typography.bodyMedium)
                                    if (product.brand != null) {
                                        Text(product.brand, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        "${product.caloriesPerServing ?: "—"} kcal | P: ${product.proteinPerServing ?: "—"}g | C: ${product.carbsPerServing ?: "—"}g | F: ${product.fatPerServing ?: "—"}g",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ingredients list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.ingredients, key = { it.localId }) { ingredient ->
                    IngredientCard(
                        ingredient = ingredient,
                        onQuantityChange = { viewModel.updateQuantity(ingredient.localId, it) },
                        onDivisorChange = { viewModel.updateDivisor(ingredient.localId, it) },
                        onRemove = { viewModel.removeIngredient(ingredient.localId) }
                    )
                }
            }

            // Save button
            if (state.ingredients.isNotEmpty()) {
                Button(
                    onClick = { viewModel.showSaveDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Save to Diary")
                }
            }
        }
    }

    // Save dialog
    if (state.showSaveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveDialog() },
            title = { Text("Save to Diary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Combined vs Individual
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.asCombined,
                            onClick = { viewModel.updateAsCombined(true) }
                        )
                        Text("Combined entry", modifier = Modifier.clickable { viewModel.updateAsCombined(true) })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !state.asCombined,
                            onClick = { viewModel.updateAsCombined(false) }
                        )
                        Text("Individual entries", modifier = Modifier.clickable { viewModel.updateAsCombined(false) })
                    }

                    if (state.asCombined) {
                        OutlinedTextField(
                            value = state.combinedName,
                            onValueChange = { viewModel.updateCombinedName(it) },
                            label = { Text("Combined name (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Meal category
                    Text("Meal", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        mealNames.forEachIndexed { index, name ->
                            FilterChip(
                                selected = state.selectedMeal == index,
                                onClick = { viewModel.updateSelectedMeal(index) },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Save as recipe
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.saveAsRecipe,
                            onCheckedChange = { viewModel.updateSaveAsRecipe(it) }
                        )
                        Text("Save as recipe", modifier = Modifier.clickable {
                            viewModel.updateSaveAsRecipe(!state.saveAsRecipe)
                        })
                    }

                    if (state.saveAsRecipe) {
                        OutlinedTextField(
                            value = state.recipeName,
                            onValueChange = { viewModel.updateRecipeName(it) },
                            label = { Text("Recipe name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (state.errorMessage != null) {
                        Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveToDiary(
                            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            onSuccess = onNavigateBack
                        )
                    },
                    enabled = !state.isSaving && !(state.saveAsRecipe && state.recipeName.isBlank())
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSaveDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Recipe list bottom sheet
    if (state.showRecipeList) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissRecipeList() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Saved Recipes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (state.recipes.isEmpty()) {
                    Text(
                        "No saved recipes yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(state.recipes) { recipe ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadRecipe(recipe.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(recipe.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "%.0f kcal | P: %.1fg | C: %.1fg | F: %.1fg".format(
                                                recipe.totals.calories,
                                                recipe.totals.protein,
                                                recipe.totals.carbs,
                                                recipe.totals.fat
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteRecipe(recipe.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete recipe",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MacroColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun IngredientCard(
    ingredient: com.gymdash.companion.domain.model.BuilderIngredient,
    onQuantityChange: (Double) -> Unit,
    onDivisorChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    var quantityText by remember(ingredient.localId, ingredient.quantity) {
        mutableStateOf(if (ingredient.quantity == ingredient.quantity.toLong().toDouble()) ingredient.quantity.toLong().toString() else ingredient.quantity.toString())
    }
    var divisorText by remember(ingredient.localId, ingredient.divisor) {
        mutableStateOf(if (ingredient.divisor == ingredient.divisor.toLong().toDouble()) ingredient.divisor.toLong().toString() else ingredient.divisor.toString())
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ingredient.foodProduct.name, style = MaterialTheme.typography.titleSmall)
                    if (ingredient.foodProduct.brand != null) {
                        Text(ingredient.foodProduct.brand, style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        it.toDoubleOrNull()?.let { qty -> if (qty > 0) onQuantityChange(qty) }
                    },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Text("\u00F7", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = divisorText,
                    onValueChange = {
                        divisorText = it
                        it.toDoubleOrNull()?.let { div -> if (div > 0) onDivisorChange(div) }
                    },
                    label = { Text("Divisor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "%.0f kcal | P: %.1fg | C: %.1fg | F: %.1fg".format(
                    ingredient.effectiveCalories,
                    ingredient.effectiveProtein,
                    ingredient.effectiveCarbs,
                    ingredient.effectiveFat
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

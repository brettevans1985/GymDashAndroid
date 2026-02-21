package com.gymdash.companion.ui.fooddiary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    repository: FoodDiaryRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodLookupResponse>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<FoodLookupResponse?>(null) }
    var quantity by remember { mutableStateOf("1.0") }
    var selectedMeal by remember { mutableIntStateOf(0) }
    var isAdding by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val mealNames = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Food") },
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
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (selectedProduct == null) {
                // Search input
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search food (min 3 chars)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.length >= 3) {
                                isSearching = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        results = repository.searchFood(query)
                                    } catch (e: Exception) {
                                        errorMessage = "Search failed: ${e.message}"
                                    }
                                    isSearching = false
                                }
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (query.length >= 3) {
                            isSearching = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    results = repository.searchFood(query)
                                } catch (e: Exception) {
                                    errorMessage = "Search failed: ${e.message}"
                                }
                                isSearching = false
                            }
                        }
                    },
                    enabled = query.length >= 3 && !isSearching,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSearching) "Searching..." else "Search")
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (results.isEmpty() && query.length >= 3) {
                    Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(results) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProduct = product }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(product.name, style = MaterialTheme.typography.titleSmall)
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
            } else {
                // Add entry form
                val p = selectedProduct!!
                Text(p.name, style = MaterialTheme.typography.headlineSmall)
                if (p.brand != null) {
                    Text(p.brand, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (servings)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Meal", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    mealNames.forEachIndexed { index, name ->
                        FilterChip(
                            selected = selectedMeal == index,
                            onClick = { selectedMeal = index },
                            label = { Text(name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { selectedProduct = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            val qty = quantity.toDoubleOrNull() ?: 1.0
                            isAdding = true
                            scope.launch {
                                try {
                                    repository.createEntry(
                                        CreateFoodDiaryEntryRequest(
                                            foodProductId = p.id,
                                            calendarDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                            mealCategory = selectedMeal,
                                            quantity = qty,
                                            productName = p.name,
                                            barcode = p.barcode,
                                            servingSize = p.servingSize ?: 100.0,
                                            servingUnit = p.servingUnit,
                                            caloriesPerServing = p.caloriesPerServing ?: 0.0,
                                            proteinPerServing = p.proteinPerServing ?: 0.0,
                                            carbsPerServing = p.carbsPerServing ?: 0.0,
                                            fatPerServing = p.fatPerServing ?: 0.0,
                                            fibrePerServing = p.fibrePerServing ?: 0.0,
                                            saltPerServing = p.saltPerServing ?: 0.0,
                                            entrySource = 1  // ManualSearch
                                        )
                                    )
                                    onNavigateBack()
                                } catch (e: Exception) {
                                    errorMessage = "Failed to add: ${e.message}"
                                }
                                isAdding = false
                            }
                        },
                        enabled = !isAdding,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isAdding) "Adding..." else "Add to Diary")
                    }
                }
            }
        }
    }
}

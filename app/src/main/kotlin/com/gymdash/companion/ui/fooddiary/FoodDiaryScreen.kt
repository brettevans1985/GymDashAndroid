package com.gymdash.companion.ui.fooddiary

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymdash.companion.data.remote.dto.FoodDiaryEntryDto
import com.gymdash.companion.data.remote.dto.MealGroupDto

private val mealCategoryNames = mapOf(
    0 to "Breakfast", 1 to "Lunch", 2 to "Dinner", 3 to "Snack"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDiaryScreen(
    viewModel: FoodDiaryViewModel,
    waterTrackerViewModel: WaterTrackerViewModel,
    onNavigateToScanner: (String) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToBuilder: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val waterState by waterTrackerViewModel.uiState.collectAsState()
    val dateString = state.selectedDate.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Diary") },
                actions = {
                    IconButton(onClick = { viewModel.showCopyDatePicker() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy from another day")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { onNavigateToBuilder(dateString) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text("Build", modifier = Modifier.padding(horizontal = 8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                SmallFloatingActionButton(
                    onClick = { onNavigateToSearch(dateString) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("Search", modifier = Modifier.padding(horizontal = 8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(onClick = { onNavigateToScanner(dateString) }) {
                    Icon(Icons.Default.Add, contentDescription = "Scan Food")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date navigation row
            item {
                DateNavigationRow(
                    label = state.dateLabel,
                    canGoForward = state.canNavigateForward,
                    onBack = { viewModel.navigateDate(-1) },
                    onForward = { viewModel.navigateDate(1) }
                )
            }

            // Daily summary card
            val diary = state.diary
            if (diary != null) {
                item {
                    DailySummaryCard(
                        calories = diary.dailyTotals.calories,
                        calorieGoal = diary.targets?.calorieGoal,
                        protein = diary.dailyTotals.protein,
                        carbs = diary.dailyTotals.carbs,
                        fat = diary.dailyTotals.fat
                    )
                }
            }

            // Water tracker
            item {
                if (state.isToday) {
                    WaterTrackerCard(
                        currentMl = waterState.currentMl,
                        goalMl = waterState.goalMl,
                        glasses = waterState.glasses,
                        progress = waterState.progress,
                        onAddGlass = { waterTrackerViewModel.addGlass() },
                        onRemoveGlass = { waterTrackerViewModel.removeGlass() }
                    )
                } else if (diary != null) {
                    ReadOnlyWaterCard(waterMl = diary.waterMl)
                }
            }

            // Loading state
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error state
            if (state.error != null) {
                item {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Meal groups
            if (diary != null && diary.meals.isNotEmpty()) {
                diary.meals.forEach { mealGroup ->
                    item(key = "header_${mealGroup.mealCategory}") {
                        MealGroupHeader(mealGroup)
                    }
                    items(
                        items = mealGroup.entries,
                        key = { entry -> entry.id }
                    ) { entry ->
                        SwipeToDeleteEntryCard(
                            entry = entry,
                            onDelete = { viewModel.requestDelete(entry) },
                            onTap = { viewModel.requestEdit(entry) }
                        )
                    }
                }
            } else if (diary != null && !state.isLoading) {
                item {
                    Text(
                        "No entries for this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            // Bottom spacer for FAB clearance
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Delete confirmation dialog
    if (state.entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            title = { Text("Delete Entry") },
            text = { Text("Remove \"${state.entryToDelete!!.productName}\" from your diary?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    enabled = !state.isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (state.isDeleting) "Deleting..." else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit entry bottom sheet
    if (state.entryToEdit != null) {
        EditEntryBottomSheet(
            entry = state.entryToEdit!!,
            isSaving = state.isSaving,
            onSave = { updates -> viewModel.saveEdit(updates) },
            onDismiss = { viewModel.dismissEdit() }
        )
    }

    // Copy date picker dialog
    if (state.showCopyDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { viewModel.dismissCopyDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.selectCopySourceDate(it) }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCopyDatePicker() }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Copy loading indicator
    if (state.isCopyLoading) {
        AlertDialog(
            onDismissRequest = {},
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Loading entries...")
                }
            },
            confirmButton = {}
        )
    }

    // Copy bottom sheet
    if (state.showCopySheet && state.copySourceDiary != null) {
        CopyEntriesBottomSheet(
            sourceDiary = state.copySourceDiary!!,
            sourceDate = state.copySourceDate!!,
            selectedIds = state.selectedCopyEntryIds,
            targetMeal = state.copyTargetMeal,
            isCopying = state.isCopying,
            copyError = state.copyError,
            onToggleEntry = { viewModel.toggleCopyEntry(it) },
            onToggleSelectAll = { viewModel.toggleSelectAllCopyEntries() },
            onSetTargetMeal = { viewModel.setCopyTargetMeal(it) },
            onCopy = { viewModel.executeCopy() },
            onDismiss = { viewModel.dismissCopySheet() }
        )
    }
}

@Composable
private fun DateNavigationRow(
    label: String,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
        }
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun DailySummaryCard(
    calories: Double,
    calorieGoal: Double?,
    protein: Double,
    carbs: Double,
    fat: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (calorieGoal != null && calorieGoal > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Calories",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "%.0f / %.0f kcal".format(calories, calorieGoal),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (calories / calorieGoal).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else {
                Text(
                    "%.0f kcal".format(calories),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroChip("P", "%.1fg".format(protein))
                MacroChip("C", "%.1fg".format(carbs))
                MacroChip("F", "%.1fg".format(fat))
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun WaterTrackerCard(
    currentMl: Int,
    goalMl: Int,
    glasses: Int,
    progress: Float,
    onAddGlass: () -> Unit,
    onRemoveGlass: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Water Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "$currentMl / $goalMl ml",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onRemoveGlass,
                    enabled = currentMl > 0
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Remove glass")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    "$glasses glasses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(16.dp))

                FilledTonalIconButton(onClick = onAddGlass) {
                    Icon(Icons.Default.Add, contentDescription = "Add glass")
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyWaterCard(waterMl: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Water",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$waterMl ml",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun MealGroupHeader(mealGroup: MealGroupDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            mealGroup.mealCategory,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "%.0f kcal".format(mealGroup.subtotals.calories),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteEntryCard(
    entry: FoodDiaryEntryDto,
    onDelete: () -> Unit,
    onTap: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't settle — the dialog will handle it
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        EntryCard(entry = entry, onClick = onTap)
    }
}

@Composable
private fun EntryCard(entry: FoodDiaryEntryDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                entry.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val servingLabel = buildString {
                append("%.1f".format(entry.quantity))
                append(" \u00D7 ")
                val size = entry.servingSize
                append(if (size == size.toLong().toDouble()) size.toLong().toString() else "%.1f".format(size))
                append(entry.servingUnit ?: "g")
            }
            Text(
                servingLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val totalCal = entry.caloriesPerServing * entry.quantity
            val totalP = entry.proteinPerServing * entry.quantity
            val totalC = entry.carbsPerServing * entry.quantity
            val totalF = entry.fatPerServing * entry.quantity
            Text(
                "%.0f kcal | P: %.1fg | C: %.1fg | F: %.1fg".format(totalCal, totalP, totalC, totalF),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntryBottomSheet(
    entry: FoodDiaryEntryDto,
    isSaving: Boolean,
    onSave: (Map<String, Any>) -> Unit,
    onDismiss: () -> Unit
) {
    var productName by remember { mutableStateOf(entry.productName) }
    var quantity by remember { mutableStateOf(entry.quantity.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it) }) }
    var selectedMeal by remember { mutableIntStateOf(
        when (entry.mealCategory) {
            "Breakfast" -> 0; "Lunch" -> 1; "Dinner" -> 2; else -> 3
        }
    ) }
    var calories by remember { mutableStateOf(entry.caloriesPerServing.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it) }) }
    var protein by remember { mutableStateOf(entry.proteinPerServing.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it) }) }
    var carbs by remember { mutableStateOf(entry.carbsPerServing.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it) }) }
    var fat by remember { mutableStateOf(entry.fatPerServing.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it) }) }
    var fibre by remember { mutableStateOf(entry.fibrePerServing.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it) }) }
    var salt by remember { mutableStateOf(entry.saltPerServing.let { if (it == it.toLong().toDouble()) it.toLong().toString() else "%.2f".format(it) }) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Edit Entry",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Product name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Meal", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val mealNames = listOf("Breakfast", "Lunch", "Dinner", "Snack")
                mealNames.forEachIndexed { index, name ->
                    FilterChip(
                        selected = selectedMeal == index,
                        onClick = { selectedMeal = index },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Macros (per serving)", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))

            // 2-column macro grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Carbs") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fat") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fibre,
                    onValueChange = { fibre = it },
                    label = { Text("Fibre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = salt,
                    onValueChange = { salt = it },
                    label = { Text("Salt") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val updates = mutableMapOf<String, Any>(
                        "productName" to productName,
                        "mealCategory" to selectedMeal
                    )
                    quantity.toDoubleOrNull()?.let { updates["quantity"] = it }
                    calories.toDoubleOrNull()?.let { updates["caloriesPerServing"] = it }
                    protein.toDoubleOrNull()?.let { updates["proteinPerServing"] = it }
                    carbs.toDoubleOrNull()?.let { updates["carbsPerServing"] = it }
                    fat.toDoubleOrNull()?.let { updates["fatPerServing"] = it }
                    fibre.toDoubleOrNull()?.let { updates["fibrePerServing"] = it }
                    salt.toDoubleOrNull()?.let { updates["saltPerServing"] = it }
                    onSave(updates)
                },
                enabled = !isSaving && productName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyEntriesBottomSheet(
    sourceDiary: com.gymdash.companion.data.remote.dto.FoodDiaryResponse,
    sourceDate: java.time.LocalDate,
    selectedIds: Set<Int>,
    targetMeal: Int?,
    isCopying: Boolean,
    copyError: String?,
    onToggleEntry: (Int) -> Unit,
    onToggleSelectAll: () -> Unit,
    onSetTargetMeal: (Int?) -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val allIds = sourceDiary.meals.flatMap { m -> m.entries.map { it.id } }.toSet()
    val allSelected = selectedIds == allIds && allIds.isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Copy from ${sourceDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (sourceDiary.meals.isEmpty()) {
                Text(
                    "No entries on this date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { onToggleSelectAll() }
                    )
                    Text("Select all", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    sourceDiary.meals.forEach { mealGroup ->
                        item(key = "copy_header_${mealGroup.mealCategory}") {
                            Text(
                                mealGroup.mealCategory,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(mealGroup.entries, key = { "copy_${it.id}" }) { entry ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = entry.id in selectedIds,
                                    onCheckedChange = { onToggleEntry(entry.id) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        entry.productName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "%.0f kcal".format(entry.caloriesPerServing * entry.quantity),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Target meal selector
                Text(
                    "Copy to meal",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val mealOptions = listOf(null to "Keep original", 0 to "Breakfast", 1 to "Lunch", 2 to "Dinner", 3 to "Snack")
                    mealOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = targetMeal == value,
                            onClick = { onSetTargetMeal(value) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (copyError != null) {
                    Text(
                        copyError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Button(
                    onClick = onCopy,
                    enabled = selectedIds.isNotEmpty() && !isCopying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        if (isCopying) "Copying..."
                        else "Copy ${selectedIds.size} ${if (selectedIds.size == 1) "entry" else "entries"}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

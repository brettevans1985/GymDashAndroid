package com.gymdash.companion.ui.fooddiary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.FoodDiaryEntryDto
import com.gymdash.companion.data.remote.dto.FoodDiaryResponse
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class FoodDiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val diary: FoodDiaryResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Delete flow
    val entryToDelete: FoodDiaryEntryDto? = null,
    val isDeleting: Boolean = false,
    // Copy flow
    val showCopyDatePicker: Boolean = false,
    val copySourceDate: LocalDate? = null,
    val copySourceDiary: FoodDiaryResponse? = null,
    val isCopyLoading: Boolean = false,
    val showCopySheet: Boolean = false,
    val selectedCopyEntryIds: Set<Int> = emptySet(),
    val copyTargetMeal: Int? = null, // null = keep original
    val isCopying: Boolean = false,
    val copyError: String? = null,
    // Edit entry flow
    val entryToEdit: FoodDiaryEntryDto? = null,
    val isSaving: Boolean = false
) {
    val isToday: Boolean get() = selectedDate == LocalDate.now()
    val canNavigateForward: Boolean get() = selectedDate < LocalDate.now()

    val dateLabel: String get() {
        val today = LocalDate.now()
        return when (selectedDate) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        }
    }
}

@HiltViewModel
class FoodDiaryViewModel @Inject constructor(
    val repository: FoodDiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodDiaryUiState())
    val uiState: StateFlow<FoodDiaryUiState> = _uiState.asStateFlow()

    init {
        loadDiary()
    }

    fun loadDiary() {
        val date = _uiState.value.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val diary = repository.getDiary(date)
                _uiState.update { it.copy(diary = diary, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load diary: ${e.message}", isLoading = false) }
            }
        }
    }

    fun navigateDate(delta: Int) {
        _uiState.update { state ->
            val newDate = state.selectedDate.plusDays(delta.toLong())
            if (newDate > LocalDate.now()) return@update state
            state.copy(selectedDate = newDate, diary = null)
        }
        loadDiary()
    }

    // Delete flow
    fun requestDelete(entry: FoodDiaryEntryDto) {
        _uiState.update { it.copy(entryToDelete = entry) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(entryToDelete = null) }
    }

    fun confirmDelete() {
        val entry = _uiState.value.entryToDelete ?: return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                repository.deleteEntry(entry.id)
                _uiState.update { it.copy(entryToDelete = null, isDeleting = false) }
                loadDiary()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Failed to delete: ${e.message}",
                    entryToDelete = null,
                    isDeleting = false
                ) }
            }
        }
    }

    // Copy flow
    fun showCopyDatePicker() {
        _uiState.update { it.copy(showCopyDatePicker = true, copyError = null) }
    }

    fun dismissCopyDatePicker() {
        _uiState.update { it.copy(showCopyDatePicker = false) }
    }

    fun selectCopySourceDate(dateMillis: Long) {
        val date = java.time.Instant.ofEpochMilli(dateMillis)
            .atZone(java.time.ZoneId.of("UTC"))
            .toLocalDate()
        _uiState.update { it.copy(
            showCopyDatePicker = false,
            copySourceDate = date,
            isCopyLoading = true,
            copyError = null
        ) }
        viewModelScope.launch {
            try {
                val diary = repository.getDiary(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                _uiState.update { it.copy(
                    copySourceDiary = diary,
                    showCopySheet = true,
                    selectedCopyEntryIds = emptySet(),
                    isCopyLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    copyError = "Failed to load source date: ${e.message}",
                    isCopyLoading = false
                ) }
            }
        }
    }

    fun toggleCopyEntry(entryId: Int) {
        _uiState.update { state ->
            val ids = state.selectedCopyEntryIds.toMutableSet()
            if (entryId in ids) ids.remove(entryId) else ids.add(entryId)
            state.copy(selectedCopyEntryIds = ids)
        }
    }

    fun toggleSelectAllCopyEntries() {
        _uiState.update { state ->
            val allIds = state.copySourceDiary?.meals
                ?.flatMap { m -> m.entries.map { it.id } }?.toSet() ?: emptySet()
            val newSelection = if (state.selectedCopyEntryIds == allIds) emptySet() else allIds
            state.copy(selectedCopyEntryIds = newSelection)
        }
    }

    fun setCopyTargetMeal(meal: Int?) {
        _uiState.update { it.copy(copyTargetMeal = meal) }
    }

    fun dismissCopySheet() {
        _uiState.update { it.copy(
            showCopySheet = false,
            copySourceDiary = null,
            selectedCopyEntryIds = emptySet(),
            copySourceDate = null,
            copyTargetMeal = null
        ) }
    }

    fun executeCopy() {
        val state = _uiState.value
        val sourceDiary = state.copySourceDiary ?: return
        val targetDate = state.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val selectedIds = state.selectedCopyEntryIds

        val entries = sourceDiary.meals
            .flatMap { it.entries }
            .filter { it.id in selectedIds }

        if (entries.isEmpty()) return

        _uiState.update { it.copy(isCopying = true, copyError = null) }
        viewModelScope.launch {
            try {
                val targetMeal = state.copyTargetMeal
                for (entry in entries) {
                    val mealCategoryInt = targetMeal ?: when (entry.mealCategory) {
                        "Breakfast" -> 0
                        "Lunch" -> 1
                        "Dinner" -> 2
                        else -> 3 // Snack
                    }
                    repository.createEntry(
                        CreateFoodDiaryEntryRequest(
                            foodProductId = null,
                            calendarDate = targetDate,
                            mealCategory = mealCategoryInt,
                            quantity = entry.quantity,
                            productName = entry.productName,
                            barcode = entry.barcode,
                            servingSize = entry.servingSize,
                            servingUnit = entry.servingUnit,
                            caloriesPerServing = entry.caloriesPerServing,
                            proteinPerServing = entry.proteinPerServing,
                            carbsPerServing = entry.carbsPerServing,
                            fatPerServing = entry.fatPerServing,
                            fibrePerServing = entry.fibrePerServing,
                            saltPerServing = entry.saltPerServing,
                            entrySource = when (entry.entrySource) {
                                "BarcodeScan" -> 0
                                "ManualSearch" -> 1
                                else -> 1
                            }
                        )
                    )
                }
                _uiState.update { it.copy(
                    isCopying = false,
                    showCopySheet = false,
                    copySourceDiary = null,
                    selectedCopyEntryIds = emptySet(),
                    copySourceDate = null,
                    copyTargetMeal = null
                ) }
                loadDiary()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    copyError = "Failed to copy entries: ${e.message}",
                    isCopying = false
                ) }
            }
        }
    }

    // Edit entry flow
    fun requestEdit(entry: FoodDiaryEntryDto) {
        _uiState.update { it.copy(entryToEdit = entry) }
    }

    fun dismissEdit() {
        _uiState.update { it.copy(entryToEdit = null) }
    }

    fun saveEdit(updates: Map<String, Any>) {
        val entry = _uiState.value.entryToEdit ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                repository.updateEntry(entry.id, updates)
                _uiState.update { it.copy(entryToEdit = null, isSaving = false) }
                loadDiary()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Failed to save: ${e.message}",
                    entryToEdit = null,
                    isSaving = false
                ) }
            }
        }
    }
}

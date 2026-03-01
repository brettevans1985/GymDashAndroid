package com.gymdash.companion.ui.fooddiary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.local.datastore.WaterPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterTrackerUiState(
    val currentMl: Int = 0,
    val goalMl: Int = 2000
) {
    val glasses: Int get() = currentMl / GLASS_ML
    val progress: Float get() = if (goalMl > 0) (currentMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f

    companion object {
        const val GLASS_ML = 250
    }
}

@HiltViewModel
class WaterTrackerViewModel @Inject constructor(
    private val waterPreferences: WaterPreferences
) : ViewModel() {

    val uiState: StateFlow<WaterTrackerUiState> = combine(
        waterPreferences.waterMl,
        waterPreferences.waterGoalMl
    ) { ml, goal ->
        WaterTrackerUiState(currentMl = ml, goalMl = goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaterTrackerUiState())

    fun addGlass() {
        viewModelScope.launch {
            waterPreferences.addWater(WaterTrackerUiState.GLASS_ML)
        }
    }

    fun removeGlass() {
        viewModelScope.launch {
            waterPreferences.removeWater(WaterTrackerUiState.GLASS_ML)
        }
    }
}

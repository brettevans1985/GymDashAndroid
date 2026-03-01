package com.gymdash.companion.ui.fooddiary

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.local.datastore.WaterPreferences
import com.gymdash.companion.data.remote.api.GymDashApi
import com.gymdash.companion.data.remote.dto.HealthSyncRequest
import com.gymdash.companion.data.remote.dto.WaterIntakeSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WaterTrackerUiState(
    val currentMl: Int = 0,
    val goalMl: Int = 2000,
    val isSyncing: Boolean = false,
    val syncError: String? = null
) {
    val glasses: Int get() = currentMl / GLASS_ML
    val progress: Float get() = if (goalMl > 0) (currentMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f

    companion object {
        const val GLASS_ML = 250
    }
}

@HiltViewModel
class WaterTrackerViewModel @Inject constructor(
    private val waterPreferences: WaterPreferences,
    private val gymDashApi: GymDashApi,
    private val syncPreferences: SyncPreferences
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
            syncWater()
        }
    }

    fun removeGlass() {
        viewModelScope.launch {
            waterPreferences.removeWater(WaterTrackerUiState.GLASS_ML)
            syncWater()
        }
    }

    private suspend fun syncWater() {
        val token = syncPreferences.authToken.first() ?: return
        val waterMl = waterPreferences.waterMl.first()

        try {
            gymDashApi.syncHealthData(
                token,
                HealthSyncRequest(
                    deviceName = Build.MODEL,
                    waterIntakes = listOf(
                        WaterIntakeSync(
                            calendarDate = LocalDate.now().toString(),
                            amountMl = waterMl
                        )
                    )
                )
            )
        } catch (_: Exception) {
            // Silently fail — local state is saved, will sync on next health sync
        }
    }
}

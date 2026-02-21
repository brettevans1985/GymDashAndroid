package com.gymdash.companion.presentation.home

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.repository.HeartRateResult
import com.gymdash.companion.domain.repository.HealthRepository
import com.gymdash.companion.domain.usecase.SyncHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val lastSyncResult: String? = null,
    val isError: Boolean = false,
    val hasHealthPermissions: Boolean = false,
    val needsPermissionRequest: Boolean = false,
    val isLiveHeartRateActive: Boolean = false,
    val currentHeartRate: Int? = null,
    val heartRateStatus: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val syncHealthDataUseCase: SyncHealthDataUseCase,
    private val healthRepository: HealthRepository,
    private val preferences: SyncPreferences,
    private val healthConnectClient: HealthConnectClient
) : ViewModel() {

    companion object {
        private const val HEART_RATE_POLL_INTERVAL_MS = 10_000L

        val HEALTH_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        )
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    private var heartRateJob: Job? = null

    init {
        viewModelScope.launch {
            checkPermissions()
        }
        viewModelScope.launch {
            preferences.lastSyncTimestamp.collect { timestamp ->
                if (timestamp > 0) {
                    val formatted = Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .format(formatter)
                    _uiState.value = _uiState.value.copy(lastSyncTime = formatted)
                }
            }
        }
    }

    private suspend fun checkPermissions() {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        val hasAll = granted.containsAll(HEALTH_PERMISSIONS)
        _uiState.value = _uiState.value.copy(hasHealthPermissions = hasAll)
    }

    fun onPermissionsResult() {
        viewModelScope.launch {
            checkPermissions()
            _uiState.value = _uiState.value.copy(needsPermissionRequest = false)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            if (!_uiState.value.hasHealthPermissions) {
                _uiState.value = _uiState.value.copy(needsPermissionRequest = true)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSyncing = true, isError = false)
            when (val result = syncHealthDataUseCase()) {
                is SyncResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncResult = "${result.recordsCreated} created, ${result.recordsUpdated} updated"
                    )
                }
                is SyncResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncResult = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    fun toggleLiveHeartRate() {
        if (_uiState.value.isLiveHeartRateActive) {
            stopHeartRatePolling()
        } else {
            if (!_uiState.value.hasHealthPermissions) {
                _uiState.value = _uiState.value.copy(needsPermissionRequest = true)
                return
            }
            startHeartRatePolling()
        }
    }

    private fun startHeartRatePolling() {
        _uiState.value = _uiState.value.copy(
            isLiveHeartRateActive = true,
            heartRateStatus = "Starting..."
        )
        heartRateJob = viewModelScope.launch {
            while (isActive) {
                when (val result = healthRepository.sendLatestHeartRate()) {
                    is HeartRateResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            currentHeartRate = result.bpm,
                            heartRateStatus = "Live"
                        )
                    }
                    is HeartRateResult.NoData -> {
                        _uiState.value = _uiState.value.copy(
                            heartRateStatus = "Waiting for data..."
                        )
                    }
                    is HeartRateResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            heartRateStatus = result.message
                        )
                    }
                }
                delay(HEART_RATE_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopHeartRatePolling() {
        heartRateJob?.cancel()
        heartRateJob = null
        _uiState.value = _uiState.value.copy(
            isLiveHeartRateActive = false,
            currentHeartRate = null,
            heartRateStatus = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        heartRateJob?.cancel()
    }
}

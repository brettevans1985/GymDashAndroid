package com.gymdash.companion.presentation.home

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.mapper.MappedHealthData
import com.gymdash.companion.domain.model.HealthMetric
import com.gymdash.companion.domain.model.SyncErrorType
import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.repository.HeartRateResult
import com.gymdash.companion.domain.repository.HealthRepository
import com.gymdash.companion.domain.repository.ReadResult
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

sealed class SyncPreviewState {
    data object Hidden : SyncPreviewState()
    data object Loading : SyncPreviewState()
    data class Ready(
        val data: MappedHealthData,
        val metricCounts: Map<HealthMetric, Int>,
        val selectedMetrics: Set<HealthMetric>
    ) : SyncPreviewState() {
        val selectedCount: Int get() = selectedMetrics.sumOf { metricCounts[it] ?: 0 }
    }
    data object Sending : SyncPreviewState()
}

data class HomeUiState(
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val lastSyncResult: String? = null,
    val isError: Boolean = false,
    val hasHealthPermissions: Boolean = false,
    val needsPermissionRequest: Boolean = false,
    val isLiveHeartRateActive: Boolean = false,
    val currentHeartRate: Int? = null,
    val heartRateStatus: String? = null,
    val isHealthConnectAvailable: Boolean = true,
    val syncPreview: SyncPreviewState = SyncPreviewState.Hidden
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val preferences: SyncPreferences,
    private val healthConnectClient: HealthConnectClient?
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
            HealthPermission.getReadPermission(FloorsClimbedRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(RespiratoryRateRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
            HealthPermission.getReadPermission(Vo2MaxRecord::class),
            HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        )
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    private var heartRateJob: Job? = null

    init {
        val isAvailable = healthConnectClient != null
        _uiState.value = _uiState.value.copy(isHealthConnectAvailable = isAvailable)

        if (isAvailable) {
            viewModelScope.launch {
                checkPermissions()
            }
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
        val client = healthConnectClient ?: return
        val granted = client.permissionController.getGrantedPermissions()
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
            if (!_uiState.value.isHealthConnectAvailable) return@launch

            if (!_uiState.value.hasHealthPermissions) {
                _uiState.value = _uiState.value.copy(needsPermissionRequest = true)
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                syncPreview = SyncPreviewState.Loading,
                isError = false
            )

            when (val result = healthRepository.readHealthData()) {
                is ReadResult.Success -> {
                    val data = result.data
                    val metricCounts = HealthMetric.entries.associateWith { it.countIn(data) }
                    val nonEmpty = metricCounts.filter { it.value > 0 }.keys
                    _uiState.value = _uiState.value.copy(
                        syncPreview = SyncPreviewState.Ready(
                            data = data,
                            metricCounts = metricCounts,
                            selectedMetrics = nonEmpty
                        )
                    )
                }
                is ReadResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        syncPreview = SyncPreviewState.Hidden,
                        lastSyncResult = userFriendlyMessage(result.type, result.message),
                        isError = true
                    )
                }
            }
        }
    }

    fun toggleMetric(metric: HealthMetric) {
        val preview = _uiState.value.syncPreview
        if (preview !is SyncPreviewState.Ready) return
        val count = preview.metricCounts[metric] ?: 0
        if (count == 0) return

        val updated = if (metric in preview.selectedMetrics) {
            preview.selectedMetrics - metric
        } else {
            preview.selectedMetrics + metric
        }
        _uiState.value = _uiState.value.copy(
            syncPreview = preview.copy(selectedMetrics = updated)
        )
    }

    fun toggleAllMetrics() {
        val preview = _uiState.value.syncPreview
        if (preview !is SyncPreviewState.Ready) return
        val nonEmpty = preview.metricCounts.filter { it.value > 0 }.keys
        val allSelected = nonEmpty == preview.selectedMetrics
        _uiState.value = _uiState.value.copy(
            syncPreview = preview.copy(
                selectedMetrics = if (allSelected) emptySet() else nonEmpty
            )
        )
    }

    fun confirmSync() {
        val preview = _uiState.value.syncPreview
        if (preview !is SyncPreviewState.Ready) return
        if (preview.selectedMetrics.isEmpty()) return

        val filteredData = preview.data.filterByMetrics(preview.selectedMetrics)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncPreview = SyncPreviewState.Sending)

            when (val result = healthRepository.sendHealthData(filteredData)) {
                is SyncResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        syncPreview = SyncPreviewState.Hidden,
                        lastSyncResult = "${result.recordsCreated} created, ${result.recordsUpdated} updated"
                    )
                }
                is SyncResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        syncPreview = SyncPreviewState.Hidden,
                        lastSyncResult = userFriendlyMessage(result.type, result.message),
                        isError = true
                    )
                }
            }
        }
    }

    fun dismissSyncPreview() {
        _uiState.value = _uiState.value.copy(syncPreview = SyncPreviewState.Hidden)
    }

    private fun userFriendlyMessage(type: SyncErrorType, fallback: String): String {
        return when (type) {
            SyncErrorType.NETWORK -> "No network connection. Please check your internet and try again."
            SyncErrorType.AUTH_EXPIRED -> "Session expired. Please log in again."
            SyncErrorType.SERVER -> "Server error. Please try again later."
            SyncErrorType.HEALTH_CONNECT -> "Health Connect permission denied. Please grant permissions."
            SyncErrorType.UNKNOWN -> fallback
        }
    }

    fun toggleLiveHeartRate() {
        if (_uiState.value.isLiveHeartRateActive) {
            stopHeartRatePolling()
        } else {
            if (!_uiState.value.isHealthConnectAvailable) return
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

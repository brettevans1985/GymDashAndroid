package com.gymdash.companion.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.usecase.SyncHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val lastSyncResult: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val syncHealthDataUseCase: SyncHealthDataUseCase,
    private val preferences: SyncPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

    init {
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

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, isError = false)
            when (val result = syncHealthDataUseCase()) {
                is SyncResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncResult = "${result.accepted} records synced"
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
}

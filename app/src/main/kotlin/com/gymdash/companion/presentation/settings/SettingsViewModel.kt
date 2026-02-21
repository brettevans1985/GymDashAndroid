package com.gymdash.companion.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.domain.repository.AuthRepository
import com.gymdash.companion.worker.HealthSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "",
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Long = 60
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: SyncPreferences,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val autoSync = preferences.autoSyncEnabled.first()
            val interval = preferences.syncIntervalMinutes.first()
            _uiState.value = SettingsUiState(
                serverUrl = preferences.serverUrl.first(),
                autoSyncEnabled = autoSync,
                syncIntervalMinutes = interval
            )
            if (autoSync) {
                HealthSyncWorker.enqueue(context, interval)
            }
        }
    }

    fun onServerUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        viewModelScope.launch { preferences.setServerUrl(url) }
    }

    fun onAutoSyncChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
        viewModelScope.launch {
            preferences.setAutoSyncEnabled(enabled)
            if (enabled) {
                HealthSyncWorker.enqueue(context, _uiState.value.syncIntervalMinutes)
            } else {
                HealthSyncWorker.cancel(context)
            }
        }
    }

    fun onSyncIntervalChanged(minutes: Long) {
        _uiState.value = _uiState.value.copy(syncIntervalMinutes = minutes)
        viewModelScope.launch {
            preferences.setSyncIntervalMinutes(minutes)
            if (_uiState.value.autoSyncEnabled) {
                HealthSyncWorker.enqueue(context, minutes)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            HealthSyncWorker.cancel(context)
            authRepository.logout()
            onComplete()
        }
    }
}

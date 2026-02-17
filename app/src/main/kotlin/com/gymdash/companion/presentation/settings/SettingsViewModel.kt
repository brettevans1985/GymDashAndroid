package com.gymdash.companion.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(
                serverUrl = preferences.serverUrl.first(),
                autoSyncEnabled = preferences.autoSyncEnabled.first(),
                syncIntervalMinutes = preferences.syncIntervalMinutes.first()
            )
        }
    }

    fun onServerUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        viewModelScope.launch { preferences.setServerUrl(url) }
    }

    fun onAutoSyncChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
        viewModelScope.launch { preferences.setAutoSyncEnabled(enabled) }
    }

    fun onSyncIntervalChanged(minutes: Long) {
        _uiState.value = _uiState.value.copy(syncIntervalMinutes = minutes)
        viewModelScope.launch { preferences.setSyncIntervalMinutes(minutes) }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}

package com.gymdash.companion.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.BuildConfig
import com.gymdash.companion.data.remote.BaseUrlInterceptor
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ServerEnvironment(val label: String, val url: String) {
    DEVELOPMENT("Development", BuildConfig.DEFAULT_SERVER_URL),
    PRODUCTION("Production", BuildConfig.PRODUCTION_SERVER_URL);
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val selectedServer: ServerEnvironment =
        if (BuildConfig.DEBUG) ServerEnvironment.DEVELOPMENT else ServerEnvironment.PRODUCTION
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val baseUrlInterceptor: BaseUrlInterceptor,
    private val preferences: SyncPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun onServerChanged(server: ServerEnvironment) {
        _uiState.value = _uiState.value.copy(selectedServer = server, error = null)
        baseUrlInterceptor.baseUrl = server.url
    }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Username and password are required")
            return
        }

        baseUrlInterceptor.baseUrl = state.selectedServer.url

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            loginUseCase(state.username, state.password)
                .onSuccess {
                    preferences.setServerUrl(state.selectedServer.url)
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Login failed"
                    )
                }
        }
    }
}

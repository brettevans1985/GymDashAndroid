package com.gymdash.companion.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymdash.companion.data.remote.dto.DefaultThemeDto
import com.gymdash.companion.data.remote.dto.CustomThemeDto
import com.gymdash.companion.data.remote.dto.ThemeColorsDto
import com.gymdash.companion.data.remote.dto.ThemePreferenceDto
import com.gymdash.companion.domain.repository.ThemeRepository
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeSettingsUiState(
    val defaultThemes: List<DefaultThemeDto> = emptyList(),
    val customThemes: List<CustomThemeDto> = emptyList(),
    val activePreference: ThemePreferenceDto = ThemePreferenceDto("default", "midnight"),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val moshi: Moshi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    private val colorsAdapter = moshi.adapter(ThemeColorsDto::class.java)

    init {
        loadThemes()
    }

    fun loadThemes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = themeRepository.getThemes()
                _uiState.value = ThemeSettingsUiState(
                    defaultThemes = response.defaultThemes,
                    customThemes = response.customThemes,
                    activePreference = response.activeTheme,
                    isLoading = false
                )
                // Cache the active theme colors locally
                val colorsJson = resolveColorsJson(response.activeTheme, response)
                themeRepository.cachePreference(
                    response.activeTheme.themeType,
                    response.activeTheme.themeIdentifier,
                    colorsJson
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load themes"
                )
            }
        }
    }

    fun selectTheme(type: String, identifier: String) {
        val preference = ThemePreferenceDto(type, identifier)
        _uiState.value = _uiState.value.copy(activePreference = preference)

        viewModelScope.launch {
            try {
                val result = themeRepository.setPreference(preference)
                _uiState.value = _uiState.value.copy(activePreference = result)

                val state = _uiState.value
                val colorsJson = when (result.themeType) {
                    "default" -> {
                        val theme = state.defaultThemes.find { it.key == result.themeIdentifier }
                        theme?.let { colorsAdapter.toJson(it.colors) }
                    }
                    "custom" -> {
                        val theme = state.customThemes.find { it.name == result.themeIdentifier }
                        theme?.let { colorsAdapter.toJson(it.colors) }
                    }
                    else -> null
                }
                themeRepository.cachePreference(result.themeType, result.themeIdentifier, colorsJson)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun getColorsForTheme(type: String, identifier: String): ThemeColorsDto? {
        val state = _uiState.value
        return when (type) {
            "default" -> state.defaultThemes.find { it.key == identifier }?.colors
            "custom" -> state.customThemes.find { it.name == identifier }?.colors
            else -> null
        }
    }

    private fun resolveColorsJson(pref: ThemePreferenceDto, response: com.gymdash.companion.data.remote.dto.ThemeListResponseDto): String? {
        return when (pref.themeType) {
            "default" -> {
                val theme = response.defaultThemes.find { it.key == pref.themeIdentifier }
                theme?.let { colorsAdapter.toJson(it.colors) }
            }
            "custom" -> {
                val theme = response.customThemes.find { it.name == pref.themeIdentifier }
                theme?.let { colorsAdapter.toJson(it.colors) }
            }
            else -> null
        }
    }
}

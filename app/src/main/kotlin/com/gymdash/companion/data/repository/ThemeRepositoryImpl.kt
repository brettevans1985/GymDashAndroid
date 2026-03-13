package com.gymdash.companion.data.repository

import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.local.datastore.ThemePreferences
import com.gymdash.companion.data.remote.api.ThemeApi
import com.gymdash.companion.data.remote.dto.ThemeListResponseDto
import com.gymdash.companion.data.remote.dto.ThemePreferenceDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val themeApi: ThemeApi,
    private val syncPreferences: SyncPreferences,
    private val themePreferences: ThemePreferences
) : com.gymdash.companion.domain.repository.ThemeRepository {

    override suspend fun getThemes(): ThemeListResponseDto {
        val token = syncPreferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")
        return themeApi.getThemes(token)
    }

    override suspend fun setPreference(preference: ThemePreferenceDto): ThemePreferenceDto {
        val token = syncPreferences.authToken.first()
            ?: throw IllegalStateException("Not authenticated")
        val result = themeApi.setPreference(token, preference)
        themePreferences.setPreference(result.themeType, result.themeIdentifier)
        return result
    }

    override suspend fun getCachedPreference(): ThemePreferenceDto {
        return ThemePreferenceDto(
            themeType = themePreferences.themeType.first(),
            themeIdentifier = themePreferences.themeIdentifier.first()
        )
    }

    override suspend fun getCachedColorsJson(): String? {
        return themePreferences.themeColorsJson.first()
    }

    override suspend fun cachePreference(type: String, identifier: String, colorsJson: String?) {
        themePreferences.setPreference(type, identifier)
        themePreferences.setThemeColorsJson(colorsJson)
    }
}

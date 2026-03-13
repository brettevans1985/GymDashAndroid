package com.gymdash.companion.domain.repository

import com.gymdash.companion.data.remote.dto.ThemeListResponseDto
import com.gymdash.companion.data.remote.dto.ThemePreferenceDto

interface ThemeRepository {
    suspend fun getThemes(): ThemeListResponseDto
    suspend fun setPreference(preference: ThemePreferenceDto): ThemePreferenceDto
    suspend fun getCachedPreference(): ThemePreferenceDto
    suspend fun getCachedColorsJson(): String?
    suspend fun cachePreference(type: String, identifier: String, colorsJson: String?)
}

package com.gymdash.companion.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ThemeColorsDto(
    val bgPrimary: String,
    val bgSecondary: String,
    val bgCard: String,
    val accent: String,
    val accentLight: String,
    val textPrimary: String,
    val textSecondary: String,
    val textMuted: String,
    val success: String,
    val warning: String,
    val danger: String,
    val border: String
)

@JsonClass(generateAdapter = true)
data class DefaultThemeDto(
    val key: String,
    val name: String,
    val type: String,
    val colors: ThemeColorsDto
)

@JsonClass(generateAdapter = true)
data class CustomThemeDto(
    val id: Int,
    val name: String,
    val colors: ThemeColorsDto,
    val createdAt: String,
    val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class ThemePreferenceDto(
    val themeType: String,
    val themeIdentifier: String
)

@JsonClass(generateAdapter = true)
data class ThemeListResponseDto(
    val defaultThemes: List<DefaultThemeDto>,
    val customThemes: List<CustomThemeDto>,
    val activeTheme: ThemePreferenceDto
)

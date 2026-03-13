package com.gymdash.companion.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.gymdash.companion.data.remote.dto.ThemeColorsDto

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    return Color(android.graphics.Color.parseColor("#$cleaned"))
}

fun themeColorsToColorScheme(colors: ThemeColorsDto): ColorScheme {
    val bg = parseHexColor(colors.bgPrimary)
    val bgCard = parseHexColor(colors.bgCard)
    val accent = parseHexColor(colors.accent)
    val accentLight = parseHexColor(colors.accentLight)
    val textPrimary = parseHexColor(colors.textPrimary)
    val textSecondary = parseHexColor(colors.textSecondary)
    val danger = parseHexColor(colors.danger)
    val bgSecondary = parseHexColor(colors.bgSecondary)

    // Determine if the theme is dark based on background luminance
    val isDark = isColorDark(colors.bgPrimary)

    return if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = textPrimary,
            primaryContainer = accentLight,
            onPrimaryContainer = textPrimary,
            secondary = textSecondary,
            onSecondary = bg,
            background = bg,
            onBackground = textPrimary,
            surface = bgCard,
            onSurface = textPrimary,
            surfaceVariant = bgSecondary,
            onSurfaceVariant = textSecondary,
            error = danger,
            onError = textPrimary,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = bg,
            primaryContainer = accentLight,
            onPrimaryContainer = textPrimary,
            secondary = textSecondary,
            onSecondary = bg,
            background = bg,
            onBackground = textPrimary,
            surface = bgCard,
            onSurface = textPrimary,
            surfaceVariant = bgSecondary,
            onSurfaceVariant = textSecondary,
            error = danger,
            onError = bg,
        )
    }
}

private fun isColorDark(hex: String): Boolean {
    val cleaned = hex.removePrefix("#")
    val r = Integer.parseInt(cleaned.substring(0, 2), 16) / 255.0
    val g = Integer.parseInt(cleaned.substring(2, 4), 16) / 255.0
    val b = Integer.parseInt(cleaned.substring(4, 6), 16) / 255.0
    val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    return luminance < 0.5
}

@Composable
fun GymDashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    overrideColorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = overrideColorScheme ?: when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

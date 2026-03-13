package com.gymdash.companion.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val THEME_TYPE = stringPreferencesKey("theme_type")
        val THEME_IDENTIFIER = stringPreferencesKey("theme_identifier")
        val THEME_COLORS_JSON = stringPreferencesKey("theme_colors_json")
    }

    val themeType: Flow<String> = dataStore.data.map { it[THEME_TYPE] ?: "default" }
    val themeIdentifier: Flow<String> = dataStore.data.map { it[THEME_IDENTIFIER] ?: "midnight" }
    val themeColorsJson: Flow<String?> = dataStore.data.map { it[THEME_COLORS_JSON] }

    suspend fun setPreference(type: String, identifier: String) {
        dataStore.edit {
            it[THEME_TYPE] = type
            it[THEME_IDENTIFIER] = identifier
        }
    }

    suspend fun setThemeColorsJson(json: String?) {
        dataStore.edit { prefs ->
            if (json != null) prefs[THEME_COLORS_JSON] = json
            else prefs.remove(THEME_COLORS_JSON)
        }
    }
}

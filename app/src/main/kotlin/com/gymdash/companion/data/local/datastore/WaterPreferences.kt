package com.gymdash.companion.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaterPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val WATER_DATE = stringPreferencesKey("water_date")
        val WATER_ML = intPreferencesKey("water_ml")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
    }

    private fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val waterMl: Flow<Int> = dataStore.data.map { prefs ->
        val storedDate = prefs[WATER_DATE]
        if (storedDate == today()) prefs[WATER_ML] ?: 0 else 0
    }

    val waterGoalMl: Flow<Int> = dataStore.data.map { prefs ->
        prefs[WATER_GOAL_ML] ?: 2000
    }

    suspend fun addWater(ml: Int) {
        dataStore.edit { prefs ->
            val storedDate = prefs[WATER_DATE]
            val currentMl = if (storedDate == today()) prefs[WATER_ML] ?: 0 else 0
            val newMl = (currentMl + ml).coerceAtLeast(0)
            prefs[WATER_DATE] = today()
            prefs[WATER_ML] = newMl
        }
    }

    suspend fun removeWater(ml: Int) {
        dataStore.edit { prefs ->
            val storedDate = prefs[WATER_DATE]
            val currentMl = if (storedDate == today()) prefs[WATER_ML] ?: 0 else 0
            val newMl = (currentMl - ml).coerceAtLeast(0)
            prefs[WATER_DATE] = today()
            prefs[WATER_ML] = newMl
        }
    }

    suspend fun setGoal(ml: Int) {
        dataStore.edit { prefs ->
            prefs[WATER_GOAL_ML] = ml
        }
    }
}

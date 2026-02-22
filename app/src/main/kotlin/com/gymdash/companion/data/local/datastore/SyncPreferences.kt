package com.gymdash.companion.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val SYNC_INTERVAL_MINUTES = longPreferencesKey("sync_interval_minutes")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_CHANGES_TOKEN = stringPreferencesKey("last_changes_token")
        val USER_ID = stringPreferencesKey("user_id")
        val LAST_USERNAME = stringPreferencesKey("last_username")
        val LAST_DEV_URL = stringPreferencesKey("last_dev_url")
    }

    val authToken: Flow<String?> = dataStore.data.map { it[AUTH_TOKEN] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[REFRESH_TOKEN] }
    val lastUsername: Flow<String?> = dataStore.data.map { it[LAST_USERNAME] }
    val lastDevUrl: Flow<String?> = dataStore.data.map { it[LAST_DEV_URL] }
    val serverUrl: Flow<String> = dataStore.data.map { it[SERVER_URL] ?: com.gymdash.companion.BuildConfig.DEFAULT_SERVER_URL }
    val autoSyncEnabled: Flow<Boolean> = dataStore.data.map { it[AUTO_SYNC_ENABLED] ?: true }
    val syncIntervalMinutes: Flow<Long> = dataStore.data.map { it[SYNC_INTERVAL_MINUTES] ?: 60L }
    val lastSyncTimestamp: Flow<Long> = dataStore.data.map { it[LAST_SYNC_TIMESTAMP] ?: 0L }
    val lastChangesToken: Flow<String?> = dataStore.data.map { it[LAST_CHANGES_TOKEN] }

    suspend fun setAuthToken(token: String?) {
        dataStore.edit { prefs ->
            if (token != null) prefs[AUTH_TOKEN] = token
            else prefs.remove(AUTH_TOKEN)
        }
    }

    suspend fun setRefreshToken(token: String?) {
        dataStore.edit { prefs ->
            if (token != null) prefs[REFRESH_TOKEN] = token
            else prefs.remove(REFRESH_TOKEN)
        }
    }

    suspend fun setServerUrl(url: String) {
        dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[AUTO_SYNC_ENABLED] = enabled }
    }

    suspend fun setSyncIntervalMinutes(minutes: Long) {
        dataStore.edit { it[SYNC_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { it[LAST_SYNC_TIMESTAMP] = timestamp }
    }

    suspend fun setLastChangesToken(token: String?) {
        dataStore.edit { prefs ->
            if (token != null) prefs[LAST_CHANGES_TOKEN] = token
            else prefs.remove(LAST_CHANGES_TOKEN)
        }
    }

    suspend fun setUserId(userId: String) {
        dataStore.edit { it[USER_ID] = userId }
    }

    suspend fun setLastUsername(username: String) {
        dataStore.edit { it[LAST_USERNAME] = username }
    }

    suspend fun setLastDevUrl(url: String) {
        dataStore.edit { it[LAST_DEV_URL] = url }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}

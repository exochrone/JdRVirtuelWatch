package com.jdrvirtuel.watcher.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val CONSECUTIVE_CHALLENGE_FAILURES = intPreferencesKey("consecutive_challenge_failures")
        private val LAST_CHALLENGE_PROMPT_AT = longPreferencesKey("last_challenge_prompt_at")
        private val PREFERRED_BROWSER_PACKAGE = stringPreferencesKey("preferred_browser_package")
        private val TEST_MODE_ENABLED = booleanPreferencesKey("test_mode_enabled")
        private val TEST_MODE_INTERVAL_MINUTES = intPreferencesKey("test_mode_interval_minutes")
        private val TEST_MODE_LOG = stringPreferencesKey("test_mode_log")
        private val SYNC_LOG = stringPreferencesKey("sync_log")
    }

    val consecutiveChallengeFailures: Flow<Int> = dataStore.data
        .map { preferences -> preferences[CONSECUTIVE_CHALLENGE_FAILURES] ?: 0 }

    suspend fun setConsecutiveChallengeFailures(value: Int) {
        dataStore.edit { preferences ->
            preferences[CONSECUTIVE_CHALLENGE_FAILURES] = value
        }
    }

    val lastChallengePromptAt: Flow<Long> = dataStore.data
        .map { preferences -> preferences[LAST_CHALLENGE_PROMPT_AT] ?: 0L }

    suspend fun setLastChallengePromptAt(value: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_CHALLENGE_PROMPT_AT] = value
        }
    }

    val preferredBrowserPackage: Flow<String?> = dataStore.data
        .map { preferences -> preferences[PREFERRED_BROWSER_PACKAGE] }

    suspend fun setPreferredBrowserPackage(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(PREFERRED_BROWSER_PACKAGE)
            } else {
                preferences[PREFERRED_BROWSER_PACKAGE] = value
            }
        }
    }

    val isTestModeEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[TEST_MODE_ENABLED] ?: false }

    suspend fun setTestModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TEST_MODE_ENABLED] = enabled
        }
    }

    val testModeIntervalMinutes: Flow<Int> = dataStore.data
        .map { preferences -> preferences[TEST_MODE_INTERVAL_MINUTES] ?: 2 }

    suspend fun setTestModeIntervalMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[TEST_MODE_INTERVAL_MINUTES] = minutes
        }
    }

    val testModeLog: Flow<String?> = dataStore.data
        .map { preferences -> preferences[TEST_MODE_LOG] }

    suspend fun setTestModeLog(log: String?) {
        dataStore.edit { preferences ->
            if (log == null) {
                preferences.remove(TEST_MODE_LOG)
            } else {
                preferences[TEST_MODE_LOG] = log
            }
        }
    }

    val syncLog: Flow<String?> = dataStore.data
        .map { preferences -> preferences[SYNC_LOG] }

    suspend fun setSyncLog(log: String?) {
        dataStore.edit { preferences ->
            if (log == null) {
                preferences.remove(SYNC_LOG)
            } else {
                preferences[SYNC_LOG] = log
            }
        }
    }
}

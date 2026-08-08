package com.jdrvirtuel.watcher.notification

import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.NotificationLogEntry
import com.jdrvirtuel.watcher.domain.model.NotificationType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationLog @Inject constructor(
    private val appPreferences: AppPreferences
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun addEntry(type: NotificationType, forumName: String? = null, topicTitle: String? = null) {
        val currentLog = appPreferences.notificationLog.first()
        val entries = if (currentLog != null) {
            try {
                json.decodeFromString<List<NotificationLogEntry>>(currentLog).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        entries.add(0, NotificationLogEntry(
            timestampMs = System.currentTimeMillis(),
            type = type,
            forumName = forumName,
            topicTitle = topicTitle
        ))

        // Keep only last 50 entries
        val limitedEntries = entries.take(50)
        appPreferences.setNotificationLog(json.encodeToString(limitedEntries))
    }

    suspend fun getEntries(): List<NotificationLogEntry> {
        val currentLog = appPreferences.notificationLog.first() ?: return emptyList()
        return try {
            json.decodeFromString<List<NotificationLogEntry>>(currentLog)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun clear() {
        appPreferences.setNotificationLog(null)
    }
}

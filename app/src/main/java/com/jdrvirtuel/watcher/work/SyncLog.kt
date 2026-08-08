package com.jdrvirtuel.watcher.work

import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.ForumSyncResult
import com.jdrvirtuel.watcher.domain.model.SyncLogEntry
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.SyncSource
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncLog @Inject constructor(
    private val appPreferences: AppPreferences
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun addEntry(source: SyncSource, outcomes: List<SyncOutcome>) {
        val currentLog = appPreferences.syncLog.first()
        val entries = if (currentLog != null) {
            try {
                json.decodeFromString<List<SyncLogEntry>>(currentLog).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val forumResults = outcomes.map { outcome ->
            val name = when (outcome.forumId) {
                15 -> "Oneshots"
                16 -> "Campagnes"
                else -> "Forum ${outcome.forumId}"
            }
            ForumSyncResult(
                forumName = name,
                status = outcome.status,
                newTopicsCount = outcome.newTopics.size + outcome.newReplies.size,
                insertedCount = outcome.insertedCount,
                updatedCount = outcome.updatedCount
            )
        }

        entries.add(0, SyncLogEntry(
            timestampMs = System.currentTimeMillis(),
            source = source,
            forumResults = forumResults
        ))

        // Keep only last 50 entries
        val limitedEntries = entries.take(50)
        appPreferences.setSyncLog(json.encodeToString(limitedEntries))
    }

    suspend fun getEntries(): List<SyncLogEntry> {
        val currentLog = appPreferences.syncLog.first() ?: return emptyList()
        return try {
            json.decodeFromString<List<SyncLogEntry>>(currentLog)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun clear() {
        appPreferences.setSyncLog(null)
    }
}

package com.jdrvirtuel.watcher.work

import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TestModeEntry(
    val timestamp: Long,
    val forum15Result: String,
    val forum16Result: String,
    val newTopicsCount: Int
) {
    fun format(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return "${sdf.format(Date(timestamp))} - F15: $forum15Result, F16: $forum16Result, New: $newTopicsCount"
    }
}

@Singleton
class TestModeLog @Inject constructor(
    private val appPreferences: AppPreferences
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun addEntry(outcomes: List<SyncOutcome>) {
        val currentLog = appPreferences.testModeLog.first()
        val entries = if (currentLog != null) {
            try {
                json.decodeFromString<List<TestModeEntry>>(currentLog).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val f15 = outcomes.find { it.forumId == 15 }
        val f16 = outcomes.find { it.forumId == 16 }
        
        val f15Res = f15?.status?.name ?: "UNKNOWN"
        val f16Res = f16?.status?.name ?: "UNKNOWN"
        val newCount = outcomes.sumOf { it.newTopics.size + it.newReplies.size }

        entries.add(0, TestModeEntry(System.currentTimeMillis(), f15Res, f16Res, newCount))
        
        // Keep only last 50 entries to avoid bloating DataStore
        val limitedEntries = entries.take(50)
        appPreferences.setTestModeLog(json.encodeToString(limitedEntries))
    }

    suspend fun clear() {
        appPreferences.setTestModeLog(null)
    }

    suspend fun getEntries(): List<TestModeEntry> {
        val currentLog = appPreferences.testModeLog.first() ?: return emptyList()
        return try {
            json.decodeFromString<List<TestModeEntry>>(currentLog)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

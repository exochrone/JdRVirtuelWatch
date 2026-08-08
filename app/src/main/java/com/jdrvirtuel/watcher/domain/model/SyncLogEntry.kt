package com.jdrvirtuel.watcher.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SyncSource {
    MANUAL,
    PERIODIC,
    TEST
}

@Serializable
data class ForumSyncResult(
    val forumName: String,
    val status: SyncStatus,
    val newTopicsCount: Int = 0,
    val newRepliesCount: Int = 0,
    val parsedCount: Int = 0,
    val errorMessage: String? = null
)

@Serializable
data class SyncLogEntry(
    val timestampMs: Long,
    val source: SyncSource,
    val forumResults: List<ForumSyncResult>
)

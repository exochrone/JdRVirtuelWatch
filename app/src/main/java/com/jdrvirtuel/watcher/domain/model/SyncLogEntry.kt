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
    val newTopicsCount: Int
)

@Serializable
data class SyncLogEntry(
    val timestampMs: Long,
    val source: SyncSource,
    val forumResults: List<ForumSyncResult>
)

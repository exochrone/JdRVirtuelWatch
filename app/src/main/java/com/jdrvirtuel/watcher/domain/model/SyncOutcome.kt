package com.jdrvirtuel.watcher.domain.model

import kotlinx.serialization.Serializable

data class SyncOutcome(
    val forumId: Int,
    val status: SyncStatus,
    val newTopics: List<Topic> = emptyList(),
    val newReplies: List<Topic> = emptyList(),
    val parsedCount: Int = 0,
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val purgedCount: Int = 0,
    val errorMessage: String? = null
)

@Serializable
enum class SyncStatus { SUCCESS, CHALLENGE_REQUIRED, ERROR }

package com.jdrvirtuel.watcher.domain.repository

import kotlinx.coroutines.flow.Flow

interface ChallengeStateRepository {
    val consecutiveFailures: Flow<Int>
    val lastPromptAt: Flow<Long>

    suspend fun incrementFailures()
    suspend fun resetFailures()
    suspend fun updateLastPromptAt(timestamp: Long)
}

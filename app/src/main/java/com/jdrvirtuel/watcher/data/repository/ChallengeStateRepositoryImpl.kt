package com.jdrvirtuel.watcher.data.repository

import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.repository.ChallengeStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChallengeStateRepositoryImpl @Inject constructor(
    private val appPreferences: AppPreferences
) : ChallengeStateRepository {

    override val consecutiveFailures: Flow<Int> = appPreferences.consecutiveChallengeFailures
    override val lastPromptAt: Flow<Long> = appPreferences.lastChallengePromptAt

    override suspend fun incrementFailures() {
        val current = appPreferences.consecutiveChallengeFailures.first()
        appPreferences.setConsecutiveChallengeFailures(current + 1)
    }

    override suspend fun resetFailures() {
        appPreferences.setConsecutiveChallengeFailures(0)
    }

    override suspend fun updateLastPromptAt(timestamp: Long) {
        appPreferences.setLastChallengePromptAt(timestamp)
    }
}

package com.jdrvirtuel.watcher.notification

import javax.inject.Inject
import javax.inject.Singleton

enum class StatusLevel { IDLE, NEW, ALERT }

enum class AlertReason { VERIFICATION, SYNC_FAILED, STALE }

data class ForumSyncState(
    val lastSyncAt: Long?,
    val lastSyncSuccess: Boolean,
    val lastSyncError: String?
)

data class StatusDecision(
    val level: StatusLevel,
    val reason: AlertReason?
)

@Singleton
class StatusLevelResolver @Inject constructor() {
    fun resolve(
        forums: List<ForumSyncState>,
        challengeFailures: Int,
        pendingNewCount: Int,
        now: Long
    ): StatusDecision {
        if (challengeFailures >= 1) {
            return StatusDecision(StatusLevel.ALERT, AlertReason.VERIFICATION)
        }

        if (forums.any { !it.lastSyncSuccess && it.lastSyncError != null }) {
            return StatusDecision(StatusLevel.ALERT, AlertReason.SYNC_FAILED)
        }

        val lastSuccess = forums.mapNotNull { it.lastSyncAt }.maxOrNull()
        if (lastSuccess != null && (now - lastSuccess) >= STALE_THRESHOLD_MS) {
            return StatusDecision(StatusLevel.ALERT, AlertReason.STALE)
        }

        if (pendingNewCount > 0) {
            return StatusDecision(StatusLevel.NEW, null)
        }

        return StatusDecision(StatusLevel.IDLE, null)
    }

    companion object {
        const val STALE_THRESHOLD_MS = 60 * 60 * 1000L
    }
}

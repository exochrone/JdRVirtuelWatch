package com.jdrvirtuel.watcher.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StatusLevelResolverTest {

    private lateinit var resolver: StatusLevelResolver
    private val now = 1_000_000_000_000L // arbitrary fixed timestamp in ms

    @Before
    fun setUp() {
        resolver = StatusLevelResolver()
    }

    @Test
    fun testNoSync_returnsIdle() {
        val forums = listOf(
            ForumSyncState(lastSyncAt = null, lastSyncSuccess = false, lastSyncError = null),
            ForumSyncState(lastSyncAt = null, lastSyncSuccess = false, lastSyncError = null)
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 0,
            now = now
        )
        assertEquals(StatusLevel.IDLE, decision.level)
        assertNull(decision.reason)
    }

    @Test
    fun testRecentSync_noPending_returnsIdle() {
        val tenMinutesAgo = now - (10 * 60 * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = tenMinutesAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = tenMinutesAgo, lastSyncSuccess = true, lastSyncError = null)
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 0,
            now = now
        )
        assertEquals(StatusLevel.IDLE, decision.level)
        assertNull(decision.reason)
    }

    @Test
    fun testRecentSync_withPending_returnsNew() {
        val tenMinutesAgo = now - (10 * 60 * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = tenMinutesAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = tenMinutesAgo, lastSyncSuccess = true, lastSyncError = null)
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 3,
            now = now
        )
        assertEquals(StatusLevel.NEW, decision.level)
        assertNull(decision.reason)
    }

    @Test
    fun testForumFailure_returnsAlertSyncFailed() {
        val tenMinutesAgo = now - (10 * 60 * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = tenMinutesAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = null, lastSyncSuccess = false, lastSyncError = "Erreur réseau")
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 0,
            now = now
        )
        assertEquals(StatusLevel.ALERT, decision.level)
        assertEquals(AlertReason.SYNC_FAILED, decision.reason)
    }

    @Test
    fun testVerificationRequired_returnsAlertVerification() {
        val tenMinutesAgo = now - (10 * 60 * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = tenMinutesAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = null, lastSyncSuccess = false, lastSyncError = "Vérification Cloudflare requise")
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 1,
            pendingNewCount = 0,
            now = now
        )
        assertEquals(StatusLevel.ALERT, decision.level)
        assertEquals(AlertReason.VERIFICATION, decision.reason)
    }

    @Test
    fun testStale_61MinutesAgo_returnsAlertStale() {
        val sixtyOneMinutesAgo = now - (61 * 60 * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = sixtyOneMinutesAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = sixtyOneMinutesAgo, lastSyncSuccess = true, lastSyncError = null)
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 0,
            now = now
        )
        assertEquals(StatusLevel.ALERT, decision.level)
        assertEquals(AlertReason.STALE, decision.reason)
    }

    @Test
    fun testStale_exactBoundary_returnsAlertStale() {
        val sixtyMinutesAgo = now - (60 * 60 * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = sixtyMinutesAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = sixtyMinutesAgo, lastSyncSuccess = true, lastSyncError = null)
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 0,
            now = now
        )
        assertEquals(StatusLevel.ALERT, decision.level)
        assertEquals(AlertReason.STALE, decision.reason)
    }

    @Test
    fun testStale_justBeforeBoundary_returnsIdle() {
        val fiftyNineMinutes59SecondsAgo = now - ((59 * 60 + 59) * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = fiftyNineMinutes59SecondsAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = fiftyNineMinutes59SecondsAgo, lastSyncSuccess = true, lastSyncError = null)
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 0,
            now = now
        )
        assertEquals(StatusLevel.IDLE, decision.level)
        assertNull(decision.reason)
    }

    @Test
    fun testStale_withPending_returnsAlertStale() {
        val sixtyOneMinutesAgo = now - (61 * 60 * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = sixtyOneMinutesAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = sixtyOneMinutesAgo, lastSyncSuccess = true, lastSyncError = null)
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 2,
            now = now
        )
        assertEquals(StatusLevel.ALERT, decision.level)
        assertEquals(AlertReason.STALE, decision.reason)
    }

    @Test
    fun testLastSyncRetained_maxLastSyncUsed() {
        val seventyMinutesAgo = now - (70 * 60 * 1000L)
        val fiveMinutesAgo = now - (5 * 60 * 1000L)
        val forums = listOf(
            ForumSyncState(lastSyncAt = seventyMinutesAgo, lastSyncSuccess = true, lastSyncError = null),
            ForumSyncState(lastSyncAt = fiveMinutesAgo, lastSyncSuccess = true, lastSyncError = null)
        )
        val decision = resolver.resolve(
            forums = forums,
            challengeFailures = 0,
            pendingNewCount = 0,
            now = now
        )
        assertEquals(StatusLevel.IDLE, decision.level)
        assertNull(decision.reason)
    }
}

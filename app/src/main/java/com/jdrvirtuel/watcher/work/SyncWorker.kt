package com.jdrvirtuel.watcher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.SyncSource
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.usecase.SyncAllForumsUseCase
import com.jdrvirtuel.watcher.domain.repository.ChallengeStateRepository
import com.jdrvirtuel.watcher.notification.AppNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import javax.inject.Provider

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncAllForums: SyncAllForumsUseCase,
    private val forumRepository: ForumRepository,
    private val appPreferences: AppPreferences,
    private val challengeRepository: ChallengeStateRepository,
    private val appNotifier: AppNotifier,
    private val testModeLog: TestModeLog,
    private val syncLog: SyncLog,
    private val syncSchedulerProvider: Provider<SyncScheduler>
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sourceStr = inputData.getString("sync_source")
        val source = try {
            if (sourceStr != null) SyncSource.valueOf(sourceStr) else SyncSource.PERIODIC
        } catch (e: Exception) {
            SyncSource.PERIODIC
        }

        return try {
            val outcomes = syncAllForums()
            
            // Permanent sync log
            syncLog.addEntry(source, outcomes)

            // Log for test mode if active AND source is TEST
            if (source == SyncSource.TEST && appPreferences.isTestModeEnabled.first()) {
                testModeLog.addEntry(outcomes)
                syncSchedulerProvider.get().scheduleNextTestRun()
            }

            val allFailedNet = outcomes.all { it.status == SyncStatus.ERROR }
            val anyChallenge = outcomes.any { it.status == SyncStatus.CHALLENGE_REQUIRED }
            val anySuccess = outcomes.any { it.status == SyncStatus.SUCCESS }

            if (anyChallenge) {
                handleChallenge()
            } else if (anySuccess) {
                syncSchedulerProvider.get().reschedulePeriodicSync(isLongPeriod = false)
            }

            when {
                anySuccess -> Result.success()
                anyChallenge -> Result.success()
                allFailedNet -> Result.retry()
                else -> Result.success()
            }
        } catch (e: Exception) {
            val forums = forumRepository.observeForums().first()
            val errorOutcomes = forums.map { forum ->
                SyncOutcome(forumId = forum.id, status = SyncStatus.ERROR, errorMessage = e.message)
            }
            syncLog.addEntry(source, errorOutcomes)
            
            if (source == SyncSource.TEST && appPreferences.isTestModeEnabled.first()) {
                syncSchedulerProvider.get().scheduleNextTestRun()
            }
            Result.retry()
        }
    }

    private suspend fun handleChallenge() {
        val failures = challengeRepository.consecutiveFailures.first()
        if (failures >= 3) {
            syncSchedulerProvider.get().reschedulePeriodicSync(isLongPeriod = true)
        }

        val lastPrompt = challengeRepository.lastPromptAt.first()
        val now = System.currentTimeMillis()
        if (now - lastPrompt > 60 * 60 * 1000) {
            appNotifier.notifyVerificationRequired()
            challengeRepository.updateLastPromptAt(now)
        }
    }
}

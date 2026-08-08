package com.jdrvirtuel.watcher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.SyncSource
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import com.jdrvirtuel.watcher.domain.usecase.SyncAllForumsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import javax.inject.Provider

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncAllForums: SyncAllForumsUseCase,
    private val appPreferences: AppPreferences,
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

            when {
                anySuccess -> Result.success()
                anyChallenge -> Result.success() // Challenge will be handled by UI/Module 09
                allFailedNet -> Result.retry()
                else -> Result.success()
            }
        } catch (e: Exception) {
            val errorOutcomes = listOf(
                SyncOutcome(forumId = 15, status = SyncStatus.ERROR, errorMessage = e.message),
                SyncOutcome(forumId = 16, status = SyncStatus.ERROR, errorMessage = e.message)
            )
            syncLog.addEntry(source, errorOutcomes)
            
            if (source == SyncSource.TEST && appPreferences.isTestModeEnabled.first()) {
                syncSchedulerProvider.get().scheduleNextTestRun()
            }
            Result.retry()
        }
    }
}

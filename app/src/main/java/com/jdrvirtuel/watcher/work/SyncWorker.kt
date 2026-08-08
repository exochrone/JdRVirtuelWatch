package com.jdrvirtuel.watcher.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
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
    private val syncSchedulerProvider: Provider<SyncScheduler>
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val outcomes = syncAllForums()
            
            // Log for test mode if active
            if (appPreferences.isTestModeEnabled.first()) {
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
            Result.retry()
        }
    }
}

package com.jdrvirtuel.watcher.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.SyncSource
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val appPreferences: AppPreferences
) {
    companion object {
        private const val PERIODIC_SYNC_NAME = "periodic_sync"
        private const val IMMEDIATE_SYNC_NAME = "immediate_sync"
        private const val TEST_SYNC_NAME = "test_sync"
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("sync_source", SyncSource.PERIODIC.name)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("sync_source", SyncSource.MANUAL.name)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(PERIODIC_SYNC_NAME)
    }

    fun reschedulePeriodicSync() {
        workManager.cancelUniqueWork(PERIODIC_SYNC_NAME)
        schedulePeriodicSync()
    }

    suspend fun startTestMode(intervalMinutes: Int) {
        appPreferences.setTestModeEnabled(true)
        appPreferences.setTestModeIntervalMinutes(intervalMinutes)
        appPreferences.setTestModeLog(null) // Clear previous log
        scheduleNextTestRun()
    }

    suspend fun stopTestMode() {
        appPreferences.setTestModeEnabled(false)
        workManager.cancelUniqueWork(TEST_SYNC_NAME)
    }

    suspend fun scheduleNextTestRun() {
        if (!appPreferences.isTestModeEnabled.first()) return

        val interval = appPreferences.testModeIntervalMinutes.first()

        val inputData = Data.Builder()
            .putString("sync_source", SyncSource.TEST.name)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(interval.toLong(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            TEST_SYNC_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

package com.jdrvirtuel.watcher.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
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

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
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
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(interval.toLong(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniqueWork(
            TEST_SYNC_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

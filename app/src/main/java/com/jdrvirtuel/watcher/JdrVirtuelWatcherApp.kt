package com.jdrvirtuel.watcher

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jdrvirtuel.watcher.core.di.ApplicationScope
import com.jdrvirtuel.watcher.data.local.db.DatabaseSeeder
import com.jdrvirtuel.watcher.notification.NotificationChannels
import com.jdrvirtuel.watcher.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JdrVirtuelWatcherApp : Application(), Configuration.Provider {
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        applicationScope.launch {
            databaseSeeder.seedIfEmpty()
        }
        syncScheduler.schedulePeriodicSync()
    }
}

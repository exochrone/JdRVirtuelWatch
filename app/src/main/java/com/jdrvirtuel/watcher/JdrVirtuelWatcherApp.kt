package com.jdrvirtuel.watcher

import android.app.Application
import com.jdrvirtuel.watcher.core.di.ApplicationScope
import com.jdrvirtuel.watcher.data.local.db.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class JdrVirtuelWatcherApp : Application() {
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            databaseSeeder.seedIfEmpty()
        }
    }
}

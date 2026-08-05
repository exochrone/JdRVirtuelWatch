package com.jdrvirtuel.watcher

import android.app.Application
import com.jdrvirtuel.watcher.data.local.db.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JdrVirtuelWatcherApp : Application() {
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    override fun onCreate() {
        super.onCreate()
        databaseSeeder.seedIfEmpty()
    }
}

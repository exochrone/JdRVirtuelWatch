package com.jdrvirtuel.watcher.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jdrvirtuel.watcher.domain.model.SyncSource
import com.jdrvirtuel.watcher.work.SyncWorker

class SyncActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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

        WorkManager.getInstance(context).enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

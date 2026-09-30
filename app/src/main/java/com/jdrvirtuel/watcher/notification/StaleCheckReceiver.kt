package com.jdrvirtuel.watcher.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StaleCheckReceiver : BroadcastReceiver() {

    @Inject
    lateinit var statusNotifier: StatusNotifier

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                statusNotifier.update()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

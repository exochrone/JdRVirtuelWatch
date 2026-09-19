package com.jdrvirtuel.watcher.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.jdrvirtuel.watcher.R

object NotificationChannels {
    const val STATUS = "status"

    fun create(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create STATUS channel with DEFAULT importance for alerts
        val statusChannel = NotificationChannel(
            STATUS,
            context.getString(R.string.notification_channel_status),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_status_desc)
            setShowBadge(true)
        }
        manager.createNotificationChannel(statusChannel)

        // Remove old channels
        manager.deleteNotificationChannel("new_topics")
        manager.deleteNotificationChannel("new_replies")
        manager.deleteNotificationChannel("verification")
    }
}

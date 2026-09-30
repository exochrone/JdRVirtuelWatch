package com.jdrvirtuel.watcher.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.jdrvirtuel.watcher.R

object NotificationChannels {
    const val STATUS_IDLE = "status_idle"
    const val STATUS_NEW = "status_new"
    const val STATUS_ALERT = "status_alert"

    fun create(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val idleChannel = NotificationChannel(
            STATUS_IDLE,
            context.getString(R.string.notification_channel_status_idle),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = context.getString(R.string.notification_channel_status_idle_desc)
            setShowBadge(false)
        }

        val newChannel = NotificationChannel(
            STATUS_NEW,
            context.getString(R.string.notification_channel_status_new),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_status_new_desc)
            setShowBadge(true)
        }

        val alertChannel = NotificationChannel(
            STATUS_ALERT,
            context.getString(R.string.notification_channel_status_alert),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_status_alert_desc)
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(idleChannel, newChannel, alertChannel))

        // Remove old channels
        manager.deleteNotificationChannel("status")
        manager.deleteNotificationChannel("new_topics")
        manager.deleteNotificationChannel("new_replies")
        manager.deleteNotificationChannel("verification")
    }
}

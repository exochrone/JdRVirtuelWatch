package com.jdrvirtuel.watcher.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.jdrvirtuel.watcher.R

object NotificationChannels {
    const val NEW_TOPICS = "new_topics"
    const val NEW_REPLIES = "new_replies"
    const val VERIFICATION = "verification"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                NEW_TOPICS,
                context.getString(R.string.notification_channel_new_topics),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_new_topics_desc)
            },
            NotificationChannel(
                NEW_REPLIES,
                context.getString(R.string.notification_channel_new_replies),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_new_replies_desc)
            },
            NotificationChannel(
                VERIFICATION,
                context.getString(R.string.notification_channel_verification),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_verification_desc)
            }
        )

        manager.createNotificationChannels(channels)
    }
}

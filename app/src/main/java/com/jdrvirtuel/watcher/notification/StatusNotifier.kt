package com.jdrvirtuel.watcher.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.jdrvirtuel.watcher.MainActivity
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.util.DateFormatter
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forumRepository: ForumRepository,
    private val appPreferences: AppPreferences
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    suspend fun update() {
        if (!appPreferences.isStatusNotificationEnabled.first()) {
            cancel()
            return
        }

        val forums = forumRepository.observeForums().first()
        if (forums.isEmpty()) return

        val lastSuccess = forums.mapNotNull { it.lastSyncAt }.maxOrNull()
        val allFailed = forums.isNotEmpty() && forums.all { !it.lastSyncSuccess && it.lastSyncAt != null }
        val anyChallenge = forums.any { it.lastSyncError == "CHALLENGE_REQUIRED" } // Or use a proper status if available in Forum model

        val title = when {
            anyChallenge -> context.getString(R.string.verification_title)
            allFailed -> context.getString(R.string.notification_status_error)
            else -> context.getString(R.string.notification_status_active)
        }

        val text = when {
            anyChallenge -> context.getString(R.string.notification_status_cloudflare_text)
            lastSuccess != null -> {
                val time = DateFormatter.formatStatusTime(lastSuccess)
                if (allFailed) {
                    context.getString(R.string.notification_status_last_success_failed, time)
                } else {
                    context.getString(R.string.notification_status_last_success, time)
                }
            }
            else -> context.getString(R.string.notification_status_never)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = Intent(context, SyncActionReceiver::class.java)
        val syncPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            syncIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground, // Replace with appropriate sync icon if available
                context.getString(R.string.notification_status_sync),
                syncPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground, // Replace with appropriate open icon if available
                context.getString(R.string.notification_status_open),
                openPendingIntent
            )
            .build()

        notificationManager.notify(NotificationIds.STATUS, notification)
    }

    fun cancel() {
        notificationManager.cancel(NotificationIds.STATUS)
    }
}

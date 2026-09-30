package com.jdrvirtuel.watcher.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.jdrvirtuel.watcher.MainActivity
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.SyncHighlights
import com.jdrvirtuel.watcher.domain.repository.ChallengeStateRepository
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val appPreferences: AppPreferences,
    private val challengeRepository: ChallengeStateRepository,
    private val staleCheckScheduler: StaleCheckScheduler,
    private val statusLevelResolver: StatusLevelResolver
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun update(hasNewTopics: Boolean = false) {
        if (!appPreferences.isStatusNotificationEnabled.first()) {
            cancel()
            return
        }

        val forums: List<Forum> = forumRepository.observeForums().first().sortedBy { it.id }
        if (forums.isEmpty()) return

        val forumSyncStates = forums.map { forum ->
            ForumSyncState(
                lastSyncAt = forum.lastSyncAt,
                lastSyncSuccess = forum.lastSyncSuccess,
                lastSyncError = forum.lastSyncError
            )
        }

        val highlights = getHighlights()
        var pendingNewCount = 0
        for (forum in forums) {
            val forumIdKey = forum.id.toString()
            val topicCount = highlights.newTopicsByForum[forumIdKey]?.size ?: 0
            val replyCount = highlights.newReplyCountByForum[forumIdKey] ?: 0
            pendingNewCount += topicCount + replyCount
        }

        val challengeFailures = challengeRepository.consecutiveFailures.first()
        val now = System.currentTimeMillis()

        val decision = statusLevelResolver.resolve(
            forums = forumSyncStates,
            challengeFailures = challengeFailures,
            pendingNewCount = pendingNewCount,
            now = now
        )

        val lastSuccess: Long? = forums.mapNotNull { it.lastSyncAt }.maxOrNull()

        val title = when (decision.level) {
            StatusLevel.IDLE, StatusLevel.NEW -> {
                if (lastSuccess != null) {
                    context.getString(R.string.notification_status_active)
                } else {
                    context.getString(R.string.notification_status_never)
                }
            }
            StatusLevel.ALERT -> {
                when (decision.reason) {
                    AlertReason.VERIFICATION -> context.getString(R.string.verification_required)
                    AlertReason.SYNC_FAILED -> context.getString(R.string.notification_status_error_title)
                    AlertReason.STALE -> context.getString(R.string.notification_status_stale_title)
                    null -> context.getString(R.string.notification_status_error_title)
                }
            }
        }

        val separator = context.getString(R.string.notification_status_separator)
        val lines = mutableListOf<String>()

        for (forum in forums) {
            val forumIdKey = forum.id.toString()
            val visibleCount = topicRepository.observeVisibleCount(forum.id).first()
            val baseLine = context.getString(R.string.notification_status_line, forum.name, visibleCount)
            val lineBuilder = StringBuilder(baseLine)

            val topicCount = highlights.newTopicsByForum[forumIdKey]?.size ?: 0
            val replyCount = highlights.newReplyCountByForum[forumIdKey] ?: 0

            if (topicCount > 0) {
                lineBuilder.append(separator)
                lineBuilder.append(
                    context.resources.getQuantityString(
                        R.plurals.notification_status_topics,
                        topicCount,
                        topicCount
                    )
                )
            }

            if (replyCount > 0) {
                lineBuilder.append(separator)
                lineBuilder.append(
                    context.resources.getQuantityString(
                        R.plurals.notification_status_replies,
                        replyCount,
                        replyCount
                    )
                )
            }

            lines.add(lineBuilder.toString())
        }

        val text = lines.firstOrNull() ?: ""
        val bigText = lines.joinToString("\n")

        val targetChannel = when (decision.level) {
            StatusLevel.IDLE -> NotificationChannels.STATUS_IDLE
            StatusLevel.NEW -> NotificationChannels.STATUS_NEW
            StatusLevel.ALERT -> NotificationChannels.STATUS_ALERT
        }

        val currentSignature = when (decision.level) {
            StatusLevel.IDLE -> "IDLE"
            StatusLevel.NEW -> "NEW"
            StatusLevel.ALERT -> "ALERT_${decision.reason?.name ?: "UNKNOWN"}"
        }

        val lastSignature = appPreferences.lastStatusSignature.first()

        val channelChanged = lastSignature == null || getChannelForSignature(lastSignature) != targetChannel

        val isSilent = when (decision.level) {
            StatusLevel.IDLE -> true
            StatusLevel.NEW -> !hasNewTopics
            StatusLevel.ALERT -> {
                val previousWasAlert = lastSignature != null && lastSignature.startsWith("ALERT")
                if (!previousWasAlert) {
                    false
                } else {
                    lastSignature == currentSignature
                }
            }
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val syncIntent = Intent(context, SyncActionReceiver::class.java)
        val syncPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            syncIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, targetChannel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .setSilent(isSilent)
            .addAction(
                R.drawable.ic_sync,
                context.getString(R.string.notification_status_sync),
                syncPendingIntent
            )

        for ((index, forum) in forums.withIndex()) {
            val forumIntent = Intent(
                Intent.ACTION_VIEW,
                "jdrvirtuel://forum/${forum.id}".toUri(),
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val forumPendingIntent = PendingIntent.getActivity(
                context,
                2 + index,
                forumIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                R.drawable.ic_open,
                forum.name,
                forumPendingIntent
            )
        }

        if (lastSuccess != null) {
            builder.setWhen(lastSuccess)
            builder.setShowWhen(true)
        } else {
            builder.setShowWhen(false)
        }

        if (channelChanged) {
            notificationManager.cancel(NotificationIds.STATUS)
        }

        notificationManager.notify(NotificationIds.STATUS, builder.build())
        appPreferences.setLastStatusSignature(currentSignature)

        if (lastSuccess != null && (decision.level != StatusLevel.ALERT || decision.reason != AlertReason.STALE)) {
            staleCheckScheduler.schedule(lastSuccess + StatusLevelResolver.STALE_THRESHOLD_MS)
        } else {
            staleCheckScheduler.cancel()
        }
    }

    private fun getChannelForSignature(signature: String): String {
        return when {
            signature == "IDLE" -> NotificationChannels.STATUS_IDLE
            signature == "NEW" -> NotificationChannels.STATUS_NEW
            signature.startsWith("ALERT") -> NotificationChannels.STATUS_ALERT
            else -> NotificationChannels.STATUS_IDLE
        }
    }

    private suspend fun getHighlights(): SyncHighlights {
        val serialized = appPreferences.lastSyncHighlights.first() ?: return SyncHighlights()
        return try {
            json.decodeFromString<SyncHighlights>(serialized)
        } catch (e: Exception) {
            SyncHighlights()
        }
    }

    fun cancel() {
        notificationManager.cancel(NotificationIds.STATUS)
        staleCheckScheduler.cancel()
    }
}

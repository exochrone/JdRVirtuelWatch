package com.jdrvirtuel.watcher.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
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
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val appPreferences: AppPreferences,
    private val challengeRepository: ChallengeStateRepository
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun update(hasNewTopics: Boolean = false) {
        if (!appPreferences.isStatusNotificationEnabled.first()) {
            cancel()
            return
        }

        val forums: List<Forum> = forumRepository.observeForums().first()
        if (forums.isEmpty()) return

        val lastSuccess: Long? = forums.mapNotNull { f: Forum -> f.lastSyncAt }.maxOrNull()
        val allFailed = forums.isNotEmpty() && forums.all { f: Forum -> f.lastSyncAt != null || f.lastSyncError != null } && forums.all { f: Forum -> !f.lastSyncSuccess }
        val anyChallenge = challengeRepository.consecutiveFailures.first() >= 1

        val title = when {
            anyChallenge -> context.getString(R.string.verification_required)
            allFailed -> context.getString(R.string.notification_status_error_title)
            lastSuccess != null -> context.getString(R.string.notification_status_active)
            else -> context.getString(R.string.notification_status_never)
        }

        val highlights = getHighlights()
        val text: String
        val bigText: String?

        if ((highlights.isEmpty && !hasNewTopics) || anyChallenge) {
            text = if (anyChallenge) {
                context.getString(R.string.notification_status_cloudflare_text)
            } else {
                val oneshots = forums.find { it.id == 15 }
                val campagnes = forums.find { it.id == 16 }
                
                val count15 = topicRepository.observeVisibleCount(15).first()
                val count16 = topicRepository.observeVisibleCount(16).first()
                
                context.getString(
                    R.string.notification_status_repos,
                    oneshots?.name ?: "Oneshots", count15,
                    campagnes?.name ?: "Campagnes", count16
                )
            }
            bigText = null
        } else {
            val lines = mutableListOf<String>()
            val sortedForums = forums.sortedBy { it.id }
            
            for (forum in sortedForums) {
                val forumIdKey = forum.id.toString()
                
                // 1. New topics line for this forum
                val topicsList = highlights.newTopicsByForum[forumIdKey]
                if (!topicsList.isNullOrEmpty()) {
                    val count = topicRepository.observeVisibleCount(forum.id).first()
                    if (topicsList.size == 1) {
                        lines.add(context.getString(R.string.notification_status_new_topics_single, forum.name, count, topicsList.first()))
                    } else {
                        lines.add(context.getString(R.string.notification_status_new_topics_multiple, forum.name, count, topicsList.size.toLong()))
                    }
                }
                
                // 2. New replies line for this forum
                val replyCount = highlights.newReplyCountByForum[forumIdKey] ?: 0
                if (replyCount > 0) {
                    lines.add(
                        context.resources.getQuantityString(
                            R.plurals.notification_status_forum_replies,
                            replyCount,
                            forum.name,
                            replyCount
                        )
                    )
                }
            }
            
            val totalLines = lines.size
            if (totalLines > 0) {
                val displayLines = lines.take(5).toMutableList()
                if (totalLines > 5) {
                    displayLines.add(context.getString(R.string.notification_status_others, (totalLines - 5).toLong()))
                }
                
                text = displayLines.first()
                bigText = if (displayLines.size > 1) displayLines.joinToString("\n") else null
            } else {
                val count15 = topicRepository.observeVisibleCount(15).first()
                val count16 = topicRepository.observeVisibleCount(16).first()
                text = context.getString(
                    R.string.notification_status_repos,
                    "Oneshots", count15,
                    "Campagnes", count16
                )
                bigText = null
            }
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

        val builder = NotificationCompat.Builder(context, NotificationChannels.STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setWhen(lastSuccess ?: 0L)
            .setShowWhen(lastSuccess != null)
            .setOnlyAlertOnce(!hasNewTopics && !anyChallenge && !allFailed)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_sync,
                context.getString(R.string.notification_status_sync),
                syncPendingIntent
            )
            .addAction(
                R.drawable.ic_open,
                context.getString(R.string.notification_status_open),
                openPendingIntent
            )

        if (bigText != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }

        notificationManager.notify(NotificationIds.STATUS, builder.build())
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
    }
}

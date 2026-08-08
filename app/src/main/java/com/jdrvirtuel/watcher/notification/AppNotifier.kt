package com.jdrvirtuel.watcher.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.jdrvirtuel.watcher.MainActivity
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.NotificationType
import com.jdrvirtuel.watcher.domain.model.Topic
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationLog: NotificationLog
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    suspend fun notifyNewTopics(forum: Forum, topics: List<Topic>) {
        if (topics.isEmpty()) return

        topics.forEach { topic ->
            val notification = createTopicNotification(
                forum = forum,
                topic = topic,
                channelId = NotificationChannels.NEW_TOPICS,
                notificationId = NotificationIds.forNewTopic(topic.id)
            )
            notificationManager.notify(NotificationIds.forNewTopic(topic.id), notification)
            
            notificationLog.addEntry(
                type = NotificationType.NEW_TOPIC,
                forumName = forum.name,
                topicTitle = topic.title
            )
        }
        
        updateSummary(forum, NotificationChannels.NEW_TOPICS, topics)
    }

    suspend fun notifyNewReplies(forum: Forum, topics: List<Topic>) {
        if (topics.isEmpty()) return

        topics.forEach { topic ->
            val notification = createTopicNotification(
                forum = forum,
                topic = topic,
                channelId = NotificationChannels.NEW_REPLIES,
                notificationId = NotificationIds.forNewReply(topic.id),
                isReply = true
            )
            notificationManager.notify(NotificationIds.forNewReply(topic.id), notification)
            
            notificationLog.addEntry(
                type = NotificationType.NEW_REPLY,
                forumName = forum.name,
                topicTitle = topic.title
            )
        }
        
        updateSummary(forum, NotificationChannels.NEW_REPLIES, topics)
    }

    suspend fun notifyVerificationRequired() {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("jdrvirtuel://verification")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            42,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.VERIFICATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.verification_title))
            .setContentText(context.getString(R.string.verification_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(42, notification)
        
        notificationLog.addEntry(type = NotificationType.VERIFICATION)
    }

    private fun createTopicNotification(
        forum: Forum,
        topic: Topic,
        channelId: String,
        notificationId: Int,
        isReply: Boolean = false
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("jdrvirtuel://topic/${topic.id}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (isReply) {
            context.getString(R.string.notification_new_reply_body, topic.title)
        } else {
            topic.title
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(forum.name)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setGroup(NotificationIds.getGroupKey(forum.id))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun updateSummary(forum: Forum, channelId: String, topics: List<Topic>) {
        val count = topics.size
        val text = context.resources.getQuantityString(R.plurals.notification_summary_more, count, count)
        
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(forum.name)
            .setSummaryText(text)
        
        topics.take(5).forEach { 
            inboxStyle.addLine(it.title)
        }

        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(forum.name)
            .setContentText(text)
            .setGroup(NotificationIds.getGroupKey(forum.id))
            .setGroupSummary(true)
            .setStyle(inboxStyle)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationIds.forGroupSummary(forum.id), summaryNotification)
    }
}

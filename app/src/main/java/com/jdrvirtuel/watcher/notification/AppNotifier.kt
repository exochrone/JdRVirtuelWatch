package com.jdrvirtuel.watcher.notification

import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.NotificationType
import com.jdrvirtuel.watcher.domain.model.Topic
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles logging of notification events. 
 * Actual Android notifications are now handled by [StatusNotifier].
 */
@Singleton
class AppNotifier @Inject constructor(
    private val notificationLog: NotificationLog
) {
    suspend fun notifyNewTopics(forum: Forum, topics: List<Topic>) {
        if (topics.isEmpty()) return

        topics.forEach { topic ->
            notificationLog.addEntry(
                type = NotificationType.NEW_TOPIC,
                forumName = forum.name,
                topicTitle = topic.title
            )
        }
    }

    suspend fun notifyNewReplies(forum: Forum, topics: List<Topic>) {
        if (topics.isEmpty()) return

        topics.forEach { topic ->
            notificationLog.addEntry(
                type = NotificationType.NEW_REPLY,
                forumName = forum.name,
                topicTitle = topic.title
            )
        }
    }

    suspend fun notifyVerificationRequired() {
        notificationLog.addEntry(type = NotificationType.VERIFICATION)
    }
}

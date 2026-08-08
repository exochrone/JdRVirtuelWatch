package com.jdrvirtuel.watcher.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    NEW_TOPIC,
    NEW_REPLY,
    VERIFICATION
}

@Serializable
data class NotificationLogEntry(
    val timestampMs: Long,
    val type: NotificationType,
    val forumName: String?,
    val topicTitle: String?
)

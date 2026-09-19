package com.jdrvirtuel.watcher.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncHighlights(
    val newTopicsByForum: Map<String, List<String>> = emptyMap(),
    val newRepliesByTopic: List<ReplyHighlight> = emptyList()
) {
    val isEmpty: Boolean get() = newTopicsByForum.isEmpty() && newRepliesByTopic.isEmpty()
}

@Serializable
data class ReplyHighlight(
    val title: String,
    val count: Int
)

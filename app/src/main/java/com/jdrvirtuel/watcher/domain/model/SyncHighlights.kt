package com.jdrvirtuel.watcher.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncHighlights(
    val newTopicsByForum: Map<String, List<String>> = emptyMap(),
    val newReplyCountByForum: Map<String, Int> = emptyMap()
) {
    val isEmpty: Boolean get() = newTopicsByForum.isEmpty() && newReplyCountByForum.isEmpty()
}

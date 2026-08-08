package com.jdrvirtuel.watcher.notification

object NotificationIds {
    private const val REPLY_OFFSET = 1_000_000

    fun forNewTopic(topicId: Int): Int = topicId

    fun forNewReply(topicId: Int): Int = topicId + REPLY_OFFSET

    fun forGroupSummary(forumId: Int): Int = forumId

    fun getGroupKey(forumId: Int): String = "group_forum_$forumId"
}

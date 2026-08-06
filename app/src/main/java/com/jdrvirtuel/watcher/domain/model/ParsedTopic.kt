package com.jdrvirtuel.watcher.domain.model

data class ParsedTopic(
    val id: Int,
    val title: String,
    val url: String,
    val author: String,
    val createdAt: Long,
    val replyCount: Int,
    val lastPostAuthor: String,
    val lastPostAt: Long,
    val isFull: Boolean
)

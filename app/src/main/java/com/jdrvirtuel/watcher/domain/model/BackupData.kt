package com.jdrvirtuel.watcher.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val appVersion: String,
    val forums: List<BackupForum>
)

@Serializable
data class BackupForum(
    val id: Int,
    val name: String,
    val topics: List<BackupTopic>
)

@Serializable
data class BackupTopic(
    val id: Int,
    val title: String,
    val url: String,
    val author: String,
    val createdAt: Long,
    val replyCount: Int,
    val lastPostAuthor: String,
    val lastPostAt: Long,
    val isFull: Boolean,
    val isHidden: Boolean,
    val isWatched: Boolean,
    val isRead: Boolean,
    val firstSeenAt: Long,
    val lastSeenAt: Long
)

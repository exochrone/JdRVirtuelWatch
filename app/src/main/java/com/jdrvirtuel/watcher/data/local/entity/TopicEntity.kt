package com.jdrvirtuel.watcher.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = ForumEntity::class,
            parentColumns = ["id"],
            childColumns = ["forumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["forumId"])]
)
data class TopicEntity(
    @PrimaryKey val id: Int,
    val forumId: Int,
    val title: String,
    val url: String,
    val author: String,
    val createdAt: Long,
    val replyCount: Int = 0,
    val lastPostAuthor: String,
    val lastPostAt: Long,
    val isFull: Boolean = false,
    val isHidden: Boolean = false,
    val isWatched: Boolean = false,
    val isRead: Boolean = false,
    val firstSeenAt: Long,
    val lastSeenAt: Long
)

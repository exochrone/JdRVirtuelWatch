package com.jdrvirtuel.watcher.data.mapper

import com.jdrvirtuel.watcher.data.local.entity.TopicEntity
import com.jdrvirtuel.watcher.domain.model.Topic

fun TopicEntity.toDomain() = Topic(
    id = id,
    forumId = forumId,
    title = title,
    url = url,
    author = author,
    createdAt = createdAt,
    replyCount = replyCount,
    lastPostAuthor = lastPostAuthor,
    lastPostAt = lastPostAt,
    isFull = isFull,
    isHidden = isHidden,
    isWatched = isWatched,
    isRead = isRead,
    firstSeenAt = firstSeenAt,
    lastSeenAt = lastSeenAt
)

fun Topic.toEntity() = TopicEntity(
    id = id,
    forumId = forumId,
    title = title,
    url = url,
    author = author,
    createdAt = createdAt,
    replyCount = replyCount,
    lastPostAuthor = lastPostAuthor,
    lastPostAt = lastPostAt,
    isFull = isFull,
    isHidden = isHidden,
    isWatched = isWatched,
    isRead = isRead,
    firstSeenAt = firstSeenAt,
    lastSeenAt = lastSeenAt
)

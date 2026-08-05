package com.jdrvirtuel.watcher.data.mapper

import com.jdrvirtuel.watcher.data.local.entity.ForumEntity
import com.jdrvirtuel.watcher.domain.model.Forum

fun ForumEntity.toDomain() = Forum(
    id = id,
    name = name,
    url = url,
    lastSyncAt = lastSyncAt,
    lastSyncSuccess = lastSyncSuccess,
    lastSyncError = lastSyncError,
    isBootstrapped = isBootstrapped
)

fun Forum.toEntity() = ForumEntity(
    id = id,
    name = name,
    url = url,
    lastSyncAt = lastSyncAt,
    lastSyncSuccess = lastSyncSuccess,
    lastSyncError = lastSyncError,
    isBootstrapped = isBootstrapped
)

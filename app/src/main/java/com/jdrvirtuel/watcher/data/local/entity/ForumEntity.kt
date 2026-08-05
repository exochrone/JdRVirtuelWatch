package com.jdrvirtuel.watcher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forums")
data class ForumEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val url: String,
    val lastSyncAt: Long? = null,
    val lastSyncSuccess: Boolean = false,
    val lastSyncError: String? = null,
    val isBootstrapped: Boolean = false
)

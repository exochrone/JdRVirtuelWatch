package com.jdrvirtuel.watcher.domain.model

data class Forum(
    val id: Int,
    val name: String,
    val url: String,
    val lastSyncAt: Long? = null,
    val lastSyncSuccess: Boolean = false,
    val lastSyncError: String? = null,
    val isBootstrapped: Boolean = false
)

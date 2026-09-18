package com.jdrvirtuel.watcher.domain.model

data class BackupResult(
    val restoredCount: Int = 0,
    val insertedCount: Int = 0,
    val ignoredCount: Int = 0,
    val intactCount: Int = 0
)

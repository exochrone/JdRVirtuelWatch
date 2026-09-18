package com.jdrvirtuel.watcher.domain.repository

import com.jdrvirtuel.watcher.domain.model.BackupData

interface BackupRepository {
    fun serialize(data: BackupData): String
    fun deserialize(json: String): BackupData?
}

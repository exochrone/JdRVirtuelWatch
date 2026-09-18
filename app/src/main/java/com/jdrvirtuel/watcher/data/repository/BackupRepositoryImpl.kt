package com.jdrvirtuel.watcher.data.repository

import com.jdrvirtuel.watcher.domain.model.BackupData
import com.jdrvirtuel.watcher.domain.repository.BackupRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor() : BackupRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    override fun serialize(data: BackupData): String {
        return json.encodeToString(data)
    }

    override fun deserialize(jsonString: String): BackupData? {
        return try {
            json.decodeFromString<BackupData>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}

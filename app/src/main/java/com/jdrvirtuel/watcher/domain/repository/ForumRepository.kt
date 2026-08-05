package com.jdrvirtuel.watcher.domain.repository

import com.jdrvirtuel.watcher.domain.model.Forum
import kotlinx.coroutines.flow.Flow

interface ForumRepository {
    fun observeForums(): Flow<List<Forum>>
    fun observeForum(id: Int): Flow<Forum?>
    suspend fun getForum(id: Int): Forum?
    suspend fun updateSyncState(id: Int, success: Boolean, at: Long, error: String?)
    suspend fun markBootstrapped(id: Int)
}

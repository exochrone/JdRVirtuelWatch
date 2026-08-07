package com.jdrvirtuel.watcher.domain.repository

import com.jdrvirtuel.watcher.domain.model.Topic
import kotlinx.coroutines.flow.Flow

interface TopicRepository {
    fun observeTopics(forumId: Int): Flow<List<Topic>>
    suspend fun getTopics(forumId: Int): List<Topic>
    suspend fun upsertAll(topics: List<Topic>)
    suspend fun setHidden(id: Int, hidden: Boolean)
    suspend fun setWatched(id: Int, watched: Boolean)
    suspend fun setRead(id: Int, read: Boolean)
    suspend fun deleteById(id: Int)
    suspend fun deleteStale(forumId: Int, threshold: Long): Int
    fun observeUnreadCount(forumId: Int): Flow<Int>
    
    // For debug
    suspend fun deleteAll()
    fun observeTotalCount(): Flow<Int>
}

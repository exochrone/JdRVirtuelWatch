package com.jdrvirtuel.watcher.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jdrvirtuel.watcher.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE forumId = :forumId ORDER BY lastPostAt DESC")
    fun observeByForum(forumId: Int): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE forumId = :forumId")
    suspend fun getByForum(forumId: Int): List<TopicEntity>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getById(id: Int): TopicEntity?

    @Upsert
    suspend fun upsertAll(topics: List<TopicEntity>): List<Long>

    @Query("UPDATE topics SET isHidden = :hidden, isWatched = CASE WHEN :hidden THEN 0 ELSE isWatched END WHERE id = :id")
    suspend fun updateHidden(id: Int, hidden: Boolean): Int

    @Query("UPDATE topics SET isWatched = :watched WHERE id = :id")
    suspend fun updateWatched(id: Int, watched: Boolean): Int

    @Query("UPDATE topics SET isRead = :read WHERE id = :id")
    suspend fun updateRead(id: Int, read: Boolean): Int

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM topics WHERE forumId = :forumId AND lastSeenAt < :threshold AND isWatched = 0")
    suspend fun deleteStale(forumId: Int, threshold: Long): Int

    @Query("SELECT COUNT(*) FROM topics WHERE forumId = :forumId AND isRead = 0 AND isHidden = 0")
    fun countUnread(forumId: Int): Flow<Int>

    @Query("DELETE FROM topics")
    suspend fun deleteAll(): Int
    
    @Query("SELECT COUNT(*) FROM topics")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM topics WHERE forumId = :forumId")
    fun observeTopicCount(forumId: Int): Flow<Int>
}

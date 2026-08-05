package com.jdrvirtuel.watcher.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jdrvirtuel.watcher.data.local.entity.ForumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForumDao {
    @Query("SELECT * FROM forums ORDER BY id ASC")
    fun observeAll(): Flow<List<ForumEntity>>

    @Query("SELECT * FROM forums WHERE id = :id")
    fun observeById(id: Int): Flow<ForumEntity?>

    @Query("SELECT * FROM forums WHERE id = :id")
    suspend fun getById(id: Int): ForumEntity?

    @Upsert
    suspend fun upsert(forum: ForumEntity): Long

    @Query("SELECT COUNT(*) FROM forums")
    suspend fun count(): Int
}

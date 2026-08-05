package com.jdrvirtuel.watcher.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jdrvirtuel.watcher.data.local.dao.ForumDao
import com.jdrvirtuel.watcher.data.local.dao.TopicDao
import com.jdrvirtuel.watcher.data.local.entity.ForumEntity
import com.jdrvirtuel.watcher.data.local.entity.TopicEntity

@Database(
    entities = [ForumEntity::class, TopicEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun forumDao(): ForumDao
    abstract fun topicDao(): TopicDao
}

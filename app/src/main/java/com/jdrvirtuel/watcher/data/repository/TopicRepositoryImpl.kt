package com.jdrvirtuel.watcher.data.repository

import com.jdrvirtuel.watcher.data.local.dao.TopicDao
import com.jdrvirtuel.watcher.data.mapper.toDomain
import com.jdrvirtuel.watcher.data.mapper.toEntity
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicRepositoryImpl @Inject constructor(
    private val topicDao: TopicDao
) : TopicRepository {

    override fun observeTopics(forumId: Int): Flow<List<Topic>> =
        topicDao.observeByForum(forumId).map { list -> list.map { it.toDomain() } }

    override suspend fun getTopics(forumId: Int): List<Topic> =
        topicDao.getByForum(forumId).map { it.toDomain() }

    override suspend fun upsertAll(topics: List<Topic>) {
        topicDao.upsertAll(topics.map { it.toEntity() })
    }

    override suspend fun setHidden(id: Int, hidden: Boolean) {
        topicDao.updateHidden(id, hidden)
    }

    override suspend fun setWatched(id: Int, watched: Boolean) {
        topicDao.updateWatched(id, watched)
    }

    override suspend fun setRead(id: Int, read: Boolean) {
        topicDao.updateRead(id, read)
    }

    override suspend fun deleteById(id: Int) {
        topicDao.deleteById(id)
    }

    override suspend fun deleteStale(threshold: Long): Int {
        return topicDao.deleteStale(threshold)
    }

    override fun observeUnreadCount(forumId: Int): Flow<Int> =
        topicDao.countUnread(forumId)

    override suspend fun deleteAll() {
        topicDao.deleteAll()
    }

    override fun observeTotalCount(): Flow<Int> =
        topicDao.observeCount()
}

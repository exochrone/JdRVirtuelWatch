package com.jdrvirtuel.watcher.data.repository

import com.jdrvirtuel.watcher.data.local.dao.ForumDao
import com.jdrvirtuel.watcher.data.mapper.toDomain
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForumRepositoryImpl @Inject constructor(
    private val forumDao: ForumDao
) : ForumRepository {

    override fun observeForums(): Flow<List<Forum>> =
        forumDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeForum(id: Int): Flow<Forum?> =
        forumDao.observeById(id).map { it?.toDomain() }

    override suspend fun getForum(id: Int): Forum? =
        forumDao.getById(id)?.toDomain()

    override suspend fun updateSyncState(id: Int, success: Boolean, at: Long?, error: String?) {
        val forum = forumDao.getById(id) ?: return
        forumDao.upsert(
            forum.copy(
                lastSyncAt = at ?: forum.lastSyncAt,
                lastSyncSuccess = success,
                lastSyncError = error
            )
        )
    }

    override suspend fun markBootstrapped(id: Int) {
        val forum = forumDao.getById(id) ?: return
        forumDao.upsert(forum.copy(isBootstrapped = true))
    }

    override suspend fun resetBootstrap(id: Int) {
        val forum = forumDao.getById(id) ?: return
        forumDao.upsert(forum.copy(isBootstrapped = false, lastSyncAt = null, lastSyncSuccess = false, lastSyncError = null))
    }
}

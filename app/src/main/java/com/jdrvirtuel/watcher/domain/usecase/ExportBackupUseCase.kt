package com.jdrvirtuel.watcher.domain.usecase

import com.jdrvirtuel.watcher.BuildConfig
import com.jdrvirtuel.watcher.domain.model.BackupData
import com.jdrvirtuel.watcher.domain.model.BackupForum
import com.jdrvirtuel.watcher.domain.model.BackupTopic
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(): BackupData {
        val forums = forumRepository.observeForums().first()
        val backupForums = forums.map { forum ->
            val topics = topicRepository.getTopics(forum.id)
            BackupForum(
                id = forum.id,
                name = forum.name,
                topics = topics.map { topic ->
                    BackupTopic(
                        id = topic.id,
                        title = topic.title,
                        url = topic.url,
                        author = topic.author,
                        createdAt = topic.createdAt,
                        replyCount = topic.replyCount,
                        lastPostAuthor = topic.lastPostAuthor,
                        lastPostAt = topic.lastPostAt,
                        isFull = topic.isFull,
                        isHidden = topic.isHidden,
                        isWatched = false,
                        isRead = topic.isRead,
                        firstSeenAt = topic.firstSeenAt,
                        lastSeenAt = topic.lastSeenAt
                    )
                }
            )
        }

        return BackupData(
            exportedAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            forums = backupForums
        )
    }
}

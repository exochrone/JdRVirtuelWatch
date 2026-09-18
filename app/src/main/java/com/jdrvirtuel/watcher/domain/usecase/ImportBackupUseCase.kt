package com.jdrvirtuel.watcher.domain.usecase

import com.jdrvirtuel.watcher.domain.model.BackupData
import com.jdrvirtuel.watcher.domain.model.BackupResult
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(data: BackupData): BackupResult {
        if (data.formatVersion > 1) {
            throw IllegalArgumentException("Unsupported backup format version: ${data.formatVersion}")
        }

        val existingForums = forumRepository.observeForums().first().map { it.id }.toSet()
        
        var restoredCount = 0
        var insertedCount = 0
        var ignoredCount = 0
        
        data.forums.forEach { backupForum ->
            if (backupForum.id !in existingForums) {
                ignoredCount += backupForum.topics.size
                return@forEach
            }
            
            val localTopics = topicRepository.getTopics(backupForum.id).associateBy { it.id }
            val topicsToUpsert = mutableListOf<Topic>()
            
            backupForum.topics.forEach { backupTopic ->
                val localTopic = localTopics[backupTopic.id]
                
                if (localTopic != null) {
                    // Restore user states only
                    val updatedTopic = localTopic.copy(
                        isHidden = backupTopic.isHidden,
                        isWatched = if (backupTopic.isHidden) false else backupTopic.isWatched,
                        isRead = backupTopic.isRead,
                        firstSeenAt = backupTopic.firstSeenAt
                    )
                    if (updatedTopic != localTopic) {
                        topicsToUpsert.add(updatedTopic)
                        restoredCount++
                    }
                } else {
                    // Insert new topic
                    topicsToUpsert.add(
                        Topic(
                            id = backupTopic.id,
                            forumId = backupForum.id,
                            title = backupTopic.title,
                            url = backupTopic.url,
                            author = backupTopic.author,
                            createdAt = backupTopic.createdAt,
                            replyCount = backupTopic.replyCount,
                            lastPostAuthor = backupTopic.lastPostAuthor,
                            lastPostAt = backupTopic.lastPostAt,
                            isFull = backupTopic.isFull,
                            isHidden = backupTopic.isHidden,
                            isWatched = if (backupTopic.isHidden) false else backupTopic.isWatched,
                            isRead = backupTopic.isRead,
                            firstSeenAt = backupTopic.firstSeenAt,
                            lastSeenAt = backupTopic.lastSeenAt
                        )
                    )
                    insertedCount++
                }
            }
            
            if (topicsToUpsert.isNotEmpty()) {
                topicRepository.upsertAll(topicsToUpsert)
            }
        }

        val allLocalTopicsAfter = forumRepository.observeForums().first().sumOf { 
            topicRepository.getTopics(it.id).size 
        }
        
        // Estimation of intact count: all local topics - (restored + inserted)
        // Actually restored are those updated. 
        // Intact count = topics in base before that were NOT updated.
        val totalLocalTopicsBefore = forumRepository.observeForums().first().sumOf { 
            localTopicsCount(it.id) 
        }
        
        // Simplification for the result:
        return BackupResult(
            restoredCount = restoredCount,
            insertedCount = insertedCount,
            ignoredCount = ignoredCount,
            intactCount = totalLocalTopicsBefore - restoredCount
        )
    }

    private suspend fun localTopicsCount(forumId: Int): Int {
        return topicRepository.getTopics(forumId).size
    }
}

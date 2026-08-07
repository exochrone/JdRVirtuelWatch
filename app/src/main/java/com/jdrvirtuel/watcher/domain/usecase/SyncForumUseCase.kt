package com.jdrvirtuel.watcher.domain.usecase

import com.jdrvirtuel.watcher.domain.model.*
import com.jdrvirtuel.watcher.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncForumUseCase @Inject constructor(
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val forumPageSource: ForumPageSource,
    private val parser: TopicParser,
    private val notifier: NewContentNotifier
) {
    private val mutexes = mutableMapOf<Int, Mutex>()

    private fun getMutex(forumId: Int): Mutex {
        return synchronized(mutexes) {
            mutexes.getOrPut(forumId) { Mutex() }
        }
    }

    suspend operator fun invoke(forumId: Int): SyncOutcome = withContext(Dispatchers.IO) {
        val mutex = getMutex(forumId)
        mutex.withLock {
            val forum = forumRepository.getForum(forumId) ?: return@withContext SyncOutcome(
                forumId = forumId,
                status = SyncStatus.ERROR,
                errorMessage = "Forum introuvable"
            )

            val fetchResult = forumPageSource.fetchHtml(forum.url)
            when (fetchResult) {
                is FetchResult.ChallengeRequired -> {
                    forumRepository.updateSyncState(forumId, false, null, "Vérification Cloudflare requise")
                    SyncOutcome(forumId, SyncStatus.CHALLENGE_REQUIRED)
                }
                is FetchResult.Error -> {
                    forumRepository.updateSyncState(forumId, false, null, fetchResult.message)
                    SyncOutcome(forumId, SyncStatus.ERROR, errorMessage = fetchResult.message)
                }
                is FetchResult.Success -> {
                    val parseResult = parser.parse(fetchResult.html)
                    if (parseResult.topics.isEmpty() && fetchResult.html.isNotEmpty()) {
                        val error = "Structure inattendue : aucun sujet trouvé"
                        forumRepository.updateSyncState(forumId, false, null, error)
                        return@withContext SyncOutcome(forumId, SyncStatus.ERROR, errorMessage = error)
                    }

                    val now = System.currentTimeMillis()
                    val existingTopics = topicRepository.getTopics(forumId).associateBy { it.id }
                    val newTopics = mutableListOf<Topic>()
                    val newReplies = mutableListOf<Topic>()
                    val topicsToUpsert = mutableListOf<Topic>()

                    for (parsed in parseResult.topics) {
                        val existing = existingTopics[parsed.id]
                        if (existing == null) {
                            val newTopic = Topic(
                                id = parsed.id,
                                forumId = forumId,
                                title = parsed.title,
                                url = parsed.url,
                                author = parsed.author,
                                createdAt = parsed.createdAt,
                                replyCount = parsed.replyCount,
                                lastPostAuthor = parsed.lastPostAuthor,
                                lastPostAt = parsed.lastPostAt,
                                isFull = parsed.isFull,
                                isHidden = false,
                                isWatched = false,
                                isRead = false,
                                firstSeenAt = now,
                                lastSeenAt = now
                            )
                            topicsToUpsert.add(newTopic)
                            if (forum.isBootstrapped) {
                                newTopics.add(newTopic)
                            }
                        } else {
                            var updatedTopic = existing.copy(
                                title = parsed.title,
                                url = parsed.url,
                                author = parsed.author,
                                createdAt = parsed.createdAt,
                                replyCount = parsed.replyCount,
                                lastPostAuthor = parsed.lastPostAuthor,
                                lastPostAt = parsed.lastPostAt,
                                isFull = parsed.isFull,
                                lastSeenAt = now
                            )

                            if (parsed.replyCount > existing.replyCount && existing.isWatched) {
                                updatedTopic = updatedTopic.copy(isRead = false)
                                newReplies.add(updatedTopic)
                            }
                            topicsToUpsert.add(updatedTopic)
                        }
                    }

                    topicRepository.upsertAll(topicsToUpsert)

                    if (!forum.isBootstrapped) {
                        forumRepository.markBootstrapped(forumId)
                    }

                    val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                    val purgedCount = topicRepository.deleteStale(forumId, thirtyDaysAgo)

                    forumRepository.updateSyncState(forumId, true, now, null)

                    if (newTopics.isNotEmpty()) {
                        notifier.notifyNewTopics(forum, newTopics)
                    }
                    if (newReplies.isNotEmpty()) {
                        notifier.notifyNewReplies(forum, newReplies)
                    }

                    SyncOutcome(
                        forumId = forumId,
                        status = SyncStatus.SUCCESS,
                        newTopics = newTopics,
                        newReplies = newReplies,
                        parsedCount = parseResult.topics.size,
                        insertedCount = topicsToUpsert.count { it.id !in existingTopics },
                        updatedCount = topicsToUpsert.count { it.id in existingTopics },
                        purgedCount = purgedCount
                    )
                }
            }
        }
    }
}

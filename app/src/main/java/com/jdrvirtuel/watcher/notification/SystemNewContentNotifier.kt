package com.jdrvirtuel.watcher.notification

import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.SyncHighlights
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.NewContentNotifier
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemNewContentNotifier @Inject constructor(
    private val appNotifier: AppNotifier,
    private val appPreferences: AppPreferences
) : NewContentNotifier {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun notifyNewTopics(forum: Forum, topics: List<Topic>) {
        appNotifier.notifyNewTopics(forum, topics)
        
        val currentHighlights = getCurrentHighlights()
        val newTopicsMap = currentHighlights.newTopicsByForum.toMutableMap()
        val forumIdKey = forum.id.toString()
        val existingTitles = newTopicsMap[forumIdKey] ?: emptyList()
        newTopicsMap[forumIdKey] = existingTitles + topics.map { it.title }
        
        saveHighlights(currentHighlights.copy(newTopicsByForum = newTopicsMap))
    }

    override suspend fun notifyNewReplies(forum: Forum, topics: List<Pair<Topic, Int>>) {
        appNotifier.notifyNewReplies(forum, topics.map { it.first })
        
        val currentHighlights = getCurrentHighlights()
        val newReplyCounts = currentHighlights.newReplyCountByForum.toMutableMap()
        val forumIdKey = forum.id.toString()
        val totalDiff = topics.sumOf { it.second }
        val existingCount = newReplyCounts[forumIdKey] ?: 0
        newReplyCounts[forumIdKey] = existingCount + totalDiff
        
        saveHighlights(currentHighlights.copy(newReplyCountByForum = newReplyCounts))
    }

    override suspend fun clearHighlights() {
        appPreferences.setLastSyncHighlights(null)
    }

    private suspend fun getCurrentHighlights(): SyncHighlights {
        val serialized = appPreferences.lastSyncHighlights.first() ?: return SyncHighlights()
        return try {
            json.decodeFromString<SyncHighlights>(serialized)
        } catch (e: Exception) {
            SyncHighlights()
        }
    }

    private suspend fun saveHighlights(highlights: SyncHighlights) {
        val serialized = json.encodeToString(SyncHighlights.serializer(), highlights)
        appPreferences.setLastSyncHighlights(serialized)
    }
}

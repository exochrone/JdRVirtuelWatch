package com.jdrvirtuel.watcher.data.repository

import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.NewContentNotifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpNewContentNotifier @Inject constructor() : NewContentNotifier {
    override suspend fun notifyNewTopics(forum: Forum, topics: List<Topic>) {
        // No-op
    }

    override suspend fun notifyNewReplies(forum: Forum, topics: List<Topic>) {
        // No-op
    }
}

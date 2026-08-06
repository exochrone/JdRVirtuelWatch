package com.jdrvirtuel.watcher.domain.repository

import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.Topic

interface NewContentNotifier {
    suspend fun notifyNewTopics(forum: Forum, topics: List<Topic>)
    suspend fun notifyNewReplies(forum: Forum, topics: List<Topic>)
}

package com.jdrvirtuel.watcher.notification

import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.NewContentNotifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemNewContentNotifier @Inject constructor(
    private val appNotifier: AppNotifier
) : NewContentNotifier {

    override suspend fun notifyNewTopics(forum: Forum, topics: List<Topic>) {
        appNotifier.notifyNewTopics(forum, topics)
    }

    override suspend fun notifyNewReplies(forum: Forum, topics: List<Topic>) {
        appNotifier.notifyNewReplies(forum, topics)
    }
}

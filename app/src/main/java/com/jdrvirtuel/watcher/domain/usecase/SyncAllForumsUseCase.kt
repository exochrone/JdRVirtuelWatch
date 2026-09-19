package com.jdrvirtuel.watcher.domain.usecase

import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncAllForumsUseCase @Inject constructor(
    private val syncForumUseCase: SyncForumUseCase,
    private val notifier: com.jdrvirtuel.watcher.domain.repository.NewContentNotifier
) {
    suspend operator fun invoke(): List<SyncOutcome> {
        notifier.clearHighlights()
        val outcomes = mutableListOf<SyncOutcome>()
        // Synchronise les forums l'un après l'autre (15 puis 16)
        outcomes.add(syncForumUseCase(15, clearHighlights = false))
        delay(3000)
        outcomes.add(syncForumUseCase(16, clearHighlights = false))
        return outcomes
    }
}

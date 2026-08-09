package com.jdrvirtuel.watcher.domain.usecase

import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncAllForumsUseCase @Inject constructor(
    private val syncForumUseCase: SyncForumUseCase
) {
    suspend operator fun invoke(): List<SyncOutcome> {
        val outcomes = mutableListOf<SyncOutcome>()
        // Synchronise les forums l'un après l'autre (15 puis 16)
        outcomes.add(syncForumUseCase(15))
        delay(3000)
        outcomes.add(syncForumUseCase(16))
        return outcomes
    }
}

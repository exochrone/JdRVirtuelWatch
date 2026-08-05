package com.jdrvirtuel.watcher.domain.model

sealed interface FetchResult {
    data class Success(val html: String) : FetchResult
    data object ChallengeRequired : FetchResult
    data class Error(val message: String) : FetchResult
}

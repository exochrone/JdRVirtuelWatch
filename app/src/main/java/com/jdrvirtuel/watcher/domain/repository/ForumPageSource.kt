package com.jdrvirtuel.watcher.domain.repository

import com.jdrvirtuel.watcher.domain.model.FetchResult

interface ForumPageSource {
    suspend fun fetchHtml(url: String): FetchResult
}

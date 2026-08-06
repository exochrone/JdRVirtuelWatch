package com.jdrvirtuel.watcher.feature.debug

import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.ParseResult
import com.jdrvirtuel.watcher.domain.model.Topic

data class DebugUiState(
    val forums: List<Forum> = emptyList(),
    val topicsByForum: Map<Int, List<Topic>> = emptyMap(),
    val totalTopics: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    // Network Debug
    val isNetworkLoading: Boolean = false,
    val fetchResult: String? = null,
    val htmlContent: String? = null,
    val htmlSize: Int = 0,

    // Parser Debug
    val parseResult: ParseResult? = null
)

sealed interface DebugEvent {
    data object InsertTestTopic : DebugEvent
    data object ClearTopics : DebugEvent
    data object BackClicked : DebugEvent
    
    // Network Debug
    data class FetchForumHtml(val forumId: Int) : DebugEvent
    data class CopyToClipboard(val text: String) : DebugEvent

    // Parser Debug
    data object ParseTestFile : DebugEvent
    data object ParseLastLoadedHtml : DebugEvent
}

sealed interface DebugEffect {
    data object NavigateBack : DebugEffect
    data class CopyToClipboard(val text: String) : DebugEffect
}

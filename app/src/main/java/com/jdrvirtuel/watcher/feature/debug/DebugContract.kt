package com.jdrvirtuel.watcher.feature.debug

import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.Topic

data class DebugUiState(
    val forums: List<Forum> = emptyList(),
    val topicsByForum: Map<Int, List<Topic>> = emptyMap(),
    val totalTopics: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface DebugEvent {
    data object InsertTestTopic : DebugEvent
    data object ClearTopics : DebugEvent
    data object BackClicked : DebugEvent
}

sealed interface DebugEffect {
    data object NavigateBack : DebugEffect
}

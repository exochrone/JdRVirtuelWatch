package com.jdrvirtuel.watcher.feature.forumdetail

import androidx.compose.runtime.Immutable

@Immutable
data class ForumDetailUiState(
    val forumName: String = "",
    val topics: List<TopicUiModel> = emptyList(),
    val showHidden: Boolean = false,
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@Immutable
data class TopicUiModel(
    val id: Int,
    val title: String,
    val url: String,
    val author: String,
    val createdAtLabel: String,
    val replyCount: Int,
    val lastPostAuthor: String,
    val lastPostAtLabel: String,
    val isFull: Boolean,
    val isHidden: Boolean,
    val isWatched: Boolean,
    val isRead: Boolean
)

sealed interface ForumDetailEvent {
    data class OnTopicClick(val topic: TopicUiModel) : ForumDetailEvent
    data class OnToggleHidden(val topicId: Int) : ForumDetailEvent
    data class OnToggleWatched(val topicId: Int) : ForumDetailEvent
    data class OnUndoHide(val topicId: Int, val wasWatched: Boolean) : ForumDetailEvent
    data object OnToggleShowHidden : ForumDetailEvent
    data object OnRefresh : ForumDetailEvent
    data object OnBack : ForumDetailEvent
}

sealed interface ForumDetailEffect {
    data class OpenUrl(val url: String) : ForumDetailEffect
    data class ShowMessage(val message: String) : ForumDetailEffect
    data class ShowUndoHide(val topicId: Int, val wasWatched: Boolean) : ForumDetailEffect
    data object NavigateBack : ForumDetailEffect
}

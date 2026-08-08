package com.jdrvirtuel.watcher.feature.home

data class HomeUiState(
    val forums: List<ForumUiModel> = emptyList(),
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val consecutiveFailures: Int = 0
)

data class ForumUiModel(
    val id: Int,
    val name: String,
    val topicCount: Int,
    val unreadCount: Int,
    val lastSyncAt: Long?,
    val hasSyncError: Boolean
)

sealed interface HomeEvent {
    data class OnForumClick(val forumId: Int) : HomeEvent
    data object OnRefresh : HomeEvent
    data object OnVerificationClick : HomeEvent
}

sealed interface HomeEffect {
    data class NavigateToForum(val forumId: Int) : HomeEffect
    data object NavigateToVerification : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}

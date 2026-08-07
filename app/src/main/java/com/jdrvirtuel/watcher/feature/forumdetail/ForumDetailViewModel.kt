package com.jdrvirtuel.watcher.feature.forumdetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.util.DateFormatter
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import com.jdrvirtuel.watcher.domain.usecase.SyncForumUseCase
import com.jdrvirtuel.watcher.navigation.ForumDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val syncForumUseCase: SyncForumUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ForumDetailRoute>()
    private val forumId = route.forumId

    private val _showHidden = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)

    private val _effect = Channel<ForumDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val uiState: StateFlow<ForumDetailUiState> = combine(
        forumRepository.observeForum(forumId),
        topicRepository.observeTopics(forumId),
        _showHidden,
        _isSyncing
    ) { forum, topics, showHidden, isSyncing ->
        if (forum == null) {
            ForumDetailUiState(errorMessage = "Forum introuvable", isLoading = false)
        } else {
            val allHidden = topics.isNotEmpty() && topics.all { it.isHidden }
            val isEmpty = topics.isEmpty()

            val filteredTopics = topics
                .filter { showHidden || !it.isHidden }
                .sortedWith(
                    compareByDescending<Topic> { it.isWatched }
                        .thenByDescending { it.lastPostAt }
                )
                .map { it.toUiModel() }

            ForumDetailUiState(
                forumName = forum.name,
                topics = filteredTopics,
                showHidden = showHidden,
                isSyncing = isSyncing,
                isLoading = false,
                errorMessage = when {
                    isEmpty -> context.getString(R.string.forum_detail_empty)
                    !showHidden && allHidden -> context.getString(R.string.forum_detail_all_hidden)
                    else -> null
                }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ForumDetailUiState()
    )

    fun onEvent(event: ForumDetailEvent) {
        when (event) {
            is ForumDetailEvent.OnTopicClick -> openTopic(event.topic)
            is ForumDetailEvent.OnToggleHidden -> toggleHidden(event.topicId)
            is ForumDetailEvent.OnToggleWatched -> toggleWatched(event.topicId)
            is ForumDetailEvent.OnUndoHide -> undoHide(event.topicId, event.wasWatched)
            ForumDetailEvent.OnToggleShowHidden -> _showHidden.update { !it }
            ForumDetailEvent.OnRefresh -> refresh()
            ForumDetailEvent.OnBack -> viewModelScope.launch { _effect.send(ForumDetailEffect.NavigateBack) }
        }
    }

    private fun openTopic(topic: TopicUiModel) {
        viewModelScope.launch {
            topicRepository.setRead(topic.id, true)
            _effect.send(ForumDetailEffect.OpenUrl(topic.url))
        }
    }

    private fun toggleHidden(topicId: Int) {
        viewModelScope.launch {
            val topics = uiState.value.topics
            val topic = topics.find { it.id == topicId } ?: return@launch
            val wasWatched = topic.isWatched
            
            if (!topic.isHidden) {
                // Masquer
                topicRepository.setWatched(topicId, false)
                topicRepository.setHidden(topicId, true)
                _effect.send(ForumDetailEffect.ShowUndoHide(topicId, wasWatched))
            } else {
                // Réafficher
                topicRepository.setHidden(topicId, false)
            }
        }
    }

    private fun toggleWatched(topicId: Int) {
        viewModelScope.launch {
            val topics = uiState.value.topics
            val topic = topics.find { it.id == topicId } ?: return@launch
            
            if (!topic.isHidden) {
                val newWatched = !topic.isWatched
                topicRepository.setWatched(topicId, newWatched)
                val message = if (newWatched) {
                    context.getString(R.string.forum_detail_watch_enabled)
                } else {
                    context.getString(R.string.forum_detail_watch_disabled)
                }
                _effect.send(ForumDetailEffect.ShowMessage(message))
            }
        }
    }

    private fun undoHide(topicId: Int, wasWatched: Boolean) {
        viewModelScope.launch {
            topicRepository.setHidden(topicId, false)
            if (wasWatched) {
                topicRepository.setWatched(topicId, true)
            }
        }
    }

    private fun refresh() {
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val outcome = syncForumUseCase(forumId)
                val message = when (outcome.status) {
                    SyncStatus.SUCCESS -> {
                        if (outcome.newTopics.isEmpty()) context.getString(R.string.home_sync_success_no_news)
                        else if (outcome.newTopics.size == 1) context.getString(R.string.home_sync_success_news_singular)
                        else context.getString(R.string.home_sync_success_news_plural, outcome.newTopics.size)
                    }
                    SyncStatus.CHALLENGE_REQUIRED -> context.getString(R.string.home_sync_challenge)
                    SyncStatus.ERROR -> outcome.errorMessage ?: context.getString(R.string.home_sync_failed)
                }
                _effect.send(ForumDetailEffect.ShowMessage(message))
            } catch (e: Exception) {
                _effect.send(ForumDetailEffect.ShowMessage(context.getString(R.string.home_sync_unexpected_error)))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun Topic.toUiModel() = TopicUiModel(
        id = id,
        title = title,
        url = url,
        author = author,
        createdAtLabel = DateFormatter.formatRelative(createdAt),
        replyCount = replyCount,
        lastPostAuthor = lastPostAuthor,
        lastPostAtLabel = DateFormatter.formatRelative(lastPostAt),
        isFull = isFull,
        isHidden = isHidden,
        isWatched = isWatched,
        isRead = isRead
    )
}

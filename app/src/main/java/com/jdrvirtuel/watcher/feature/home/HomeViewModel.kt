package com.jdrvirtuel.watcher.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import com.jdrvirtuel.watcher.domain.usecase.SyncAllForumsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val syncAllForumsUseCase: SyncAllForumsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val isSyncing = MutableStateFlow(false)

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        forumRepository.observeForums().flatMapLatest { forums ->
            if (forums.isEmpty()) {
                flowOf(emptyList<ForumUiModel>())
            } else {
                val forumUiModelsFlows = forums.map { forum ->
                    combine(
                        topicRepository.observeTopicCount(forum.id),
                        topicRepository.observeUnreadCount(forum.id)
                    ) { topicCount, unreadCount ->
                        ForumUiModel(
                            id = forum.id,
                            name = forum.name,
                            topicCount = topicCount,
                            unreadCount = unreadCount,
                            lastSyncAt = forum.lastSyncAt,
                            hasSyncError = !forum.lastSyncSuccess
                        )
                    }
                }
                combine(forumUiModelsFlows) { it.toList() }
            }
        },
        isSyncing
    ) { forums, syncing ->
        HomeUiState(
            forums = forums,
            isSyncing = syncing,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnForumClick -> {
                viewModelScope.launch {
                    _effects.send(HomeEffect.NavigateToForum(event.forumId))
                }
            }
            HomeEvent.OnRefresh -> sync()
            HomeEvent.OnDebugClick -> {
                viewModelScope.launch {
                    _effects.send(HomeEffect.NavigateToDebug)
                }
            }
        }
    }

    private fun sync() {
        if (isSyncing.value) return

        viewModelScope.launch {
            isSyncing.value = true
            try {
                val outcomes = syncAllForumsUseCase()
                handleSyncOutcomes(outcomes)
            } catch (e: Exception) {
                _effects.send(HomeEffect.ShowMessage(context.getString(R.string.home_sync_unexpected_error)))
            } finally {
                isSyncing.value = false
            }
        }
    }

    private suspend fun handleSyncOutcomes(outcomes: List<SyncOutcome>) {
        val totalNewTopics = outcomes.sumOf { it.newTopics.size }
        val hasError = outcomes.any { it.status == SyncStatus.ERROR }
        val hasChallenge = outcomes.any { it.status == SyncStatus.CHALLENGE_REQUIRED }

        val message = when {
            hasChallenge -> context.getString(R.string.home_sync_challenge)
            hasError -> context.getString(R.string.home_sync_failed)
            totalNewTopics == 0 -> context.getString(R.string.home_sync_success_no_news)
            totalNewTopics == 1 -> context.getString(R.string.home_sync_success_news_singular)
            else -> context.getString(R.string.home_sync_success_news_plural, totalNewTopics)
        }
        _effects.send(HomeEffect.ShowMessage(message))
    }
}

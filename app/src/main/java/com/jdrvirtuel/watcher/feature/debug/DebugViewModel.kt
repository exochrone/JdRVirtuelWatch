package com.jdrvirtuel.watcher.feature.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrvirtuel.watcher.domain.model.FetchResult
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.ForumPageSource
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlin.random.Random

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val forumPageSource: ForumPageSource
) : ViewModel() {

    private val _effect = Channel<DebugEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _networkState = MutableStateFlow(NetworkDebugState())

    val uiState: StateFlow<DebugUiState> = combine(
        forumRepository.observeForums(),
        topicRepository.observeTopics(15),
        topicRepository.observeTopics(16),
        topicRepository.observeTotalCount(),
        _networkState
    ) { forums, topics15, topics16, totalCount, network ->
        DebugUiState(
            forums = forums,
            topicsByForum = mapOf(15 to topics15, 16 to topics16),
            totalTopics = totalCount,
            isNetworkLoading = network.isLoading,
            fetchResult = network.resultMessage,
            htmlContent = network.html,
            htmlSize = network.html?.length ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebugUiState(isLoading = true)
    )

    fun onEvent(event: DebugEvent) {
        when (event) {
            DebugEvent.InsertTestTopic -> insertTestTopic()
            DebugEvent.ClearTopics -> clearTopics()
            DebugEvent.BackClicked -> {
                viewModelScope.launch {
                    _effect.send(DebugEffect.NavigateBack)
                }
            }
            is DebugEvent.FetchForumHtml -> fetchForumHtml(event.forumId)
            is DebugEvent.CopyToClipboard -> {
                viewModelScope.launch {
                    _effect.send(DebugEffect.CopyToClipboard(event.text))
                }
            }
        }
    }

    private fun fetchForumHtml(forumId: Int) {
        if (_networkState.value.isLoading) return

        viewModelScope.launch {
            _networkState.update { it.copy(isLoading = true, resultMessage = null, html = null) }
            val url = "https://www.jdrvirtuel.com/viewforum.php?f=$forumId"
            val result = forumPageSource.fetchHtml(url)
            
            _networkState.update { state ->
                when (result) {
                    is FetchResult.Success -> state.copy(
                        isLoading = false,
                        resultMessage = "Succès",
                        html = result.html
                    )
                    FetchResult.ChallengeRequired -> state.copy(
                        isLoading = false,
                        resultMessage = "Vérification requise"
                    )
                    is FetchResult.Error -> state.copy(
                        isLoading = false,
                        resultMessage = "Erreur : ${result.message}"
                    )
                }
            }
        }
    }

    private data class NetworkDebugState(
        val isLoading: Boolean = false,
        val resultMessage: String? = null,
        val html: String? = null
    )

    private fun insertTestTopic() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = Random.nextInt(900000, 1000000)
            val topic = Topic(
                id = id,
                forumId = 15,
                title = "Sujet de test $id",
                url = "https://www.jdrvirtuel.com/viewtopic.php?t=$id",
                author = "TestAuthor",
                createdAt = now,
                replyCount = Random.nextInt(0, 50),
                lastPostAuthor = "TestAuthor",
                lastPostAt = now,
                firstSeenAt = now,
                lastSeenAt = now
            )
            topicRepository.upsertAll(listOf(topic))
        }
    }

    private fun clearTopics() {
        viewModelScope.launch {
            topicRepository.deleteAll()
        }
    }
}

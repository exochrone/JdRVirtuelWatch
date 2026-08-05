package com.jdrvirtuel.watcher.feature.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _effect = Channel<DebugEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val uiState: StateFlow<DebugUiState> = combine(
        forumRepository.observeForums(),
        topicRepository.observeTopics(15),
        topicRepository.observeTopics(16),
        topicRepository.observeTotalCount()
    ) { forums, topics15, topics16, totalCount ->
        DebugUiState(
            forums = forums,
            topicsByForum = mapOf(15 to topics15, 16 to topics16),
            totalTopics = totalCount
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
        }
    }

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

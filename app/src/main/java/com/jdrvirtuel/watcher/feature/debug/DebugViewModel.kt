package com.jdrvirtuel.watcher.feature.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrvirtuel.watcher.data.parser.TopicListParser
import com.jdrvirtuel.watcher.domain.model.FetchResult
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.ParseResult
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.ForumPageSource
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import com.jdrvirtuel.watcher.domain.usecase.SyncAllForumsUseCase
import com.jdrvirtuel.watcher.domain.usecase.SyncForumUseCase
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val forumPageSource: ForumPageSource,
    private val parser: TopicListParser,
    private val syncForumUseCase: SyncForumUseCase,
    private val syncAllForumsUseCase: SyncAllForumsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _effect = Channel<DebugEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _networkState = MutableStateFlow(NetworkDebugState())
    private val _parserState = MutableStateFlow(ParserDebugState())
    private val _syncState = MutableStateFlow(SyncDebugState())
    private val _benchState = MutableStateFlow(BenchDebugState())

    private var benchJob: Job? = null
    private var benchStartTime: Long = 0

    val uiState: StateFlow<DebugUiState> = combine(
        forumRepository.observeForums(),
        topicRepository.observeTopics(15),
        topicRepository.observeTopics(16),
        topicRepository.observeTotalCount(),
        _networkState,
        _parserState,
        _syncState,
        _benchState
    ) { array ->
        val forums = array[0] as List<Forum>
        val topics15 = array[1] as List<Topic>
        val topics16 = array[2] as List<Topic>
        val totalCount = array[3] as Int
        val network = array[4] as NetworkDebugState
        val parserState = array[5] as ParserDebugState
        val sync = array[6] as SyncDebugState
        val bench = array[7] as BenchDebugState

        DebugUiState(
            forums = forums,
            topicsByForum = mapOf(15 to topics15, 16 to topics16),
            totalTopics = totalCount,
            isNetworkLoading = network.isLoading,
            fetchResult = network.resultMessage,
            htmlContent = network.html,
            htmlSize = network.html?.length ?: 0,
            parseResult = parserState.result,
            isSyncing = sync.isSyncing,
            lastSyncOutcome = sync.lastOutcome,
            isBenchRunning = bench.isRunning,
            benchIntervalMinutes = bench.intervalMinutes,
            benchLogs = bench.logs
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
            DebugEvent.ParseTestFile -> parseTestFile()
            DebugEvent.ParseLastLoadedHtml -> parseLastLoadedHtml()
            is DebugEvent.SyncForum -> syncForum(event.forumId)
            DebugEvent.SyncAll -> syncAll()
            DebugEvent.DeleteRandomTopic -> deleteRandomTopic()
            DebugEvent.DecrementReplyCount -> decrementReplyCount()
            DebugEvent.ResetBootstrap -> resetBootstrap()
            is DebugEvent.UpdateBenchInterval -> {
                _benchState.update { it.copy(intervalMinutes = event.minutes) }
            }
            DebugEvent.StartBench -> startBench()
            DebugEvent.StopBench -> stopBench()
            DebugEvent.ClearBenchLogs -> {
                _benchState.update { it.copy(logs = emptyList()) }
            }
            DebugEvent.CopyBenchLogs -> copyBenchLogs()
        }
    }

    private fun startBench() {
        if (_benchState.value.isRunning) return
        benchStartTime = System.currentTimeMillis()
        _benchState.update { it.copy(isRunning = true) }
        benchJob = viewModelScope.launch {
            while (true) {
                runBenchIteration()
                delay(_benchState.value.intervalMinutes * 60 * 1000L)
            }
        }
    }

    private fun stopBench() {
        benchJob?.cancel()
        benchJob = null
        _benchState.update { it.copy(isRunning = false) }
    }

    private suspend fun runBenchIteration() {
        val now = System.currentTimeMillis()
        val url = "https://www.jdrvirtuel.com/viewforum.php?f=15"
        val result = forumPageSource.fetchHtml(url)
        
        val entry = when (result) {
            is FetchResult.Success -> BenchEntry(
                timestamp = now,
                timeSinceStartMs = now - benchStartTime,
                result = "Succès",
                htmlSize = result.html.length
            )
            FetchResult.ChallengeRequired -> BenchEntry(
                timestamp = now,
                timeSinceStartMs = now - benchStartTime,
                result = "Vérification requise"
            )
            is FetchResult.Error -> BenchEntry(
                timestamp = now,
                timeSinceStartMs = now - benchStartTime,
                result = "Erreur : ${result.message}"
            )
        }
        
        _benchState.update { it.copy(logs = listOf(entry) + it.logs) }
    }

    private fun copyBenchLogs() {
        val logs = _benchState.value.logs
        if (logs.isEmpty()) return
        
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        logs.forEach { entry ->
            val elapsed = entry.timeSinceStartMs / 1000
            val min = elapsed / 60
            val sec = elapsed % 60
            sb.append("${dateFormat.format(Date(entry.timestamp))} | +${min}m${sec}s | ${entry.result}")
            if (entry.htmlSize != null) sb.append(" | ${entry.htmlSize} octets")
            sb.append("\n")
        }
        
        viewModelScope.launch {
            _effect.send(DebugEffect.CopyToClipboard(sb.toString()))
        }
    }

    private fun syncForum(forumId: Int) {
        if (_syncState.value.isSyncing) return
        viewModelScope.launch {
            _syncState.update { it.copy(isSyncing = true) }
            val outcome = syncForumUseCase(forumId)
            _syncState.update { it.copy(isSyncing = false, lastOutcome = outcome) }
        }
    }

    private fun syncAll() {
        if (_syncState.value.isSyncing) return
        viewModelScope.launch {
            _syncState.update { it.copy(isSyncing = true) }
            val outcomes = syncAllForumsUseCase()
            _syncState.update { it.copy(isSyncing = false, lastOutcome = outcomes.lastOrNull()) }
        }
    }

    private fun deleteRandomTopic() {
        viewModelScope.launch {
            val topics15 = topicRepository.getTopics(15)
            val topics16 = topicRepository.getTopics(16)
            val all = topics15 + topics16
            if (all.isNotEmpty()) {
                val random = all.random()
                topicRepository.deleteById(random.id)
            }
        }
    }

    private fun decrementReplyCount() {
        viewModelScope.launch {
            val topics15 = topicRepository.getTopics(15)
            val topics16 = topicRepository.getTopics(16)
            val watched = (topics15 + topics16).filter { it.isWatched }
            if (watched.isNotEmpty()) {
                val random = watched.random()
                if (random.replyCount > 0) {
                    topicRepository.upsertAll(listOf(random.copy(replyCount = random.replyCount - 1)))
                }
            }
        }
    }

    private fun resetBootstrap() {
        viewModelScope.launch {
            forumRepository.resetBootstrap(15)
            forumRepository.resetBootstrap(16)
        }
    }

    private fun parseTestFile() {
        viewModelScope.launch {
            try {
                val html = context.assets.open("viewforum_f15.html").bufferedReader().use { it.readText() }
                val result = parser.parse(html)
                _parserState.update { it.copy(result = result) }
            } catch (e: Exception) {
                // Silently fail or log in debug
            }
        }
    }

    private fun parseLastLoadedHtml() {
        val html = _networkState.value.html
        if (html != null) {
            val result = parser.parse(html)
            _parserState.update { it.copy(result = result) }
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

    private data class ParserDebugState(
        val result: ParseResult? = null
    )

    private data class SyncDebugState(
        val isSyncing: Boolean = false,
        val lastOutcome: SyncOutcome? = null
    )

    private data class BenchDebugState(
        val isRunning: Boolean = false,
        val intervalMinutes: Int = 5,
        val logs: List<BenchEntry> = emptyList()
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

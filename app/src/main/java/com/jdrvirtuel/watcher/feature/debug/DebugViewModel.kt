package com.jdrvirtuel.watcher.feature.debug

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.jdrvirtuel.watcher.core.util.BrowserLauncher
import com.jdrvirtuel.watcher.core.util.DateFormatter
import com.jdrvirtuel.watcher.core.util.LogExporter
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.data.parser.TopicListParser
import com.jdrvirtuel.watcher.domain.model.FetchResult
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.ParseResult
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.repository.ChallengeStateRepository
import com.jdrvirtuel.watcher.domain.repository.ForumPageSource
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import com.jdrvirtuel.watcher.domain.usecase.SyncAllForumsUseCase
import com.jdrvirtuel.watcher.domain.usecase.SyncForumUseCase
import com.jdrvirtuel.watcher.work.SyncScheduler
import com.jdrvirtuel.watcher.work.TestModeLog
import android.webkit.CookieManager
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import com.jdrvirtuel.watcher.domain.model.SyncSource
import com.jdrvirtuel.watcher.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private val challengeRepository: ChallengeStateRepository,
    val appPreferences: AppPreferences,
    private val workManager: WorkManager,
    private val syncScheduler: SyncScheduler,
    private val testModeLog: TestModeLog,
    private val logExporter: LogExporter,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _effect = Channel<DebugEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _networkState = MutableStateFlow(NetworkDebugState())
    private val _parserState = MutableStateFlow(ParserDebugState())
    private val _syncState = MutableStateFlow(SyncDebugState())
    private val _benchState = MutableStateFlow(BenchDebugState())
    private val _browserState = MutableStateFlow(BrowserDebugState())

    private val periodicWorkInfo = workManager.getWorkInfosForUniqueWorkFlow("periodic_sync")
        .map { it.firstOrNull() }

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
        _benchState,
        _browserState,
        appPreferences.preferredBrowserPackage,
        periodicWorkInfo,
        appPreferences.isTestModeEnabled,
        appPreferences.testModeIntervalMinutes,
        appPreferences.testModeLog,
        appPreferences.syncLog,
        challengeRepository.consecutiveFailures,
        challengeRepository.lastPromptAt,
        appPreferences.simulateChallenge
    ) { array ->
        val forums = array[0] as List<Forum>
        val topics15 = array[1] as List<Topic>
        val topics16 = array[2] as List<Topic>
        val totalCount = array[3] as Int
        val network = array[4] as NetworkDebugState
        val parserState = array[5] as ParserDebugState
        val sync = array[6] as SyncDebugState
        val bench = array[7] as BenchDebugState
        val browser = array[8] as BrowserDebugState
        val preferredBrowser = array[9] as String?
        val workInfo = array[10] as WorkInfo?
        val testEnabled = array[11] as Boolean
        val testInterval = array[12] as Int
        val testLogRaw = array[13] as String?
        val syncLogRaw = array[14] as String?
        val failures = array[15] as Int
        val lastPrompt = array[16] as Long
        val simulateChallenge = array[17] as Boolean

        val workState = workInfo?.state?.name
        val workPeriod = workInfo?.periodicityInfo?.repeatIntervalMillis?.let { it / 60000 }

        val testLog = if (testLogRaw != null) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<com.jdrvirtuel.watcher.work.TestModeEntry>>(testLogRaw)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val syncLog = if (syncLogRaw != null) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<com.jdrvirtuel.watcher.domain.model.SyncLogEntry>>(syncLogRaw)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

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
            lastDeletedTopicInfo = sync.lastDeletedTopicInfo,
            selectedTopicId = sync.selectedTopicId,
            isBenchRunning = bench.isRunning,
            benchIntervalMinutes = bench.intervalMinutes,
            benchLogs = bench.logs,
            ctCompatiblePackages = browser.ctCompatiblePackages,
            installedBrowserPackages = browser.installedBrowserPackages,
            preferredBrowserPackage = preferredBrowser,
            lastBrowserTestResult = browser.lastTestResult,
            workInfoState = workState,
            workPeriodMinutes = workPeriod,
            testModeEnabled = testEnabled,
            testModeIntervalMinutes = testInterval,
            testModeLog = testLog,
            syncLog = syncLog,
            consecutiveFailures = failures,
            lastPromptAt = lastPrompt,
            simulateChallenge = simulateChallenge
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
            is DebugEvent.SelectTopic -> {
                _syncState.update { it.copy(selectedTopicId = event.topicId) }
            }
            DebugEvent.ToggleWatched -> toggleWatched()
            DebugEvent.ToggleHidden -> toggleHidden()
            DebugEvent.ToggleRead -> toggleRead()
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
            DebugEvent.ExportBenchLogs -> exportBenchLogs()
            DebugEvent.CopyBenchLogs -> copyBenchLogs()
            DebugEvent.RefreshBrowserInfo -> refreshBrowserInfo()
            is DebugEvent.SetPreferredBrowser -> {
                viewModelScope.launch {
                    appPreferences.setPreferredBrowserPackage(event.packageName)
                }
            }
            DebugEvent.TestBrowserLauncher -> testBrowserLauncher()
            DebugEvent.TestBraveCustomTabs -> testBraveCustomTabs()
            DebugEvent.TestActionViewSimple -> testActionViewSimple()
            DebugEvent.TriggerImmediateSync -> syncScheduler.triggerImmediateSync()
            DebugEvent.ReschedulePeriodicSync -> syncScheduler.reschedulePeriodicSync()
            is DebugEvent.UpdateTestModeInterval -> {
                viewModelScope.launch { appPreferences.setTestModeIntervalMinutes(event.minutes) }
            }
            DebugEvent.StartTestMode -> {
                viewModelScope.launch {
                    syncScheduler.startTestMode(uiState.value.testModeIntervalMinutes)
                }
            }
            DebugEvent.StopTestMode -> {
                viewModelScope.launch {
                    syncScheduler.stopTestMode()
                }
            }
            DebugEvent.ClearTestModeLog -> {
                viewModelScope.launch {
                    testModeLog.clear()
                }
            }
            DebugEvent.ExportTestModeLog -> {
                val logs = uiState.value.testModeLog
                if (logs.isNotEmpty()) {
                    logExporter.export(logs.joinToString("\n") { it.format() }, "Test Mode Log")
                }
            }
            DebugEvent.ExportSyncLog -> {
                val logs = uiState.value.syncLog
                if (logs.isNotEmpty()) {
                    logExporter.export(formatSyncLogs(logs), "Sync Log")
                }
            }
            DebugEvent.ClearCookies -> {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            DebugEvent.SimulateThreeFailures -> {
                viewModelScope.launch {
                    challengeRepository.incrementFailures()
                    challengeRepository.incrementFailures()
                    challengeRepository.incrementFailures()
                }
            }
            DebugEvent.ResetFailures -> {
                viewModelScope.launch {
                    challengeRepository.resetFailures()
                }
            }
            is DebugEvent.ToggleSimulateChallenge -> {
                viewModelScope.launch {
                    appPreferences.setSimulateChallenge(event.enabled)
                }
            }
        }
    }

    private fun refreshBrowserInfo() {
        val launcher = BrowserLauncher(context, appPreferences, viewModelScope)
        val ctPackages = launcher.listCustomTabsBrowsers().map {
            BrowserPackageUiModel(it.packageName, it.label)
        }
        
        Log.d("CTDEBUG", "CT Compatible: ${ctPackages.joinToString(", ") { it.packageName }.ifEmpty { "None" }}")
        
        val pm = context.packageManager
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val browsers = pm.queryIntentActivities(browserIntent, PackageManager.MATCH_ALL)
        val browserPackages = browsers.map { it.activityInfo.packageName }
        
        _browserState.update { it.copy(
            ctCompatiblePackages = ctPackages,
            installedBrowserPackages = browserPackages
        ) }
    }

    private val testUrl = "https://www.jdrvirtuel.com/viewtopic.php?f=15&t=41234"

    private fun testBrowserLauncher() {
        val launcher = BrowserLauncher(context, appPreferences, viewModelScope)
        launcher.openUrl(testUrl) { success ->
            _browserState.update { it.copy(lastTestResult = if (success) "Succès (BrowserLauncher)" else "Échec (BrowserLauncher)") }
        }
    }

    private fun testBraveCustomTabs() {
        try {
            val intent = CustomTabsIntent.Builder().build().intent
            intent.setPackage("com.brave.browser")
            intent.data = Uri.parse(testUrl)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            _browserState.update { it.copy(lastTestResult = "Succès (Brave CT)") }
        } catch (e: Exception) {
            _browserState.update { it.copy(lastTestResult = "Exception : ${e.message}") }
        }
    }

    private fun testActionViewSimple() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(testUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            _browserState.update { it.copy(lastTestResult = "Succès (ACTION_VIEW)") }
        } catch (e: Exception) {
            _browserState.update { it.copy(lastTestResult = "Exception : ${e.message}") }
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
        
        val sb = formatBenchLogs(logs)
        
        viewModelScope.launch {
            _effect.send(DebugEffect.CopyToClipboard(sb))
        }
    }

    private fun exportBenchLogs() {
        val logs = _benchState.value.logs
        if (logs.isEmpty()) return
        logExporter.export(formatBenchLogs(logs), "Bench Logs")
    }

    private fun formatBenchLogs(logs: List<BenchEntry>): String {
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
        return sb.toString()
    }

    private fun formatSyncLogs(logs: List<com.jdrvirtuel.watcher.domain.model.SyncLogEntry>): String {
        return logs.joinToString("\n\n") { entry ->
            val timestamp = SimpleDateFormat("dd/MM/yy - HH:mm:ss", Locale.getDefault()).format(Date(entry.timestampMs))
            val source = when (entry.source) {
                SyncSource.MANUAL -> "Manuelle"
                SyncSource.PERIODIC -> "Automatique"
                SyncSource.TEST -> "Test"
            }
            val results = entry.forumResults.joinToString("\n") { res ->
                val status = when (res.status) {
                    SyncStatus.SUCCESS -> {
                        val parsed = context.resources.getQuantityString(R.plurals.sync_log_parsed, res.parsedCount, res.parsedCount)
                        val news = if (res.newTopicsCount > 0) context.resources.getQuantityString(R.plurals.sync_log_new, res.newTopicsCount, res.newTopicsCount) else null
                        val replies = if (res.newRepliesCount > 0) context.resources.getQuantityString(R.plurals.sync_log_replies, res.newRepliesCount, res.newRepliesCount) else null
                        
                        buildString {
                            append("Succès · ")
                            append(parsed)
                            if (news != null) append(", $news")
                            if (replies != null) append(", $replies")
                        }
                    }
                    SyncStatus.CHALLENGE_REQUIRED -> "Bloqué"
                    SyncStatus.ERROR -> "Erreur${if (res.errorMessage != null) " : ${res.errorMessage}" else ""}"
                }
                "   ${res.forumName} : $status"
            }
            "$timestamp · $source\n$results"
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
                _syncState.update { it.copy(lastDeletedTopicInfo = "${random.id} - ${random.title}") }
            }
        }
    }

    private fun toggleWatched() {
        viewModelScope.launch {
            val id = _syncState.value.selectedTopicId ?: return@launch
            val topics15 = topicRepository.getTopics(15)
            val topics16 = topicRepository.getTopics(16)
            val topic = (topics15 + topics16).find { it.id == id } ?: return@launch
            topicRepository.setWatched(id, !topic.isWatched)
        }
    }

    private fun toggleHidden() {
        viewModelScope.launch {
            val id = _syncState.value.selectedTopicId ?: return@launch
            val topics15 = topicRepository.getTopics(15)
            val topics16 = topicRepository.getTopics(16)
            val topic = (topics15 + topics16).find { it.id == id } ?: return@launch
            topicRepository.setHidden(id, !topic.isHidden)
        }
    }

    private fun toggleRead() {
        viewModelScope.launch {
            val id = _syncState.value.selectedTopicId ?: return@launch
            val topics15 = topicRepository.getTopics(15)
            val topics16 = topicRepository.getTopics(16)
            val topic = (topics15 + topics16).find { it.id == id } ?: return@launch
            topicRepository.setRead(id, !topic.isRead)
        }
    }

    private fun decrementReplyCount() {
        viewModelScope.launch {
            val id = _syncState.value.selectedTopicId ?: return@launch
            val topics15 = topicRepository.getTopics(15)
            val topics16 = topicRepository.getTopics(16)
            val topic = (topics15 + topics16).find { it.id == id } ?: return@launch
            if (topic.replyCount > 0) {
                topicRepository.upsertAll(listOf(topic.copy(replyCount = topic.replyCount - 1)))
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
        val lastOutcome: SyncOutcome? = null,
        val lastDeletedTopicInfo: String? = null,
        val selectedTopicId: Int? = null
    )

    private data class BenchDebugState(
        val isRunning: Boolean = false,
        val intervalMinutes: Int = 5,
        val logs: List<BenchEntry> = emptyList()
    )

    private data class BrowserDebugState(
        val ctCompatiblePackages: List<BrowserPackageUiModel> = emptyList(),
        val installedBrowserPackages: List<String> = emptyList(),
        val lastTestResult: String? = null
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

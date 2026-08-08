package com.jdrvirtuel.watcher.feature.debug

import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.ParseResult
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.Topic
import com.jdrvirtuel.watcher.domain.model.SyncLogEntry
import com.jdrvirtuel.watcher.work.TestModeEntry

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
    val parseResult: ParseResult? = null,

    // Sync Debug
    val isSyncing: Boolean = false,
    val lastSyncOutcome: SyncOutcome? = null,
    val lastDeletedTopicInfo: String? = null,
    val selectedTopicId: Int? = null,

    // Cloudflare Test Bench
    val isBenchRunning: Boolean = false,
    val benchIntervalMinutes: Int = 5,
    val benchLogs: List<BenchEntry> = emptyList(),

    // Browser Debug
    val ctCompatiblePackages: List<BrowserPackageUiModel> = emptyList(),
    val installedBrowserPackages: List<String> = emptyList(),
    val preferredBrowserPackage: String? = null,
    val lastBrowserTestResult: String? = null,

    // Background Task Debug
    val workInfoState: String? = null,
    val workPeriodMinutes: Long? = null,
    val testModeEnabled: Boolean = false,
    val testModeIntervalMinutes: Int = 2,
    val testModeLog: List<TestModeEntry> = emptyList(),
    val syncLog: List<SyncLogEntry> = emptyList(),

    // Cloudflare Debug
    val consecutiveFailures: Int = 0,
    val lastPromptAt: Long = 0L,
    val simulateChallenge: Boolean = false
)

data class BrowserPackageUiModel(
    val packageName: String,
    val label: String
)

data class BenchEntry(
    val timestamp: Long,
    val timeSinceStartMs: Long,
    val result: String,
    val htmlSize: Int? = null
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

    // Sync Debug
    data class SyncForum(val forumId: Int) : DebugEvent
    data object SyncAll : DebugEvent
    data object DeleteRandomTopic : DebugEvent
    data class SelectTopic(val topicId: Int?) : DebugEvent
    data object ToggleWatched : DebugEvent
    data object ToggleHidden : DebugEvent
    data object ToggleRead : DebugEvent
    data object DecrementReplyCount : DebugEvent
    data object ResetBootstrap : DebugEvent

    // Cloudflare Test Bench
    data class UpdateBenchInterval(val minutes: Int) : DebugEvent
    data object StartBench : DebugEvent
    data object StopBench : DebugEvent
    data object ClearBenchLogs : DebugEvent
    data object CopyBenchLogs : DebugEvent

    // Browser Debug
    data object RefreshBrowserInfo : DebugEvent
    data class SetPreferredBrowser(val packageName: String?) : DebugEvent
    data object TestBrowserLauncher : DebugEvent
    data object TestBraveCustomTabs : DebugEvent
    data object TestActionViewSimple : DebugEvent

    // Background Task Debug
    data object TriggerImmediateSync : DebugEvent
    data object ReschedulePeriodicSync : DebugEvent
    data class UpdateTestModeInterval(val minutes: Int) : DebugEvent
    data object StartTestMode : DebugEvent
    data object StopTestMode : DebugEvent
    data object ClearTestModeLog : DebugEvent

    // Cloudflare Debug
    data object ClearCookies : DebugEvent
    data object SimulateThreeFailures : DebugEvent
    data object ResetFailures : DebugEvent
    data class ToggleSimulateChallenge(val enabled: Boolean) : DebugEvent
}

sealed interface DebugEffect {
    data object NavigateBack : DebugEffect
    data class CopyToClipboard(val text: String) : DebugEffect
}

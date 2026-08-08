package com.jdrvirtuel.watcher.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrvirtuel.watcher.BuildConfig
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.util.BrowserLauncher
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import com.jdrvirtuel.watcher.work.SyncLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val appPreferences: AppPreferences,
    private val syncLog: SyncLog
) : ViewModel() {

    private val browserLauncher = BrowserLauncher(context, appPreferences, viewModelScope)
    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        forumRepository.observeForums(),
        appPreferences.consecutiveChallengeFailures,
        appPreferences.preferredBrowserPackage,
        topicRepository.observeTotalCount(),
        appPreferences.syncLog,
        _refreshTrigger
    ) { args ->
        val forums = args[0] as List<Forum>
        val failures = args[1] as Int
        val preferredBrowser = args[2] as String?
        val totalTopics = args[3] as Int
        
        val browsers = browserLauncher.listCustomTabsBrowsers().map {
            BrowserPackageInfo(it.packageName, it.label)
        }
        
        val syncLogs = syncLog.getEntries()

        SettingsUiState(
            isLoading = false,
            forums = forums,
            syncIntervalMinutes = if (failures >= 3) 60 else 15,
            preferredBrowserPackage = preferredBrowser,
            availableBrowsers = browsers,
            storedTopicsCount = totalTopics,
            syncLogs = syncLogs,
            appVersion = BuildConfig.VERSION_NAME
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isLoading = true)
    )

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.OnBack -> viewModelScope.launch { _effect.send(SettingsEffect.NavigateBack) }
            is SettingsEvent.OnBrowserSelected -> viewModelScope.launch {
                appPreferences.setPreferredBrowserPackage(event.packageName)
            }
            SettingsEvent.OnClearData -> viewModelScope.launch {
                topicRepository.deleteAll()
                forumRepository.resetBootstrap(15)
                forumRepository.resetBootstrap(16)
                _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_data_cleared)))
            }
            SettingsEvent.OnClearSyncLog -> viewModelScope.launch {
                syncLog.clear()
                _refreshTrigger.value++
                _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_sync_log_cleared)))
            }
            SettingsEvent.OnManageNotifications -> viewModelScope.launch {
                _effect.send(SettingsEffect.OpenNotificationSettings)
            }
            SettingsEvent.OnDebugClick -> viewModelScope.launch {
                _effect.send(SettingsEffect.NavigateToDebug)
            }
        }
    }
}

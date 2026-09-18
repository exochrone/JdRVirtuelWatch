package com.jdrvirtuel.watcher.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrvirtuel.watcher.BuildConfig
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.util.BrowserLauncher
import com.jdrvirtuel.watcher.core.util.DateFormatter
import com.jdrvirtuel.watcher.core.util.LogExporter
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import com.jdrvirtuel.watcher.domain.model.BackupData
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.NotificationType
import com.jdrvirtuel.watcher.domain.model.SyncSource
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import com.jdrvirtuel.watcher.domain.repository.BackupRepository
import com.jdrvirtuel.watcher.domain.repository.ForumRepository
import com.jdrvirtuel.watcher.domain.repository.TopicRepository
import com.jdrvirtuel.watcher.domain.usecase.ExportBackupUseCase
import com.jdrvirtuel.watcher.domain.usecase.ImportBackupUseCase
import com.jdrvirtuel.watcher.notification.StatusNotifier
import com.jdrvirtuel.watcher.notification.NotificationLog
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forumRepository: ForumRepository,
    private val topicRepository: TopicRepository,
    private val appPreferences: AppPreferences,
    private val backupRepository: BackupRepository,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val statusNotifier: StatusNotifier,
    private val syncLog: SyncLog,
    private val notificationLog: NotificationLog,
    private val logExporter: LogExporter
) : ViewModel() {

    private val browserLauncher = BrowserLauncher(context, appPreferences, viewModelScope)
    private val _refreshTrigger = MutableStateFlow(0)
    private var pendingBackupData: BackupData? = null

    private val commonInfoFlow = combine(
        forumRepository.observeForums(),
        appPreferences.consecutiveChallengeFailures,
        appPreferences.preferredBrowserPackage,
        appPreferences.isStatusNotificationEnabled
    ) { forums, failures, preferredBrowser, statusEnabled ->
        DataInfo(forums, failures, preferredBrowser, statusEnabled)
    }

    private data class DataInfo(
        val forums: List<Forum>,
        val failures: Int,
        val preferredBrowser: String?,
        val statusEnabled: Boolean
    )

    private val dataInfoFlow = combine(
        topicRepository.observeTotalCount(),
        appPreferences.syncLog,
        appPreferences.notificationLog,
        _refreshTrigger
    ) { totalTopics, _, _, _ ->
        totalTopics
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        commonInfoFlow,
        dataInfoFlow
    ) { common, totalTopics ->
        val forums = common.forums
        val failures = common.failures
        val preferredBrowser = common.preferredBrowser
        val statusEnabled = common.statusEnabled
        
        val browsers = browserLauncher.listCustomTabsBrowsers().map {
            BrowserPackageInfo(it.packageName, it.label)
        }
        
        val syncLogs = syncLog.getEntries()
        val notificationLogs = notificationLog.getEntries()

        SettingsUiState(
            isLoading = false,
            forums = forums,
            syncIntervalMinutes = if (failures >= 3) 60 else 15,
            preferredBrowserPackage = preferredBrowser,
            availableBrowsers = browsers,
            storedTopicsCount = totalTopics,
            statusNotificationEnabled = statusEnabled,
            syncLogs = syncLogs,
            notificationLogs = notificationLogs,
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
            SettingsEvent.OnExportSyncLog -> viewModelScope.launch {
                val logs = uiState.value.syncLogs
                if (logs.isEmpty()) return@launch
                val text = formatSyncLogs(logs)
                logExporter.export(text, context.getString(R.string.settings_section_sync_log))
            }
            SettingsEvent.OnClearNotificationLog -> viewModelScope.launch {
                notificationLog.clear()
                _refreshTrigger.value++
                _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_notification_log_cleared)))
            }
            SettingsEvent.OnExportNotificationLog -> viewModelScope.launch {
                val logs = uiState.value.notificationLogs
                if (logs.isEmpty()) return@launch
                val text = formatNotificationLogs(logs)
                logExporter.export(text, context.getString(R.string.settings_section_notification_log))
            }
            SettingsEvent.OnManageNotifications -> viewModelScope.launch {
                _effect.send(SettingsEffect.OpenNotificationSettings)
            }
            SettingsEvent.OnDebugClick -> viewModelScope.launch {
                _effect.send(SettingsEffect.NavigateToDebug)
            }
            SettingsEvent.OnDiagnosticClick -> viewModelScope.launch {
                appPreferences.setDiagnosticDismissed(false)
                _effect.send(SettingsEffect.NavigateToDiagnostic)
            }
            is SettingsEvent.OnStatusNotificationToggle -> viewModelScope.launch {
                appPreferences.setStatusNotificationEnabled(event.enabled)
                if (event.enabled) {
                    statusNotifier.update()
                } else {
                    statusNotifier.cancel()
                }
            }
            SettingsEvent.OnExportClick -> {
                val dateStr = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
                val fileName = "jdrvirtuelwatcher-$dateStr.json"
                viewModelScope.launch { _effect.send(SettingsEffect.LaunchExportPicker(fileName)) }
            }
            is SettingsEvent.OnFileToExportSelected -> viewModelScope.launch {
                try {
                    val backupData = exportBackupUseCase()
                    val json = backupRepository.serialize(backupData)
                    context.contentResolver.openOutputStream(event.uri)?.use { 
                        it.write(json.toByteArray())
                    }
                    _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_export_success, backupData.forums.sumOf { it.topics.size })))
                } catch (e: Exception) {
                    _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_export_error)))
                }
            }
            SettingsEvent.OnImportClick -> viewModelScope.launch {
                _effect.send(SettingsEffect.LaunchImportPicker)
            }
            is SettingsEvent.OnFileToImportSelected -> viewModelScope.launch {
                try {
                    val json = context.contentResolver.openInputStream(event.uri)?.use { 
                        it.bufferedReader().readText()
                    } ?: throw Exception("Empty file")
                    
                    val backupData = backupRepository.deserialize(json)
                    if (backupData == null) {
                        _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_import_error_invalid)))
                        return@launch
                    }
                    
                    if (backupData.formatVersion > 1) {
                        _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_import_error_version, backupData.formatVersion)))
                        return@launch
                    }
                    
                    pendingBackupData = backupData
                    val date = DateFormatter.formatRelative(backupData.exportedAt)
                    val totalTopics = backupData.forums.sumOf { it.topics.size }
                    _effect.send(SettingsEffect.ShowImportConfirmation(totalTopics, backupData.forums.size, date))
                } catch (e: Exception) {
                    _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_import_error_invalid)))
                }
            }
            SettingsEvent.OnConfirmImport -> viewModelScope.launch {
                val data = pendingBackupData ?: return@launch
                pendingBackupData = null
                try {
                    val result = importBackupUseCase(data)
                    _refreshTrigger.value++
                    _effect.send(SettingsEffect.ShowImportResult(
                        restored = result.restoredCount,
                        inserted = result.insertedCount,
                        ignored = result.ignoredCount,
                        intact = result.intactCount
                    ))
                } catch (e: Exception) {
                    _effect.send(SettingsEffect.ShowMessage(context.getString(R.string.settings_import_error_generic)))
                }
            }
        }
    }

    private fun formatSyncLogs(logs: List<com.jdrvirtuel.watcher.domain.model.SyncLogEntry>): String {
        return logs.joinToString("\n\n") { entry ->
            val timestamp = DateFormatter.formatLogDate(entry.timestampMs)
            val source = when (entry.source) {
                SyncSource.MANUAL -> context.getString(R.string.sync_source_manual)
                SyncSource.PERIODIC -> context.getString(R.string.sync_source_auto)
                SyncSource.TEST -> context.getString(R.string.sync_source_test)
            }
            val results = entry.forumResults.joinToString("\n") { res ->
                val status = when (res.status) {
                    SyncStatus.SUCCESS -> {
                        val parsed = context.resources.getQuantityString(R.plurals.sync_log_parsed, res.parsedCount, res.parsedCount)
                        val news = if (res.newTopicsCount > 0) context.resources.getQuantityString(R.plurals.sync_log_new, res.newTopicsCount, res.newTopicsCount) else null
                        val replies = if (res.newRepliesCount > 0) context.resources.getQuantityString(R.plurals.sync_log_replies, res.newRepliesCount, res.newRepliesCount) else null
                        
                        buildString {
                            append(context.getString(R.string.sync_status_success))
                            append(" · ")
                            append(parsed)
                            if (news != null) append(", $news")
                            if (replies != null) append(", $replies")
                        }
                    }
                    SyncStatus.CHALLENGE_REQUIRED -> context.getString(R.string.sync_status_blocked)
                    SyncStatus.ERROR -> "${context.getString(R.string.sync_status_error)}${if (res.errorMessage != null) " : ${res.errorMessage}" else ""}"
                }
                "   ${res.forumName} : $status"
            }
            "$timestamp · $source\n$results"
        }
    }

    private fun formatNotificationLogs(logs: List<com.jdrvirtuel.watcher.domain.model.NotificationLogEntry>): String {
        return logs.joinToString("\n\n") { entry ->
            val typeStr = when (entry.type) {
                NotificationType.NEW_TOPIC -> context.getString(R.string.notification_type_new_topic)
                NotificationType.NEW_REPLY -> context.getString(R.string.notification_type_new_reply)
                NotificationType.VERIFICATION -> context.getString(R.string.notification_type_verification)
            }
            val header = buildString {
                append(DateFormatter.formatLogDate(entry.timestampMs))
                append(" · ")
                append(typeStr)
                if (entry.forumName != null) append(" · ${entry.forumName}")
            }
            if (entry.topicTitle != null) {
                "$header\n${entry.topicTitle}"
            } else {
                header
            }
        }
    }
}

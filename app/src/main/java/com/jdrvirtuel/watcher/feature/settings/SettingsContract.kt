package com.jdrvirtuel.watcher.feature.settings

import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.NotificationLogEntry
import com.jdrvirtuel.watcher.domain.model.SyncLogEntry

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val forums: List<Forum> = emptyList(),
    val syncIntervalMinutes: Int = 15,
    val preferredBrowserPackage: String? = null,
    val availableBrowsers: List<BrowserPackageInfo> = emptyList(),
    val storedTopicsCount: Int = 0,
    val syncLogs: List<SyncLogEntry> = emptyList(),
    val notificationLogs: List<NotificationLogEntry> = emptyList(),
    val appVersion: String = ""
)

data class BrowserPackageInfo(
    val packageName: String,
    val label: String
)

sealed interface SettingsEvent {
    data object OnBack : SettingsEvent
    data class OnBrowserSelected(val packageName: String?) : SettingsEvent
    data object OnClearData : SettingsEvent
    data object OnClearSyncLog : SettingsEvent
    data object OnExportSyncLog : SettingsEvent
    data object OnClearNotificationLog : SettingsEvent
    data object OnExportNotificationLog : SettingsEvent
    data object OnManageNotifications : SettingsEvent
    data object OnDebugClick : SettingsEvent
    data object OnDiagnosticClick : SettingsEvent
    data object OnExportClick : SettingsEvent
    data class OnFileToExportSelected(val uri: android.net.Uri) : SettingsEvent
    data object OnImportClick : SettingsEvent
    data class OnFileToImportSelected(val uri: android.net.Uri) : SettingsEvent
    data object OnConfirmImport : SettingsEvent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateToDebug : SettingsEffect
    data object NavigateToDiagnostic : SettingsEffect
    data object OpenNotificationSettings : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
    data class LaunchExportPicker(val fileName: String) : SettingsEffect
    data object LaunchImportPicker : SettingsEffect
    data class ShowImportConfirmation(val topicCount: Int, val forumCount: Int, val date: String) : SettingsEffect
    data class ShowImportResult(val restored: Int, val inserted: Int, val ignored: Int, val intact: Int) : SettingsEffect
}

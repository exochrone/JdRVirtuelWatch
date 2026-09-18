package com.jdrvirtuel.watcher.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdrvirtuel.watcher.BuildConfig
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.component.LoadingState
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
import com.jdrvirtuel.watcher.core.ui.theme.LocalCustomColors
import com.jdrvirtuel.watcher.core.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToDiagnostic: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showClearLogDialog by remember { mutableStateOf(false) }
    var showClearNotificationLogDialog by remember { mutableStateOf(false) }
    var showImportConfirmationDialog by remember { mutableStateOf<SettingsEffect.ShowImportConfirmation?>(null) }
    var showImportResultDialog by remember { mutableStateOf<SettingsEffect.ShowImportResult?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.onEvent(SettingsEvent.OnFileToExportSelected(it)) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onEvent(SettingsEvent.OnFileToImportSelected(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> onBack()
                SettingsEffect.NavigateToDebug -> onNavigateToDebug()
                SettingsEffect.NavigateToDiagnostic -> onNavigateToDiagnostic()
                SettingsEffect.OpenNotificationSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
                is SettingsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SettingsEffect.LaunchExportPicker -> {
                    exportLauncher.launch(effect.fileName)
                }
                SettingsEffect.LaunchImportPicker -> {
                    importLauncher.launch(arrayOf("application/json"))
                }
                is SettingsEffect.ShowImportConfirmation -> {
                    showImportConfirmationDialog = effect
                }
                is SettingsEffect.ShowImportResult -> {
                    showImportResultDialog = effect
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(SettingsEvent.OnBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.md)
            ) {
                // 1. Section Données
                SettingsSectionTitle(stringResource(R.string.settings_section_data))
                Text(
                    text = stringResource(R.string.settings_stored_topics_count, uiState.storedTopicsCount),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Dimens.sm))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(SettingsEvent.OnExportClick) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.settings_data_export))
                    }
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    OutlinedButton(
                        onClick = { viewModel.onEvent(SettingsEvent.OnImportClick) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.settings_data_import))
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.sm))

                Button(
                    onClick = { showClearDataDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    Text(stringResource(R.string.settings_clear_data))
                }

                SyncLogSection(
                    logs = uiState.syncLogs,
                    onClearLog = { showClearLogDialog = true },
                    onExportLog = { viewModel.onEvent(SettingsEvent.OnExportSyncLog) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))

                // 2. Section Surveillance
                SettingsSectionTitle(stringResource(R.string.settings_section_watch))
                uiState.forums.forEach { forum ->
                    Column(modifier = Modifier.padding(vertical = Dimens.sm)) {
                        Text(
                            text = forum.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = forum.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.debug_last_sync,
                                DateFormatter.formatRelative(forum.lastSyncAt)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (uiState.syncIntervalMinutes == 60) stringResource(R.string.settings_sync_interval_60) else stringResource(R.string.settings_sync_interval_15),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.syncIntervalMinutes == 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))

                // 3. Section Notifications
                SettingsSectionTitle(stringResource(R.string.settings_section_notifications))
                Button(
                    onClick = { viewModel.onEvent(SettingsEvent.OnManageNotifications) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalCustomColors.current.notificationAction,
                        contentColor = LocalCustomColors.current.onNotificationAction
                    )
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimens.sm))
                    Text(stringResource(R.string.settings_manage_notifications))
                }

                NotificationLogSection(
                    logs = uiState.notificationLogs,
                    onClearLog = { showClearNotificationLogDialog = true },
                    onExportLog = { viewModel.onEvent(SettingsEvent.OnExportNotificationLog) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))

                // 4. Section Navigation
                SettingsSectionTitle(stringResource(R.string.settings_section_navigation))
                BrowserSelector(
                    availableBrowsers = uiState.availableBrowsers,
                    selectedPackage = uiState.preferredBrowserPackage,
                    onBrowserSelected = { viewModel.onEvent(SettingsEvent.OnBrowserSelected(it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))

                // 5. Section Diagnostic
                SettingsSectionTitle(stringResource(R.string.settings_section_diagnostic))
                OutlinedButton(
                    onClick = { viewModel.onEvent(SettingsEvent.OnDiagnosticClick) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_open_diagnostic))
                }
                Text(
                    text = stringResource(R.string.settings_diagnostic_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))

                // 6. Section À propos
                SettingsSectionTitle(stringResource(R.string.settings_section_about))
                Text(
                    text = stringResource(R.string.settings_about_app_name, uiState.appVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_about_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (BuildConfig.DEBUG) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))
                    SettingsSectionTitle(stringResource(R.string.settings_section_dev))
                    TextButton(onClick = { viewModel.onEvent(SettingsEvent.OnDebugClick) }) {
                        Text(stringResource(R.string.settings_dev_debug_screen))
                    }
                }
            }
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(stringResource(R.string.settings_clear_data_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_data_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(SettingsEvent.OnClearData)
                        showClearDataDialog = false
                    }
                ) {
                    Text(stringResource(R.string.debug_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.debug_cancel))
                }
            }
        )
    }

    if (showClearLogDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogDialog = false },
            title = { Text(stringResource(R.string.settings_clear_sync_log_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_sync_log_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(SettingsEvent.OnClearSyncLog)
                        showClearLogDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.debug_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogDialog = false }) {
                    Text(stringResource(R.string.debug_cancel))
                }
            }
        )
    }

    if (showClearNotificationLogDialog) {
        AlertDialog(
            onDismissRequest = { showClearNotificationLogDialog = false },
            title = { Text(stringResource(R.string.settings_clear_notification_log_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_notification_log_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(SettingsEvent.OnClearNotificationLog)
                        showClearNotificationLogDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.debug_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearNotificationLogDialog = false }) {
                    Text(stringResource(R.string.debug_cancel))
                }
            }
        )
    }

    showImportConfirmationDialog?.let { data ->
        AlertDialog(
            onDismissRequest = { showImportConfirmationDialog = null },
            title = { Text(stringResource(R.string.settings_import_confirm_title)) },
            text = { 
                Text(stringResource(
                    R.string.settings_import_confirm_message,
                    data.topicCount,
                    data.forumCount,
                    data.date
                )) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(SettingsEvent.OnConfirmImport)
                        showImportConfirmationDialog = null
                    }
                ) {
                    Text(stringResource(R.string.debug_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmationDialog = null }) {
                    Text(stringResource(R.string.debug_cancel))
                }
            }
        )
    }

    showImportResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { showImportResultDialog = null },
            title = { Text(stringResource(R.string.settings_import_result_title)) },
            text = {
                Text(stringResource(
                    R.string.settings_import_result_message,
                    result.restored,
                    result.inserted,
                    result.ignored,
                    result.intact
                ))
            },
            confirmButton = {
                TextButton(onClick = { showImportResultDialog = null }) {
                    Text(stringResource(R.string.debug_confirm))
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = Dimens.sm)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSelector(
    availableBrowsers: List<BrowserPackageInfo>,
    selectedPackage: String?,
    onBrowserSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedBrowser = availableBrowsers.find { it.packageName == selectedPackage }
    val displayText = selectedBrowser?.label ?: stringResource(R.string.settings_browser_system_default)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_browser_selector_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.xs))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_browser_system_default)) },
                    onClick = {
                        onBrowserSelected(null)
                        expanded = false
                    }
                )
                availableBrowsers.forEach { browser ->
                    DropdownMenuItem(
                        text = { Text(browser.label) },
                        onClick = {
                            onBrowserSelected(browser.packageName)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

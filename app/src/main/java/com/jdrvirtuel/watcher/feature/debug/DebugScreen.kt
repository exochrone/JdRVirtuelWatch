package com.jdrvirtuel.watcher.feature.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
import com.jdrvirtuel.watcher.core.util.BrowserLauncher
import com.jdrvirtuel.watcher.data.remote.WebViewConstants
import com.jdrvirtuel.watcher.domain.model.SyncSource
import com.jdrvirtuel.watcher.domain.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val browserLauncher = remember(viewModel.appPreferences) {
        BrowserLauncher(context, viewModel.appPreferences, scope)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                DebugEffect.NavigateBack -> onNavigateBack()
                is DebugEffect.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Debug Content", effect.text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copié dans le presse-papier", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DisposableEffect(uiState.isBenchRunning) {
        val window = (context as? Activity)?.window
        if (uiState.isBenchRunning) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(DebugEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.md)
        ) {
            item {
                DebugCollapsibleSection(
                    title = stringResource(R.string.debug_sync_section),
                    initialExpanded = true
                ) {
                    SyncSection(uiState, viewModel::onEvent)
                }
            }
            item {
                DebugCollapsibleSection(title = stringResource(R.string.debug_work_section), initialExpanded = true) {
                    WorkSection(uiState, viewModel::onEvent)
                }
            }
            item {
                DebugCollapsibleSection(title = stringResource(R.string.debug_database_section)) {
                    DatabaseSection(uiState, viewModel::onEvent)
                }
            }
            item {
                DebugCollapsibleSection(title = stringResource(R.string.debug_network_section)) {
                    NetworkSection(uiState, viewModel::onEvent)
                }
            }
            item {
                DebugCollapsibleSection(title = stringResource(R.string.debug_parser_section)) {
                    ParserSection(uiState, viewModel::onEvent)
                }
            }
            item {
                DebugCollapsibleSection(title = stringResource(R.string.debug_cloudflare_section)) {
                    CloudflareSection(uiState, viewModel::onEvent)
                }
            }
            item {
                DebugCollapsibleSection(title = stringResource(R.string.debug_bench_section)) {
                    BenchSection(uiState, viewModel::onEvent)
                }
            }
            item { Spacer(modifier = Modifier.height(Dimens.lg)) }
        }
    }
}

@Composable
fun DebugCollapsibleSection(
    title: String,
    initialExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = Dimens.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(bottom = Dimens.md)) {
                content()
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun SyncSection(uiState: DebugUiState, onEvent: (DebugEvent) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { onEvent(DebugEvent.SyncForum(15)) }, enabled = !uiState.isSyncing, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.debug_sync_f15))
            }
            Button(onClick = { onEvent(DebugEvent.SyncForum(16)) }, enabled = !uiState.isSyncing, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.debug_sync_f16))
            }
            Button(onClick = { onEvent(DebugEvent.SyncAll) }, enabled = !uiState.isSyncing, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.debug_sync_all))
            }
        }

        if (uiState.isSyncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.sm))
        }

        uiState.lastSyncOutcome?.let { SyncOutcomeCard(it) }

        Spacer(modifier = Modifier.height(Dimens.sm))
        Button(onClick = { onEvent(DebugEvent.DeleteRandomTopic) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debug_delete_random))
        }
        uiState.lastDeletedTopicInfo?.let {
            Text(text = stringResource(R.string.debug_last_deleted, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(Dimens.md))
        TopicSelector(uiState, onEvent)
    }
}

@Composable
fun TopicSelector(uiState: DebugUiState, onEvent: (DebugEvent) -> Unit) {
    val allTopics = uiState.topicsByForum.values.flatten()
    val selectedTopic = allTopics.find { it.id == uiState.selectedTopicId }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Outils sur sujet cible :", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Box {
            OutlinedTextField(
                value = selectedTopic?.let { "${it.id} - ${it.title.take(20)}..." } ?: "Aucun sujet sélectionné",
                onValueChange = { },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allTopics.forEach { topic ->
                    DropdownMenuItem(
                        text = { Text("${topic.id} - ${topic.title.take(30)}...") },
                        onClick = {
                            onEvent(DebugEvent.SelectTopic(topic.id))
                            expanded = false
                        }
                    )
                }
            }
        }

        if (selectedTopic != null) {
            Column(modifier = Modifier.padding(top = Dimens.sm)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                    FlagText("Surveillé", selectedTopic.isWatched)
                    FlagText("Masqué", selectedTopic.isHidden)
                    FlagText("Lu", selectedTopic.isRead)
                    FlagText("Complet", selectedTopic.isFull)
                }
                Text("Réponses : ${selectedTopic.replyCount}", style = MaterialTheme.typography.bodySmall)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                ) {
                    Button(onClick = { onEvent(DebugEvent.ToggleWatched) }, modifier = Modifier.weight(1f)) { Text("Basc. surveillé") }
                    Button(onClick = { onEvent(DebugEvent.ToggleHidden) }, modifier = Modifier.weight(1f)) { Text("Basc. masqué") }
                    Button(onClick = { onEvent(DebugEvent.ToggleRead) }, modifier = Modifier.weight(1f)) { Text("Basc. lu") }
                    Button(onClick = { onEvent(DebugEvent.DecrementReplyCount) }, modifier = Modifier.weight(1f)) { Text("Déc. réponses") }
                }
            }
        }
    }
}

@Composable
fun DatabaseSection(uiState: DebugUiState, onEvent: (DebugEvent) -> Unit) {
    var showClearConfirm by remember { mutableStateOf(false) }
    Column {
        Button(onClick = { onEvent(DebugEvent.InsertTestTopic) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.debug_insert_test_topic)) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { showClearConfirm = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.debug_clear_topics)) }
            Button(onClick = { onEvent(DebugEvent.ResetBootstrap) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.debug_reset_bootstrap)) }
        }
        Text(text = stringResource(R.string.debug_total_topics, uiState.totalTopics), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = Dimens.sm))
        
        uiState.forums.forEach { forum ->
            ForumDebugItem(forum)
            val topics = uiState.topicsByForum[forum.id] ?: emptyList()
            topics.forEach { TopicDebugItem(it) }
        }
    }
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.debug_clear_topics_confirm_title)) },
            confirmButton = { TextButton(onClick = { onEvent(DebugEvent.ClearTopics); showClearConfirm = false }) { Text(stringResource(R.string.debug_confirm)) } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.debug_cancel)) } }
        )
    }
}

@Composable
fun NetworkSection(uiState: DebugUiState, onEvent: (DebugEvent) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { onEvent(DebugEvent.FetchForumHtml(15)) }, enabled = !uiState.isNetworkLoading, modifier = Modifier.weight(1f)) { Text("Load F15") }
            Button(onClick = { onEvent(DebugEvent.FetchForumHtml(16)) }, enabled = !uiState.isNetworkLoading, modifier = Modifier.weight(1f)) { Text("Load F16") }
        }
        if (uiState.isNetworkLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(Dimens.sm))
        uiState.fetchResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
        uiState.htmlContent?.let { html ->
            Text(stringResource(R.string.debug_html_size, uiState.htmlSize), style = MaterialTheme.typography.bodySmall)
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(vertical = Dimens.xs)) {
                SelectionContainer {
                    val scroll = rememberScrollState()
                    Text(html.take(5000), modifier = Modifier.padding(Dimens.xs).verticalScroll(scroll), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
            }
            Button(onClick = { onEvent(DebugEvent.CopyToClipboard(html)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.debug_copy_html)) }
        }
    }
}

@Composable
fun ParserSection(uiState: DebugUiState, onEvent: (DebugEvent) -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { onEvent(DebugEvent.ParseTestFile) }, modifier = Modifier.weight(1f)) { Text("Parse Test File") }
            Button(onClick = { onEvent(DebugEvent.ParseLastLoadedHtml) }, enabled = uiState.htmlContent != null, modifier = Modifier.weight(1f)) { Text("Parse Last HTML") }
        }
        uiState.parseResult?.let { res ->
            Text(stringResource(R.string.debug_parse_result, res.topics.size, res.skippedSticky, res.skippedInvalid), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(vertical = Dimens.xs)) {
                LazyColumn(modifier = Modifier.padding(Dimens.xs).heightIn(max = 600.dp)) {
                    items(res.topics) { ParsedTopicDebugItem(it, dateFormat) }
                }
            }
        }
    }
}

@Composable
fun CloudflareSection(uiState: DebugUiState, onEvent: (DebugEvent) -> Unit) {
    var showWebView by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }

    Column {
        Text("Échecs consécutifs : ${uiState.consecutiveFailures}", style = MaterialTheme.typography.bodyMedium)
        val lastPrompt = if (uiState.lastPromptAt > 0) dateFormat.format(Date(uiState.lastPromptAt)) else "Jamais"
        Text("Dernière notification : $lastPrompt", style = MaterialTheme.typography.bodySmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Simuler un blocage Cloudflare", modifier = Modifier.weight(1f))
            Switch(
                checked = uiState.simulateChallenge,
                onCheckedChange = { onEvent(DebugEvent.ToggleSimulateChallenge(it)) }
            )
        }
        if (uiState.simulateChallenge) {
            Text(
                "BLOCAGE SIMULÉ ACTIF",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.sm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
        ) {
            Button(onClick = { onEvent(DebugEvent.ClearCookies) }, modifier = Modifier.weight(1f)) { Text("Effacer Cookies") }
            Button(onClick = { onEvent(DebugEvent.SimulateThreeFailures) }, modifier = Modifier.weight(1f)) { Text("Simuler 3 échecs") }
            Button(onClick = { onEvent(DebugEvent.ResetFailures) }, modifier = Modifier.weight(1f)) { Text("Reset Compteur") }
        }

        Button(onClick = { showWebView = !showWebView }, modifier = Modifier.fillMaxWidth()) { Text(if (showWebView) "Masquer WebView" else "Valider Challenge Cloudflare") }
        if (showWebView) {
            Card(modifier = Modifier.fillMaxWidth().height(600.dp).padding(vertical = Dimens.sm)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    view?.evaluateJavascript("(function(){return document.documentElement.outerHTML;})();") { valStr ->
                                        if (valStr != null && valStr.contains(WebViewConstants.SUCCESS_MARKER)) CookieManager.getInstance().flush()
                                    }
                                }
                            }
                            loadUrl("https://www.jdrvirtuel.com/viewforum.php?f=15")
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { webView ->
                        webView.stopLoading()
                        webView.destroy()
                    }
                )
            }
        }
    }
}

@Composable
fun BenchSection(uiState: DebugUiState, onEvent: (DebugEvent) -> Unit) {
    Column {
        OutlinedTextField(
            value = uiState.benchIntervalMinutes.toString(),
            onValueChange = { onEvent(DebugEvent.UpdateBenchInterval(it.toIntOrNull() ?: 5)) },
            label = { Text(stringResource(R.string.debug_bench_interval)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBenchRunning
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { onEvent(DebugEvent.StartBench) }, enabled = !uiState.isBenchRunning, modifier = Modifier.weight(1f)) { Text("Démarrer") }
            Button(onClick = { onEvent(DebugEvent.StopBench) }, enabled = uiState.isBenchRunning, modifier = Modifier.weight(1f)) { Text("Arrêter") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { onEvent(DebugEvent.ClearBenchLogs) }, modifier = Modifier.weight(1f)) { Text("Reset Log") }
            Button(onClick = { onEvent(DebugEvent.ExportBenchLogs) }, enabled = uiState.benchLogs.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Exporter") }
            Button(onClick = { onEvent(DebugEvent.CopyBenchLogs) }, enabled = uiState.benchLogs.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Copier") }
        }
        if (uiState.benchLogs.isNotEmpty()) {
            val df = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(vertical = Dimens.xs)) {
                LazyColumn(modifier = Modifier.padding(Dimens.xs).heightIn(max = 600.dp)) {
                    items(uiState.benchLogs) { e ->
                        val el = e.timeSinceStartMs / 1000
                        Text("${df.format(Date(e.timestamp))} | +${el/60}m${el%60}s | ${e.result}${e.htmlSize?.let { " | $it oct." } ?: ""}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    }
                }
            }
        }
    }
}

@Composable
fun SyncOutcomeCard(outcome: SyncOutcome) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.xs)) {
        Column(modifier = Modifier.padding(Dimens.sm)) {
            Text(stringResource(R.string.debug_sync_outcome_title, outcome.forumId), style = MaterialTheme.typography.titleSmall)
            Text("Statut : ${outcome.status}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text("Parsed: ${outcome.parsedCount} | Ins: ${outcome.insertedCount} | Upd: ${outcome.updatedCount} | Purg: ${outcome.purgedCount}", style = MaterialTheme.typography.bodySmall)
            if (outcome.newTopics.isNotEmpty()) Text("Nouveaux : ${outcome.newTopics.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            if (outcome.newReplies.isNotEmpty()) Text("Réponses : ${outcome.newReplies.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ForumDebugItem(forum: Forum) {
    Text(text = "${forum.id} - ${forum.name} (${if(forum.isBootstrapped) "Amorcé" else "Non amorcé"})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
}

@Composable
fun TopicDebugItem(topic: Topic) {
    Column(modifier = Modifier.padding(vertical = Dimens.xs)) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val flags = mutableListOf<String>()
        if (topic.isFull) flags.add("Complet")
        if (topic.isWatched) flags.add("Surveillé")
        if (topic.isHidden) flags.add("Masqué")
        if (!topic.isRead) flags.add("Non lu")

        val metadata = buildString {
            append("${topic.replyCount} réponses")
            flags.forEach { append(" · $it") }
            append(" · #${topic.id}")
        }
        Text(
            text = metadata,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ParsedTopicDebugItem(topic: ParsedTopic, df: SimpleDateFormat) {
    Column(modifier = Modifier.padding(vertical = Dimens.xs)) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val flags = mutableListOf<String>()
        if (topic.isFull) flags.add("Complet")

        val metadata = buildString {
            append("${topic.replyCount} réponses")
            flags.forEach { append(" · $it") }
            append(" · #${topic.id}")
        }
        Text(
            text = metadata,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FlagText(label: String, active: Boolean) {
    Text(text = "$label: ${if (active) "Y" else "N"} ", style = MaterialTheme.typography.bodySmall, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun WorkSection(uiState: DebugUiState, onEvent: (DebugEvent) -> Unit) {
    Column {
        Text("Tâche périodique (15 min) :", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("État : ${uiState.workInfoState ?: "Inconnu"}", style = MaterialTheme.typography.bodySmall)
        uiState.workPeriodMinutes?.let {
            Text(stringResource(R.string.debug_work_period, it), style = MaterialTheme.typography.bodySmall)
        }
        
        val lastSync = uiState.forums.maxOfOrNull { it.lastSyncAt ?: 0L } ?: 0L
        val lastSyncStr = if (lastSync > 0) {
            SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(lastSync))
        } else "Jamais"
        Text("Dernière exécution : $lastSyncStr", style = MaterialTheme.typography.bodySmall)

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.xs), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { onEvent(DebugEvent.TriggerImmediateSync) }, modifier = Modifier.weight(1f)) {
                Text("Déclencher")
            }
            Button(onClick = { onEvent(DebugEvent.ReschedulePeriodicSync) }, modifier = Modifier.weight(1f)) {
                Text("Reprogrammer")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.sm))
        
        Text("Mode test accéléré :", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text("État : ${if (uiState.testModeEnabled) "ACTIF" else "Inactif"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (uiState.testModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)

        OutlinedTextField(
            value = uiState.testModeIntervalMinutes.toString(),
            onValueChange = { onEvent(DebugEvent.UpdateTestModeInterval(it.toIntOrNull() ?: 2)) },
            label = { Text("Intervalle (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.testModeEnabled
        )

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.xs), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { onEvent(DebugEvent.StartTestMode) }, enabled = !uiState.testModeEnabled, modifier = Modifier.weight(1f)) {
                Text("Démarrer")
            }
            Button(onClick = { onEvent(DebugEvent.StopTestMode) }, enabled = uiState.testModeEnabled, modifier = Modifier.weight(1f)) {
                Text("Arrêter")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Button(onClick = { onEvent(DebugEvent.ClearTestModeLog) }, modifier = Modifier.weight(1f)) {
                Text("Vider Log")
            }
            Button(onClick = { onEvent(DebugEvent.ExportTestModeLog) }, enabled = uiState.testModeLog.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text("Exporter")
            }
        }

        if (uiState.testModeLog.isNotEmpty()) {
            Text("Journal (récent en haut) :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = Dimens.sm))
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(vertical = Dimens.xs)) {
                LazyColumn(modifier = Modifier.padding(Dimens.xs).heightIn(max = 600.dp)) {
                    items(uiState.testModeLog) { entry ->
                        Text(entry.format(), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Journal permanent (50 max) :", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (uiState.syncLog.isNotEmpty()) {
                TextButton(onClick = { onEvent(DebugEvent.ExportSyncLog) }) {
                    Text("Exporter")
                }
            }
        }

        if (uiState.syncLog.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(vertical = Dimens.xs)) {
                LazyColumn(modifier = Modifier.padding(Dimens.xs).heightIn(max = 600.dp)) {
                    items(uiState.syncLog) { entry ->
                        val sourceStr = when(entry.source) {
                            SyncSource.MANUAL -> "Manuelle"
                            SyncSource.PERIODIC -> "Automatique"
                            SyncSource.TEST -> "Test"
                        }
                        Text("${com.jdrvirtuel.watcher.core.util.DateFormatter.formatLogDate(entry.timestampMs)} · $sourceStr", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        entry.forumResults.forEach { res ->
                            val statusStr = when (res.status) {
                                SyncStatus.SUCCESS -> {
                                    val parsed = pluralStringResource(R.plurals.sync_log_parsed, res.parsedCount, res.parsedCount)
                                    val news = if (res.newTopicsCount > 0) pluralStringResource(R.plurals.sync_log_new, res.newTopicsCount, res.newTopicsCount) else null
                                    val replies = if (res.newRepliesCount > 0) pluralStringResource(R.plurals.sync_log_replies, res.newRepliesCount, res.newRepliesCount) else null
                                    
                                    buildString {
                                        append("Succès · ")
                                        append(parsed)
                                        if (news != null) {
                                            append(", ")
                                            append(news)
                                        }
                                        if (replies != null) {
                                            append(", ")
                                            append(replies)
                                        }
                                    }
                                }
                                SyncStatus.CHALLENGE_REQUIRED -> "Bloqué"
                                SyncStatus.ERROR -> "Erreur${if (res.errorMessage != null) " : ${res.errorMessage}" else ""}"
                            }
                            Text("   ${res.forumName} : $statusStr", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(Dimens.xs))
                    }
                }
            }
        } else {
            Text("Aucune entrée", style = MaterialTheme.typography.bodySmall)
        }
    }
}

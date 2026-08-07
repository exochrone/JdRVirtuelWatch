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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
import com.jdrvirtuel.watcher.data.remote.WebViewConstants
import com.jdrvirtuel.watcher.domain.model.Forum
import com.jdrvirtuel.watcher.domain.model.ParsedTopic
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.SyncStatus
import com.jdrvirtuel.watcher.domain.model.Topic
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.md)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.onEvent(DebugEvent.InsertTestTopic) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.debug_insert_test_topic))
                }
            }
            Spacer(modifier = Modifier.height(Dimens.sm))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.debug_clear_topics))
                }
            }
            Spacer(modifier = Modifier.height(Dimens.md))
            Text(
                text = stringResource(R.string.debug_total_topics, uiState.totalTopics),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.sm))

            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    BenchDebugSection(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }

                item {
                    SyncDebugSection(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }

                item {
                    NetworkDebugSection(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }

                item {
                    ParserDebugSection(
                        uiState = uiState,
                        onEvent = viewModel::onEvent
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.debug_forums_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = Dimens.sm)
                    )
                }
                items(uiState.forums, key = { it.id }) { forum ->
                    ForumDebugItem(forum)
                }

                item {
                    Text(
                        text = stringResource(R.string.debug_topics_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = Dimens.sm)
                    )
                }

                if (uiState.totalTopics == 0) {
                    item {
                        Text(
                            text = stringResource(R.string.debug_no_topics),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(Dimens.md)
                        )
                    }
                } else {
                    uiState.forums.forEach { forum ->
                        val topics = uiState.topicsByForum[forum.id] ?: emptyList()
                        if (topics.isNotEmpty()) {
                            item {
                                Text(
                                    text = forum.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = Dimens.xs)
                                )
                            }
                            items(topics, key = { it.id }) { topic ->
                                TopicDebugItem(topic)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.debug_clear_topics_confirm_title)) },
            text = { Text(stringResource(R.string.debug_clear_topics_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(DebugEvent.ClearTopics)
                    showDeleteConfirmation = false
                }) {
                    Text(stringResource(R.string.debug_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.debug_cancel))
                }
            }
        )
    }
}

@Composable
fun BenchDebugSection(
    uiState: DebugUiState,
    onEvent: (DebugEvent) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.debug_bench_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = Dimens.sm)
        )

        OutlinedTextField(
            value = uiState.benchIntervalMinutes.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onEvent(DebugEvent.UpdateBenchInterval(it)) }
            },
            label = { Text(stringResource(R.string.debug_bench_interval)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBenchRunning
        )

        Spacer(modifier = Modifier.height(Dimens.sm))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onEvent(DebugEvent.StartBench) },
                enabled = !uiState.isBenchRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_bench_start))
            }
            Spacer(modifier = Modifier.padding(Dimens.xs))
            Button(
                onClick = { onEvent(DebugEvent.StopBench) },
                enabled = uiState.isBenchRunning,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_bench_stop))
            }
        }

        Spacer(modifier = Modifier.height(Dimens.sm))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onEvent(DebugEvent.ClearBenchLogs) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_bench_reset))
            }
            Spacer(modifier = Modifier.padding(Dimens.xs))
            Button(
                onClick = { onEvent(DebugEvent.CopyBenchLogs) },
                enabled = uiState.benchLogs.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_bench_copy))
            }
        }

        if (uiState.benchLogs.isEmpty()) {
            Text(
                text = stringResource(R.string.debug_bench_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = Dimens.md)
            )
        } else {
            val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = Dimens.sm)
            ) {
                LazyColumn(modifier = Modifier.padding(Dimens.sm)) {
                    items(uiState.benchLogs) { entry ->
                        val elapsed = entry.timeSinceStartMs / 1000
                        val min = elapsed / 60
                        val sec = elapsed % 60
                        val logText = "${dateFormat.format(Date(entry.timestamp))} | +${min}m${sec}s | ${entry.result}" +
                                (entry.htmlSize?.let { " | $it octets" } ?: "")
                        
                        Text(
                            text = logText,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = when {
                                entry.result == "Succès" -> MaterialTheme.colorScheme.primary
                                entry.result == "Vérification requise" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))
    }
}

@Composable
fun SyncDebugSection(
    uiState: DebugUiState,
    onEvent: (DebugEvent) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.debug_sync_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = Dimens.sm)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onEvent(DebugEvent.SyncForum(15)) },
                enabled = !uiState.isSyncing,
                modifier = Modifier.weight(1f)
            ) {
                Text("Sync F15")
            }
            Spacer(modifier = Modifier.padding(Dimens.xs))
            Button(
                onClick = { onEvent(DebugEvent.SyncForum(16)) },
                enabled = !uiState.isSyncing,
                modifier = Modifier.weight(1f)
            ) {
                Text("Sync F16")
            }
            Spacer(modifier = Modifier.padding(Dimens.xs))
            Button(
                onClick = { onEvent(DebugEvent.SyncAll) },
                enabled = !uiState.isSyncing,
                modifier = Modifier.weight(1f)
            ) {
                Text("Tout Sync")
            }
        }

        if (uiState.isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.md),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.lastSyncOutcome?.let { outcome ->
            SyncOutcomeCard(outcome)
        }

        Spacer(modifier = Modifier.height(Dimens.sm))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onEvent(DebugEvent.DeleteRandomTopic) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_delete_random))
            }
            Spacer(modifier = Modifier.padding(Dimens.xs))
            Button(
                onClick = { onEvent(DebugEvent.DecrementReplyCount) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_decrement_replies))
            }
        }

        Button(
            onClick = { onEvent(DebugEvent.ResetBootstrap) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.debug_reset_bootstrap))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))
    }
}

@Composable
fun SyncOutcomeCard(outcome: SyncOutcome) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.sm)
    ) {
        Column(modifier = Modifier.padding(Dimens.sm)) {
            Text(
                text = stringResource(R.string.debug_sync_outcome_title, outcome.forumId),
                style = MaterialTheme.typography.titleSmall
            )
            val statusRes = when (outcome.status) {
                SyncStatus.SUCCESS -> R.string.debug_sync_success
                SyncStatus.CHALLENGE_REQUIRED -> R.string.debug_sync_challenge
                SyncStatus.ERROR -> R.string.debug_sync_error
            }
            Text(
                text = stringResource(R.string.debug_sync_status, stringResource(statusRes)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = when (outcome.status) {
                    SyncStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
            )
            if (outcome.errorMessage != null) {
                Text(
                    text = outcome.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = stringResource(
                    R.string.debug_sync_counts,
                    outcome.parsedCount,
                    outcome.insertedCount,
                    outcome.updatedCount,
                    outcome.purgedCount
                ),
                style = MaterialTheme.typography.bodySmall
            )

            if (outcome.newTopics.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.debug_sync_new_topics, outcome.newTopics.size),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Dimens.xs)
                )
                outcome.newTopics.forEach { topic ->
                    Text(text = "• ${topic.title}", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (outcome.newReplies.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.debug_sync_new_replies, outcome.newReplies.size),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Dimens.xs)
                )
                outcome.newReplies.forEach { topic ->
                    Text(text = "• ${topic.title}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ParserDebugSection(
    uiState: DebugUiState,
    onEvent: (DebugEvent) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column {
        Text(
            text = stringResource(R.string.debug_parser_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = Dimens.sm)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onEvent(DebugEvent.ParseTestFile) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_parse_test_file))
            }
            Spacer(modifier = Modifier.padding(Dimens.xs))
            Button(
                onClick = { onEvent(DebugEvent.ParseLastLoadedHtml) },
                enabled = uiState.htmlContent != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_parse_last_html))
            }
        }

        uiState.parseResult?.let { result ->
            Text(
                text = stringResource(
                    R.string.debug_parse_result,
                    result.topics.size,
                    result.skippedSticky,
                    result.skippedInvalid
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = Dimens.sm)
            )

            result.topics.forEach { parsedTopic ->
                ParsedTopicDebugItem(parsedTopic, dateFormat)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))
    }
}

@Composable
fun ParsedTopicDebugItem(topic: ParsedTopic, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.xs)
    ) {
        Column(modifier = Modifier.padding(Dimens.sm)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ID: ${topic.id} | Réponses: ${topic.replyCount}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.debug_topic_author,
                    topic.author,
                    dateFormat.format(Date(topic.createdAt))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.debug_topic_last_post,
                    topic.lastPostAuthor,
                    dateFormat.format(Date(topic.lastPostAt))
                ),
                style = MaterialTheme.typography.bodySmall
            )
            if (topic.isFull) {
                androidx.compose.material3.AssistChip(
                    onClick = { },
                    label = { Text(stringResource(R.string.debug_topic_full)) },
                    modifier = Modifier.padding(top = Dimens.xs)
                )
            }
        }
    }
}

@Composable
fun NetworkDebugSection(
    uiState: DebugUiState,
    onEvent: (DebugEvent) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.debug_network_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = Dimens.sm)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onEvent(DebugEvent.FetchForumHtml(15)) },
                enabled = !uiState.isNetworkLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_load_forum_15))
            }
            Spacer(modifier = Modifier.padding(Dimens.xs))
            Button(
                onClick = { onEvent(DebugEvent.FetchForumHtml(16)) },
                enabled = !uiState.isNetworkLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.debug_load_forum_16))
            }
        }

        if (uiState.isNetworkLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.md),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.fetchResult?.let { result ->
            Text(
                text = result,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    result == "Succès" -> MaterialTheme.colorScheme.primary
                    result == "Vérification requise" -> MaterialTheme.colorScheme.error
                    result.startsWith("Erreur") -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(vertical = Dimens.sm)
            )
        }

        uiState.htmlContent?.let { html ->
            Text(
                text = stringResource(R.string.debug_html_size, uiState.htmlSize),
                style = MaterialTheme.typography.bodySmall
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = Dimens.sm)
            ) {
                SelectionContainer {
                    val scrollState = rememberScrollState()
                    Text(
                        text = html.take(2000),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimens.sm)
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
            Button(
                onClick = { onEvent(DebugEvent.CopyToClipboard(html)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debug_copy_html))
            }
        }

        Spacer(modifier = Modifier.height(Dimens.md))
        
        var showVisibleWebView by remember { mutableStateOf(false) }
        
        Text(
            text = "En cas de blocage persistant, ce bouton permet de renouveler manuellement le cookie d'accès en cochant la case de vérification Cloudflare.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = Dimens.xs)
        )
        Button(
            onClick = { showVisibleWebView = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Valider le challenge Cloudflare")
        }

        if (showVisibleWebView) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(700.dp)
                    .padding(vertical = Dimens.sm)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    view?.evaluateJavascript("(function(){return document.documentElement.outerHTML;})();") { value ->
                                        if (value != null && value.contains(WebViewConstants.SUCCESS_MARKER)) {
                                            CookieManager.getInstance().flush()
                                        }
                                    }
                                }
                            }
                            loadUrl("https://www.jdrvirtuel.com/viewforum.php?f=15")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.md))
    }
}

@Composable
fun ForumDebugItem(forum: Forum) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.xs)
    ) {
        Column(modifier = Modifier.padding(Dimens.sm)) {
            Text(text = "${forum.id} - ${forum.name}", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (forum.isBootstrapped) stringResource(R.string.debug_forum_bootstrapped)
                else stringResource(R.string.debug_forum_not_bootstrapped),
                style = MaterialTheme.typography.bodySmall
            )
            val syncText = forum.lastSyncAt?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.debug_never_synced)
            Text(
                text = stringResource(R.string.debug_last_sync, syncText),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TopicDebugItem(topic: Topic) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.xs)
    ) {
        Column(modifier = Modifier.padding(Dimens.sm)) {
            Text(text = topic.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.debug_topic_id, topic.id), style = MaterialTheme.typography.bodySmall)
            Text(text = stringResource(R.string.debug_topic_replies, topic.replyCount), style = MaterialTheme.typography.bodySmall)
            Row {
                FlagText("Full", topic.isFull)
                FlagText("Hidden", topic.isHidden)
                FlagText("Watched", topic.isWatched)
                FlagText("Read", topic.isRead)
            }
        }
    }
}

@Composable
fun FlagText(label: String, active: Boolean) {
    Text(
        text = "$label: ${if (active) "Y" else "N"} ",
        style = MaterialTheme.typography.bodySmall,
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

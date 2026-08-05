package com.jdrvirtuel.watcher.feature.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
import com.jdrvirtuel.watcher.data.remote.WebViewConstants
import com.jdrvirtuel.watcher.domain.model.Forum
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
                    val clip = ClipData.newPlainText("HTML Content", effect.text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "HTML copié", Toast.LENGTH_SHORT).show()
                }
            }
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
                    NetworkDebugSection(
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
                            settings.userAgentString = WebViewConstants.USER_AGENT
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

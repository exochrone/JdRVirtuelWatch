package com.jdrvirtuel.watcher.feature.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
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

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                DebugEffect.NavigateBack -> onNavigateBack()
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

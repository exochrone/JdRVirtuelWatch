package com.jdrvirtuel.watcher.feature.forumdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
import com.jdrvirtuel.watcher.core.util.BrowserLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumDetailScreen(
    viewModel: ForumDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val browserLauncher = remember(viewModel.appPreferences) { 
        BrowserLauncher(context, viewModel.appPreferences, scope) 
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ForumDetailEffect.OpenUrl -> {
                    browserLauncher.openUrl(effect.url) { success ->
                        if (!success) {
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.forum_detail_no_browser))
                            }
                        }
                    }
                }
                is ForumDetailEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is ForumDetailEffect.ShowUndoHide -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.forum_detail_topic_masked),
                        actionLabel = context.getString(R.string.forum_detail_undo),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(ForumDetailEvent.OnUndoHide(effect.topicId, effect.wasWatched))
                    }
                }
                ForumDetailEffect.NavigateBack -> {
                    onBack()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.forumName)
                        if (!uiState.isLoading && uiState.totalTopicCount > 0) {
                            val summary = when {
                                uiState.hiddenTopicCount == 0 -> pluralStringResource(
                                    R.plurals.home_topics_count,
                                    uiState.totalTopicCount,
                                    uiState.totalTopicCount
                                )
                                !uiState.showHidden -> stringResource(
                                    R.string.forum_detail_summary_displayed_on_total,
                                    uiState.displayedTopicCount,
                                    uiState.totalTopicCount
                                )
                                else -> stringResource(
                                    R.string.forum_detail_summary_total_with_hidden,
                                    uiState.totalTopicCount,
                                    uiState.hiddenTopicCount
                                )
                            }
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(ForumDetailEvent.OnBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ForumDetailEvent.OnToggleShowHidden) }) {
                        Icon(
                            imageVector = if (uiState.showHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (uiState.showHidden) {
                                stringResource(R.string.forum_detail_hide_hidden)
                            } else {
                                stringResource(R.string.forum_detail_show_hidden)
                            }
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(ForumDetailEvent.OnRefresh) }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.home_refresh)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = { viewModel.onEvent(ForumDetailEvent.OnRefresh) },
            modifier = Modifier.padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null && uiState.topics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(Dimens.md)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.md),
                    verticalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    items(uiState.topics, key = { it.id }) { topic ->
                        TopicCard(
                            topic = topic,
                            onTopicClick = { viewModel.onEvent(ForumDetailEvent.OnTopicClick(it)) },
                            onToggleHidden = { viewModel.onEvent(ForumDetailEvent.OnToggleHidden(it)) },
                            onToggleWatched = { viewModel.onEvent(ForumDetailEvent.OnToggleWatched(it)) }
                        )
                    }
                }
            }
        }
    }
}

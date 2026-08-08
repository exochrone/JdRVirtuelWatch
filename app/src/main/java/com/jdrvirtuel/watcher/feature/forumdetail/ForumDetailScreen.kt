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
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.component.EmptyState
import com.jdrvirtuel.watcher.core.ui.component.ErrorState
import com.jdrvirtuel.watcher.core.ui.component.LoadingState
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
    var showHideFullConfirm by remember { mutableStateOf(false) }

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
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is ForumDetailEffect.ShowUndoHide -> {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.forum_detail_topic_masked),
                            actionLabel = context.getString(R.string.forum_detail_undo),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onEvent(ForumDetailEvent.OnUndoHide(effect.topicId, effect.wasWatched))
                        }
                    }
                }
                is ForumDetailEffect.ShowUndoHideAllFull -> {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val count = effect.topicIds.size
                        val result = snackbarHostState.showSnackbar(
                            message = context.resources.getQuantityString(
                                R.plurals.forum_detail_hide_full_success,
                                count,
                                count
                            ),
                            actionLabel = context.getString(R.string.forum_detail_undo),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onEvent(ForumDetailEvent.OnUndoHideAllFull(effect.topicIds))
                        }
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
                            imageVector = if (uiState.showHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (uiState.showHidden) {
                                stringResource(R.string.forum_detail_hide_hidden)
                            } else {
                                stringResource(R.string.forum_detail_show_hidden)
                            }
                        )
                    }
                    
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "Plus d'options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.forum_detail_menu_refresh)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(ForumDetailEvent.OnRefresh)
                                },
                                leadingIcon = { Icon(Icons.Outlined.Refresh, null) }
                            )
                            val hideFullCount = uiState.fullTopicsToHide.size
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.forum_detail_menu_hide_full)) },
                                onClick = {
                                    showMenu = false
                                    if (hideFullCount > 0) {
                                        showHideFullConfirm = true
                                    } else {
                                        viewModel.onEvent(ForumDetailEvent.OnHideAllFull)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Outlined.PlaylistRemove, null) },
                                enabled = !uiState.isLoading
                            )
                        }
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
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.errorMessage != null && uiState.topics.isEmpty() -> {
                    ErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.onEvent(ForumDetailEvent.OnRefresh) }
                    )
                }
                uiState.topics.isEmpty() -> {
                    if (uiState.totalTopicCount > 0 && !uiState.showHidden) {
                        EmptyState(
                            icon = Icons.Outlined.VisibilityOff,
                            title = stringResource(R.string.forum_detail_all_hidden),
                            message = "",
                            actionLabel = stringResource(R.string.forum_detail_show_hidden),
                            onAction = { viewModel.onEvent(ForumDetailEvent.OnToggleShowHidden) }
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Forum,
                            title = stringResource(R.string.forum_detail_empty),
                            message = "",
                            actionLabel = stringResource(R.string.home_refresh),
                            onAction = { viewModel.onEvent(ForumDetailEvent.OnRefresh) }
                        )
                    }
                }
                else -> {
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

    if (showHideFullConfirm) {
        val count = uiState.fullTopicsToHide.size
        AlertDialog(
            onDismissRequest = { showHideFullConfirm = false },
            title = { Text(stringResource(R.string.forum_detail_hide_full_confirm_title)) },
            text = { 
                Text(
                    pluralStringResource(
                        R.plurals.forum_detail_hide_full_confirm_message,
                        count,
                        count
                    )
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(ForumDetailEvent.OnConfirmHideAllFull(uiState.fullTopicsToHide))
                        showHideFullConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.debug_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideFullConfirm = false }) {
                    Text(stringResource(R.string.debug_cancel))
                }
            }
        )
    }
}

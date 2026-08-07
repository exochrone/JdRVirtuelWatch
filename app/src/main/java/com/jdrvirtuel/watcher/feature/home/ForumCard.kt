package com.jdrvirtuel.watcher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
import com.jdrvirtuel.watcher.core.util.DateFormatter

@Composable
fun ForumCard(
    forum: ForumUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.md),
            verticalArrangement = Arrangement.spacedBy(Dimens.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = forum.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (forum.unreadCount > 0) {
                    Badge {
                        Text(
                            text = if (forum.unreadCount > 99) "99+" else forum.unreadCount.toString()
                        )
                    }
                }
            }

            Text(
                text = when {
                    forum.topicCount == 0 -> stringResource(R.string.home_topics_count_zero)
                    forum.topicCount == 1 -> stringResource(R.string.home_topics_count_singular)
                    else -> stringResource(R.string.home_topics_count_plural, forum.topicCount)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            val syncLabel = if (forum.lastSyncAt == null) {
                stringResource(R.string.home_never_synced)
            } else {
                stringResource(R.string.home_last_sync, DateFormatter.formatRelative(forum.lastSyncAt))
            }

            Text(
                text = syncLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (forum.hasSyncError) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Dimens.xs)
                    )
                    Text(
                        text = stringResource(R.string.home_sync_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

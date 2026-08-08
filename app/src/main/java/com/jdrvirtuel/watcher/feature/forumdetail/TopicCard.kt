package com.jdrvirtuel.watcher.feature.forumdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.component.DimmedContent
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
import com.jdrvirtuel.watcher.core.ui.theme.LocalCustomColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicCard(
    topic: TopicUiModel,
    onTopicClick: (TopicUiModel) -> Unit,
    onToggleHidden: (Int) -> Unit,
    onToggleWatched: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDimmed = topic.isFull || topic.isHidden
    val unreadColor = LocalCustomColors.current.unreadContainer

    Card(
        onClick = { onTopicClick(topic) },
        modifier = modifier.fillMaxWidth()
    ) {
        DimmedContent(isDimmed = isDimmed, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(Dimens.md)
                    .fillMaxWidth()
            ) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (topic.isRead) MaterialTheme.colorScheme.onSurface else unreadColor,
                    fontWeight = if (!topic.isRead) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Dimens.xs))

                Text(
                    text = stringResource(
                        R.string.forum_detail_topic_meta,
                        topic.createdAtLabel,
                        topic.author
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val repliesLabel = if (topic.replyCount == 0) {
                    stringResource(R.string.forum_detail_no_replies)
                } else {
                    androidx.compose.ui.res.pluralStringResource(
                        R.plurals.forum_detail_replies,
                        topic.replyCount,
                        if (topic.replyCount == 1) topic.lastPostAtLabel else topic.replyCount,
                        if (topic.replyCount == 1) topic.lastPostAuthor else topic.lastPostAtLabel,
                        topic.lastPostAuthor
                    )
                }

                Text(
                    text = repliesLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (topic.isFull) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = stringResource(R.string.forum_detail_topic_full),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            enabled = false
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { onToggleWatched(topic.id) },
                        enabled = !topic.isHidden
                    ) {
                        Icon(
                            imageVector = if (topic.isWatched) Icons.Filled.Notifications else Icons.Outlined.NotificationsOff,
                            contentDescription = if (topic.isWatched) {
                                stringResource(R.string.forum_detail_state_watched)
                            } else {
                                stringResource(R.string.forum_detail_state_not_watched)
                            }
                        )
                    }

                    IconButton(
                        onClick = { onToggleHidden(topic.id) }
                    ) {
                        Icon(
                            imageVector = if (topic.isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (topic.isHidden) {
                                stringResource(R.string.forum_detail_state_hidden)
                            } else {
                                stringResource(R.string.forum_detail_state_visible)
                            }
                        )
                    }
                }
            }
        }
    }
}

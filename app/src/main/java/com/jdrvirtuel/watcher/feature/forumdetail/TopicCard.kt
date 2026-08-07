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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!topic.isRead) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (topic.isFull) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = stringResource(R.string.forum_detail_topic_full),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.padding(start = Dimens.sm),
                            enabled = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.xs))

                Text(
                    text = stringResource(
                        R.string.forum_detail_topic_meta,
                        topic.author,
                        topic.createdAtLabel
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(
                        R.string.forum_detail_topic_last_post,
                        topic.replyCount,
                        topic.lastPostAuthor,
                        topic.lastPostAtLabel
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { onToggleWatched(topic.id) },
                        enabled = !topic.isHidden
                    ) {
                        Icon(
                            imageVector = if (topic.isWatched) Icons.Filled.Notifications else Icons.Outlined.NotificationsOff,
                            contentDescription = if (topic.isWatched) {
                                stringResource(R.string.forum_detail_watch_disabled)
                            } else {
                                stringResource(R.string.forum_detail_watch_enabled)
                            }
                        )
                    }

                    IconButton(
                        onClick = { onToggleHidden(topic.id) }
                    ) {
                        Icon(
                            imageVector = if (topic.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (topic.isHidden) {
                                stringResource(R.string.forum_detail_show_hidden)
                            } else {
                                stringResource(R.string.forum_detail_topic_masked)
                            }
                        )
                    }
                }
            }
        }
    }
}

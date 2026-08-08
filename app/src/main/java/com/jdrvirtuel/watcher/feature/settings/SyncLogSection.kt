package com.jdrvirtuel.watcher.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.theme.Dimens
import com.jdrvirtuel.watcher.core.util.DateFormatter
import com.jdrvirtuel.watcher.domain.model.SyncLogEntry
import com.jdrvirtuel.watcher.domain.model.SyncOutcome
import com.jdrvirtuel.watcher.domain.model.SyncSource
import com.jdrvirtuel.watcher.domain.model.SyncStatus

@Composable
fun SyncLogSection(
    logs: List<SyncLogEntry>,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = Dimens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_section_sync_log),
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                if (logs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_sync_log_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Dimens.md),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    logs.forEach { entry ->
                        SyncLogItem(entry = entry)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Dimens.xs),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Dimens.md))
                    
                    TextButton(
                        onClick = onClearLog,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.settings_clear_sync_log))
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncLogItem(
    entry: SyncLogEntry,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateFormatter.formatRelative(entry.timestampMs),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(Dimens.sm))
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                Text(
                    text = when (entry.source) {
                        SyncSource.MANUAL -> stringResource(R.string.sync_source_manual)
                        SyncSource.PERIODIC -> stringResource(R.string.sync_source_auto)
                        SyncSource.TEST -> stringResource(R.string.sync_source_test)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        entry.forumResults.forEach { result ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (result.status) {
                        SyncStatus.SUCCESS -> Icons.Default.CheckCircle
                        SyncStatus.CHALLENGE_REQUIRED -> Icons.Default.Warning
                        SyncStatus.ERROR -> Icons.Default.Error
                    },
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = when (result.status) {
                        SyncStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                        SyncStatus.CHALLENGE_REQUIRED -> MaterialTheme.colorScheme.error
                        SyncStatus.ERROR -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(Dimens.sm))
                Text(
                    text = result.forumName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = when (result.status) {
                        SyncStatus.SUCCESS -> if (result.newTopicsCount > 0) "+${result.newTopicsCount}" else stringResource(R.string.sync_status_ok)
                        SyncStatus.CHALLENGE_REQUIRED -> stringResource(R.string.sync_status_blocked)
                        SyncStatus.ERROR -> stringResource(R.string.sync_status_error)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.status == SyncStatus.SUCCESS && result.newTopicsCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

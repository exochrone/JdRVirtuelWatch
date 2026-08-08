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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.jdrvirtuel.watcher.domain.model.NotificationLogEntry
import com.jdrvirtuel.watcher.domain.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationLogSection(
    logs: List<NotificationLogEntry>,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = Dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_section_notification_log),
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
                        text = stringResource(R.string.settings_notification_log_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Dimens.md),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val dateFormat = remember { SimpleDateFormat("dd/MM/yy - HH:mm:ss", Locale.getDefault()) }
                    logs.forEach { entry ->
                        NotificationLogItem(entry = entry, dateFormat = dateFormat)
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
                        Text(stringResource(R.string.settings_clear_notification_log))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationLogItem(
    entry: NotificationLogEntry,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.sm)
    ) {
        val typeStr = when (entry.type) {
            NotificationType.NEW_TOPIC -> stringResource(R.string.notification_type_new_topic)
            NotificationType.NEW_REPLY -> stringResource(R.string.notification_type_new_reply)
            NotificationType.VERIFICATION -> stringResource(R.string.notification_type_verification)
        }
        
        val header = buildString {
            append(dateFormat.format(Date(entry.timestampMs)))
            append(" · ")
            append(typeStr)
            if (entry.forumName != null) {
                append(" · ")
                append(entry.forumName)
            }
        }
        
        Text(
            text = header,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        
        if (entry.topicTitle != null) {
            Text(
                text = entry.topicTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

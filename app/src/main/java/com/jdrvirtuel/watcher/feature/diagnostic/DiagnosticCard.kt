package com.jdrvirtuel.watcher.feature.diagnostic

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jdrvirtuel.watcher.core.ui.theme.Dimens

@Composable
fun DiagnosticCard(
    title: String,
    description: String,
    isConform: Boolean,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConform) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimens.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConform) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isConform) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.md))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConform) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(Dimens.sm))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!isConform && (actionLabel != null || secondaryActionLabel != null)) {
                Spacer(modifier = Modifier.height(Dimens.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (secondaryActionLabel != null) {
                        TextButton(onClick = onSecondaryActionClick) {
                            Text(secondaryActionLabel)
                        }
                        Spacer(modifier = Modifier.width(Dimens.sm))
                    }
                    if (actionLabel != null) {
                        Button(onClick = onActionClick) {
                            Text(actionLabel)
                        }
                    }
                }
            }
        }
    }
}

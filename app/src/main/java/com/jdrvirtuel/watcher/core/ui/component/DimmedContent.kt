package com.jdrvirtuel.watcher.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

@Composable
fun DimmedContent(
    isDimmed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isDimmed) {
        Box(modifier = modifier.alpha(0.5f)) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                content()
            }
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

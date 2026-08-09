package com.jdrvirtuel.watcher.feature.diagnostic

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdrvirtuel.watcher.R
import com.jdrvirtuel.watcher.core.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    onNavigateToHome: () -> Unit,
    viewModel: DiagnosticViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            val activity = context as? Activity
            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                if (!showRationale) {
                    // Refused permanently
                    viewModel.onNotificationPermissionRefusedPermanently()
                }
            }
        }
        viewModel.onEvent(DiagnosticEvent.OnResume)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(DiagnosticEvent.OnResume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DiagnosticEffect.NavigateToHome -> onNavigateToHome()
                DiagnosticEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                DiagnosticEffect.OpenNotificationSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
                DiagnosticEffect.RequestBatteryOptimizationExemption -> {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
                DiagnosticEffect.OpenApplicationDetails -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostic_title)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Dimens.md)
        ) {
            Text(
                text = stringResource(R.string.diagnostic_intro),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(Dimens.md))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                item {
                    DiagnosticCard(
                        title = stringResource(R.string.diagnostic_card_notifications_title),
                        description = stringResource(R.string.diagnostic_card_notifications_desc),
                        isConform = uiState.isNotificationEnabled,
                        actionLabel = stringResource(R.string.diagnostic_card_notifications_action),
                        onActionClick = { viewModel.onEvent(DiagnosticEvent.OnRequestNotificationPermission) }
                    )
                }
                
                item {
                    DiagnosticCard(
                        title = stringResource(R.string.diagnostic_card_battery_title),
                        description = stringResource(R.string.diagnostic_card_battery_desc),
                        isConform = uiState.isBatteryOptimizationIgnored,
                        actionLabel = stringResource(R.string.diagnostic_card_battery_action),
                        onActionClick = { viewModel.onEvent(DiagnosticEvent.OnRequestBatteryOptimizationExemption) }
                    )
                }
                
                item {
                    DiagnosticCard(
                        title = stringResource(R.string.diagnostic_card_manufacturer_title),
                        description = stringResource(R.string.diagnostic_card_manufacturer_desc),
                        isConform = uiState.isManufacturerSleepAcknowledged,
                        actionLabel = stringResource(R.string.diagnostic_card_manufacturer_action_done),
                        onActionClick = { viewModel.onEvent(DiagnosticEvent.OnAcknowledgeManufacturerSleep) },
                        secondaryActionLabel = stringResource(R.string.diagnostic_card_manufacturer_action_open),
                        onSecondaryActionClick = { viewModel.onEvent(DiagnosticEvent.OnOpenManufacturerSettings) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.isDismissed,
                    onCheckedChange = { viewModel.onEvent(DiagnosticEvent.OnDismissChange(it)) }
                )
                Text(
                    text = stringResource(R.string.diagnostic_dismiss_label),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(Dimens.md))
            
            Button(
                onClick = { viewModel.onEvent(DiagnosticEvent.OnContinue) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.diagnostic_continue))
            }
        }
    }
}

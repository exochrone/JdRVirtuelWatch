package com.jdrvirtuel.watcher.feature.diagnostic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrvirtuel.watcher.core.util.SystemSettingsChecker
import com.jdrvirtuel.watcher.data.local.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticViewModel @Inject constructor(
    private val systemSettingsChecker: SystemSettingsChecker,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<DiagnosticUiState> = combine(
        _refreshTrigger,
        appPreferences.isManufacturerSleepAcknowledged,
        appPreferences.isDiagnosticDismissed
    ) { _, sleepAck, dismissed ->
        DiagnosticUiState(
            isNotificationEnabled = systemSettingsChecker.areNotificationsEnabled(),
            isBatteryOptimizationIgnored = systemSettingsChecker.isIgnoringBatteryOptimizations(),
            isManufacturerSleepAcknowledged = sleepAck,
            isDismissed = dismissed,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiagnosticUiState()
    )

    private val _effects = Channel<DiagnosticEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: DiagnosticEvent) {
        when (event) {
            DiagnosticEvent.OnResume -> {
                _refreshTrigger.value += 1
            }
            DiagnosticEvent.OnRequestNotificationPermission -> {
                viewModelScope.launch {
                    if (systemSettingsChecker.areNotificationsEnabled()) {
                        // Already granted
                        _refreshTrigger.value += 1
                    } else {
                        // We check if we should show rationale or just request
                        // For simplicity, we emit an effect and let the UI handle it.
                        // The spec says: "déclenche la demande de permission... ou ouvre les réglages si déjà refusée"
                        // This logic often requires Activity context, so we emit a general effect.
                        _effects.send(DiagnosticEffect.RequestNotificationPermission)
                    }
                }
            }
            DiagnosticEvent.OnRequestBatteryOptimizationExemption -> {
                viewModelScope.launch {
                    _effects.send(DiagnosticEffect.RequestBatteryOptimizationExemption)
                }
            }
            DiagnosticEvent.OnOpenManufacturerSettings -> {
                viewModelScope.launch {
                    _effects.send(DiagnosticEffect.OpenApplicationDetails)
                }
            }
            DiagnosticEvent.OnAcknowledgeManufacturerSleep -> {
                viewModelScope.launch {
                    appPreferences.setManufacturerSleepAcknowledged(true)
                }
            }
            is DiagnosticEvent.OnDismissChange -> {
                viewModelScope.launch {
                    appPreferences.setDiagnosticDismissed(event.dismissed)
                }
            }
            DiagnosticEvent.OnContinue -> {
                viewModelScope.launch {
                    _effects.send(DiagnosticEffect.NavigateToHome)
                }
            }
        }
    }
    
    fun onNotificationPermissionRefusedPermanently() {
        viewModelScope.launch {
            _effects.send(DiagnosticEffect.OpenNotificationSettings)
        }
    }
}

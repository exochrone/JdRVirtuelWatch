package com.jdrvirtuel.watcher.feature.diagnostic

data class DiagnosticUiState(
    val isNotificationEnabled: Boolean = true,
    val isBatteryOptimizationIgnored: Boolean = true,
    val isManufacturerSleepAcknowledged: Boolean = true,
    val isDismissed: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface DiagnosticEvent {
    val type: String
        get() = this::class.java.simpleName

    data object OnResume : DiagnosticEvent
    data object OnRequestNotificationPermission : DiagnosticEvent
    data object OnRequestBatteryOptimizationExemption : DiagnosticEvent
    data object OnOpenManufacturerSettings : DiagnosticEvent
    data object OnAcknowledgeManufacturerSleep : DiagnosticEvent
    data class OnDismissChange(val dismissed: Boolean) : DiagnosticEvent
    data object OnContinue : DiagnosticEvent
}

sealed interface DiagnosticEffect {
    data object NavigateToHome : DiagnosticEffect
    data object RequestNotificationPermission : DiagnosticEffect
    data object OpenNotificationSettings : DiagnosticEffect
    data object RequestBatteryOptimizationExemption : DiagnosticEffect
    data object OpenApplicationDetails : DiagnosticEffect
}

package com.jdrvirtuel.watcher.feature.verification

data class VerificationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

sealed interface VerificationEvent {
    data object BackClicked : VerificationEvent
    data class PageFinished(val html: String) : VerificationEvent
}

sealed interface VerificationEffect {
    data object NavigateBack : VerificationEffect
    data class ShowMessage(val message: String) : VerificationEffect
}

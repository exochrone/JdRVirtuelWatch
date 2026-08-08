package com.jdrvirtuel.watcher.feature.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrvirtuel.watcher.data.remote.WebViewConstants
import com.jdrvirtuel.watcher.domain.repository.ChallengeStateRepository
import com.jdrvirtuel.watcher.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val challengeRepository: ChallengeStateRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val _effect = Channel<VerificationEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: VerificationEvent) {
        when (event) {
            is VerificationEvent.BackClicked -> {
                viewModelScope.launch {
                    _effect.send(VerificationEffect.NavigateBack)
                }
            }
            is VerificationEvent.PageFinished -> {
                if (event.html.contains(WebViewConstants.SUCCESS_MARKER)) {
                    handleSuccess()
                }
            }
        }
    }

    private fun handleSuccess() {
        if (_uiState.value.isSuccess) return
        _uiState.update { it.copy(isSuccess = true) }

        viewModelScope.launch {
            challengeRepository.resetFailures()
            syncScheduler.reschedulePeriodicSync(isLongPeriod = false)
            syncScheduler.triggerImmediateSync()
            
            _effect.send(VerificationEffect.ShowMessage("Vérification réussie"))
            _effect.send(VerificationEffect.NavigateBack)
        }
    }
}

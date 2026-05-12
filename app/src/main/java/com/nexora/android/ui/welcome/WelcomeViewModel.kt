package com.nexora.android.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.auth.PendingSignupVerification
import com.nexora.android.data.auth.PendingSignupVerificationStore
import com.nexora.android.domain.session.NexoraResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pendingVerificationStore: PendingSignupVerificationStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    init {
        pendingVerificationStore.pendingVerification()
            .onEach { pending ->
                _uiState.update { it.copy(pendingVerification = pending) }
            }
            .launchIn(viewModelScope)
    }

    fun resendCode() {
        val pending = _uiState.value.pendingVerification ?: return
        if (_uiState.value.isResending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, errorMessage = null, infoMessage = null) }
            when (val result = authRepository.resendSignupOtp(pending.email)) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(isResending = false, errorMessage = result.error.message)
                }
                is NexoraResult.Success -> _uiState.update {
                    it.copy(isResending = false, infoMessage = "A new code has been sent.")
                }
            }
        }
    }

    fun clearPendingVerification() {
        viewModelScope.launch {
            pendingVerificationStore.clearAll()
            _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
        }
    }
}

data class WelcomeUiState(
    val pendingVerification: PendingSignupVerification? = null,
    val isResending: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

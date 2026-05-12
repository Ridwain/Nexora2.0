package com.nexora.android.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.core.session.SessionRepository
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.auth.PendingSignupVerificationStore
import com.nexora.android.domain.session.NexoraResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerifyEmailOtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val pendingVerificationStore: PendingSignupVerificationStore
) : ViewModel() {
    private val email: String = checkNotNull(savedStateHandle["email"])

    private val _uiState = MutableStateFlow(VerifyEmailOtpUiState(email = email))
    val uiState: StateFlow<VerifyEmailOtpUiState> = _uiState.asStateFlow()

    fun onOtpChanged(value: String) {
        _uiState.update { it.copy(otp = AuthInputRules.otp(value), errorMessage = null, infoMessage = null) }
    }

    fun verify() {
        val state = _uiState.value
        if (state.isLoading || state.isResending) return

        AuthFormValidator.validateOtp(state.otp)?.let { error ->
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            when (val result = authRepository.verifySignupOtp(email = email, token = state.otp)) {
                is NexoraResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
                is NexoraResult.Success -> {
                    sessionRepository.clearSession()
                    pendingVerificationStore.markVerified(email)
                    _uiState.update {
                        it.copy(isLoading = false, navigationTarget = AuthNavigationTarget.Login)
                    }
                }
            }
        }
    }

    fun resend() {
        val state = _uiState.value
        if (state.isLoading || state.isResending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, errorMessage = null, infoMessage = null) }
            when (val result = authRepository.resendSignupOtp(email)) {
                is NexoraResult.Failure -> {
                    _uiState.update { it.copy(isResending = false, errorMessage = result.error.message) }
                }
                is NexoraResult.Success -> {
                    _uiState.update {
                        it.copy(isResending = false, infoMessage = "A new code has been sent.")
                    }
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigationTarget = null) }
    }
}

data class VerifyEmailOtpUiState(
    val email: String,
    val otp: String = "",
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val navigationTarget: AuthNavigationTarget? = null
)

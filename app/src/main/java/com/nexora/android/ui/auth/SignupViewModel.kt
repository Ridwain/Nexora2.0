package com.nexora.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.core.session.SessionRepository
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.auth.PendingSignupVerificationStore
import com.nexora.android.domain.session.NexoraResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val pendingVerificationStore: PendingSignupVerificationStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onDisplayNameChanged(value: String) {
        _uiState.update {
            it.copy(
                displayName = AuthInputRules.displayName(value),
                errorMessage = null,
                showVerifyEmailAction = false
            )
        }
    }

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                email = AuthInputRules.email(value),
                errorMessage = null,
                showVerifyEmailAction = false
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = AuthInputRules.password(value),
                errorMessage = null,
                showVerifyEmailAction = false
            )
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = AuthInputRules.password(value),
                errorMessage = null,
                showVerifyEmailAction = false
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        AuthFormValidator.validateSignup(
            displayName = state.displayName,
            email = state.email,
            password = state.password,
            confirmPassword = state.confirmPassword
        )?.let { error ->
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val signUp = authRepository.signUp(state.email, state.password)) {
                is NexoraResult.Failure -> {
                    val canRecoverVerification = signUp.error.isRecoverableSignupVerificationError()
                    if (canRecoverVerification) {
                        pendingVerificationStore.save(
                            email = state.email,
                            displayName = state.displayName,
                            createdAtEpochSeconds = Instant.now().epochSecond
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (canRecoverVerification) {
                                "This email already started signup. Verify your email or request a new code."
                            } else {
                                signUp.error.message
                            },
                            showVerifyEmailAction = canRecoverVerification
                        )
                    }
                }
                is NexoraResult.Success -> {
                    pendingVerificationStore.save(
                        email = state.email,
                        displayName = state.displayName,
                        createdAtEpochSeconds = Instant.now().epochSecond
                    )
                    sessionRepository.clearSession()
                    _uiState.update {
                        it.copy(isLoading = false, navigationTarget = AuthNavigationTarget.VerifyEmailOtp)
                    }
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigationTarget = null) }
    }

    private fun com.nexora.android.domain.session.NexoraError.isRecoverableSignupVerificationError(): Boolean {
        val text = listOfNotNull(message, code).joinToString(" ").lowercase()
        return "already" in text || "registered" in text || "exists" in text || "confirmed" in text
    }
}

data class SignupUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showVerifyEmailAction: Boolean = false,
    val navigationTarget: AuthNavigationTarget? = null
)

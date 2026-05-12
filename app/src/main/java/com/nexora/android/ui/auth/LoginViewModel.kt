package com.nexora.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.auth.PendingSignupVerificationStore
import com.nexora.android.data.rpc.RpcRepository
import com.nexora.android.domain.session.NexoraError
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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val rpcRepository: RpcRepository,
    private val pendingVerificationStore: PendingSignupVerificationStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        AuthFormValidator.validateLogin(state.email, state.password)?.let { error ->
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val signIn = authRepository.signIn(state.email, state.password)) {
                is NexoraResult.Failure -> {
                    val isUnverifiedEmail = signIn.error.isUnverifiedEmailError()
                    if (isUnverifiedEmail) {
                        pendingVerificationStore.save(
                            email = state.email,
                            displayName = pendingVerificationStore.displayNameFor(state.email).orEmpty(),
                            createdAtEpochSeconds = Instant.now().epochSecond
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (isUnverifiedEmail) {
                                "Email is not verified yet. Enter the email code to continue."
                            } else {
                                signIn.error.message
                            },
                            showVerifyEmailAction = isUnverifiedEmail
                        )
                    }
                }
                is NexoraResult.Success -> {
                    val pendingDisplayName = pendingVerificationStore.displayNameFor(state.email)
                    when (val profile = rpcRepository.ensureUserProfile(displayName = pendingDisplayName)) {
                        is NexoraResult.Failure -> _uiState.update {
                            it.copy(isLoading = false, errorMessage = profile.error.message)
                        }
                        is NexoraResult.Success -> {
                            if (!pendingDisplayName.isNullOrBlank()) {
                                pendingVerificationStore.clear(state.email)
                            }
                            when (val contexts = rpcRepository.getUserContexts()) {
                                is NexoraResult.Failure -> _uiState.update {
                                    it.copy(isLoading = false, errorMessage = contexts.error.message)
                                }
                                is NexoraResult.Success -> _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        navigationTarget = AuthNavigationTarget.ContextPicker
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigationTarget = null) }
    }

    private fun NexoraError.isUnverifiedEmailError(): Boolean {
        val text = listOfNotNull(message, code).joinToString(" ").lowercase()
        return ("email" in text && ("confirm" in text || "verified" in text || "verify" in text)) ||
            "email_not_confirmed" in text
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showVerifyEmailAction: Boolean = false,
    val navigationTarget: AuthNavigationTarget? = null
)

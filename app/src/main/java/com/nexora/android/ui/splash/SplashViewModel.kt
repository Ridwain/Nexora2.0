package com.nexora.android.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.core.session.SessionRepository
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.rpc.RpcRepository
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.ui.auth.AuthNavigationTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val rpcRepository: RpcRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            val storedSession = sessionRepository.currentSessionNow()
            var destination = AuthNavigationTarget.Welcome
            if (storedSession != null) {
                val refreshed = authRepository.refreshSession(storedSession.refreshToken)
                if (refreshed is NexoraResult.Success) {
                    rpcRepository.getUserContexts()
                    destination = AuthNavigationTarget.ContextPicker
                }
            }

            _uiState.update { it.copy(isFinished = true, destination = destination) }
        }
    }
}

data class SplashUiState(
    val isFinished: Boolean = false,
    val destination: AuthNavigationTarget = AuthNavigationTarget.Welcome
)

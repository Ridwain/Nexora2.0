package com.nexora.android.ui.context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.core.deeplink.DeepLinkRepository
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.rpc.RpcRepository
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.domain.session.PendingInvite
import com.nexora.android.domain.session.UserContext
import com.nexora.android.ui.auth.AuthNavigationTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContextPickerViewModel @Inject constructor(
    private val rpcRepository: RpcRepository,
    private val authRepository: AuthRepository,
    private val deepLinkRepository: DeepLinkRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContextPickerUiState())
    val uiState: StateFlow<ContextPickerUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val pendingInvite = deepLinkRepository.currentInvite()
            when (val contexts = rpcRepository.getUserContexts()) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingInvite = pendingInvite,
                        errorMessage = contexts.error.message
                    )
                }
                is NexoraResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        contexts = contexts.value,
                        pendingInvite = pendingInvite,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun logout() {
        val state = _uiState.value
        if (state.isLoggingOut) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, errorMessage = null) }
            authRepository.signOut()
            _uiState.update {
                it.copy(
                    isLoggingOut = false,
                    navigationTarget = AuthNavigationTarget.Welcome
                )
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigationTarget = null) }
    }
}

data class ContextPickerUiState(
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val contexts: List<UserContext> = emptyList(),
    val pendingInvite: PendingInvite? = null,
    val errorMessage: String? = null,
    val navigationTarget: AuthNavigationTarget? = null
)

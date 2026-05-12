package com.nexora.android.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.rpc.RpcRepository
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.domain.session.UserContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateOwnerTenantViewModel @Inject constructor(
    private val rpcRepository: RpcRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateOwnerTenantUiState())
    val uiState: StateFlow<CreateOwnerTenantUiState> = _uiState.asStateFlow()

    fun onCompanyNameChanged(value: String) {
        _uiState.update { it.copy(companyName = OwnerInputRules.companyName(value), errorMessage = null) }
    }

    fun onIndustryChanged(value: String) {
        _uiState.update { it.copy(industry = OwnerInputRules.industry(value), errorMessage = null) }
    }

    fun onCountryChanged(value: String) {
        _uiState.update { it.copy(country = OwnerInputRules.country(value), errorMessage = null) }
    }

    fun onCompanyEmailChanged(value: String) {
        _uiState.update { it.copy(companyEmail = OwnerInputRules.email(value), errorMessage = null) }
    }

    fun onCompanyPhoneChanged(value: String) {
        _uiState.update { it.copy(companyPhone = OwnerInputRules.phone(value), errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        validate(state)?.let { error ->
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = rpcRepository.createOwnerTenant(
                name = state.companyName.trim(),
                industry = state.industry.trim().ifBlank { null },
                country = state.country.trim().ifBlank { null },
                timezone = ZoneId.systemDefault().id,
                email = state.companyEmail.trim().ifBlank { null },
                phone = state.companyPhone.trim().ifBlank { null }
            )) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
                is NexoraResult.Success -> _uiState.update {
                    it.copy(isLoading = false, createdContext = result.value)
                }
            }
        }
    }

    fun consumeCreatedContext() {
        _uiState.update { it.copy(createdContext = null) }
    }

    private fun validate(state: CreateOwnerTenantUiState): String? {
        if (state.companyName.isBlank()) return "Enter a company name."
        if (state.companyEmail.isNotBlank() && "@" !in state.companyEmail) {
            return "Enter a valid company email."
        }
        return null
    }
}

data class CreateOwnerTenantUiState(
    val companyName: String = "",
    val industry: String = "",
    val country: String = "",
    val companyEmail: String = "",
    val companyPhone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdContext: UserContext? = null
)

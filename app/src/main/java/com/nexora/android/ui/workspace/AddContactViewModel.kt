package com.nexora.android.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.rpc.RpcRepository
import com.nexora.android.domain.session.CrmContact
import com.nexora.android.domain.session.NexoraResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val rpcRepository: RpcRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()

    fun onFirstNameChanged(value: String) {
        _uiState.update { it.copy(firstName = ContactInputRules.name(value), errorMessage = null) }
    }

    fun onLastNameChanged(value: String) {
        _uiState.update { it.copy(lastName = ContactInputRules.name(value), errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = ContactInputRules.email(value), errorMessage = null) }
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phone = ContactInputRules.phone(value), errorMessage = null) }
    }

    fun onCompanyNameChanged(value: String) {
        _uiState.update { it.copy(companyName = ContactInputRules.companyName(value), errorMessage = null) }
    }

    fun onJobTitleChanged(value: String) {
        _uiState.update { it.copy(jobTitle = ContactInputRules.jobTitle(value), errorMessage = null) }
    }

    fun onSourceChanged(value: String) {
        _uiState.update { it.copy(source = ContactInputRules.source(value), errorMessage = null) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = ContactInputRules.notes(value), errorMessage = null) }
    }

    fun submit(tenantId: String) {
        val state = _uiState.value
        if (state.isLoading) return

        validate(state, tenantId)?.let { error ->
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = rpcRepository.createCrmContact(
                tenantId = tenantId,
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim().ifBlank { null },
                email = state.email.trim().ifBlank { null },
                phone = state.phone.trim().ifBlank { null },
                companyName = state.companyName.trim().ifBlank { null },
                jobTitle = state.jobTitle.trim().ifBlank { null },
                source = state.source.trim().ifBlank { null },
                notes = state.notes.trim().ifBlank { null }
            )) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
                is NexoraResult.Success -> _uiState.update {
                    it.copy(isLoading = false, createdContact = result.value)
                }
            }
        }
    }

    fun consumeCreatedContact() {
        _uiState.update { it.copy(createdContact = null) }
    }

    private fun validate(state: AddContactUiState, tenantId: String): String? {
        if (tenantId.isBlank()) return "Missing tenant context."
        if (state.firstName.isBlank()) return "Enter a first name."
        if (state.email.isNotBlank() && "@" !in state.email) return "Enter a valid email."
        return null
    }
}

data class AddContactUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val companyName: String = "",
    val jobTitle: String = "",
    val source: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdContact: CrmContact? = null
)

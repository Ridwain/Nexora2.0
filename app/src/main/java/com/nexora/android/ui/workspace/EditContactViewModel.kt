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
class EditContactViewModel @Inject constructor(
    private val rpcRepository: RpcRepository,
    private val contactsMemoryCache: ContactsMemoryCache
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditContactUiState())
    val uiState: StateFlow<EditContactUiState> = _uiState.asStateFlow()

    fun load(tenantId: String, contactId: String) {
        if (_uiState.value.loadedContactId == contactId) return

        contactsMemoryCache.contactFor(tenantId, contactId)?.let { contact ->
            _uiState.value = contact.toEditState(isLoading = false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = rpcRepository.getCrmContact(tenantId, contactId)) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
                is NexoraResult.Success -> {
                    contactsMemoryCache.upsert(tenantId, result.value)
                    _uiState.value = result.value.toEditState(isLoading = false)
                }
            }
        }
    }

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

    fun onLifecycleStageChanged(value: String) {
        _uiState.update { it.copy(lifecycleStage = ContactInputRules.lifecycleStage(value), errorMessage = null) }
    }

    fun onLeadStatusChanged(value: String) {
        _uiState.update { it.copy(leadStatus = ContactInputRules.leadStatus(value), errorMessage = null) }
    }

    fun onSourceChanged(value: String) {
        _uiState.update { it.copy(source = ContactInputRules.source(value), errorMessage = null) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = ContactInputRules.notes(value), errorMessage = null) }
    }

    fun save(tenantId: String, contactId: String) {
        val state = _uiState.value
        if (state.isSaving) return

        validate(state, tenantId, contactId)?.let { error ->
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = rpcRepository.updateCrmContact(
                tenantId = tenantId,
                contactId = contactId,
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim().ifBlank { null },
                email = state.email.trim().ifBlank { null },
                phone = state.phone.trim().ifBlank { null },
                companyName = state.companyName.trim().ifBlank { null },
                jobTitle = state.jobTitle.trim().ifBlank { null },
                lifecycleStage = state.lifecycleStage,
                leadStatus = state.leadStatus,
                source = state.source.trim().ifBlank { null },
                notes = state.notes.trim().ifBlank { null }
            )) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.error.message)
                }
                is NexoraResult.Success -> {
                    contactsMemoryCache.upsert(tenantId, result.value)
                    _uiState.update {
                        it.copy(isSaving = false, savedContact = result.value)
                    }
                }
            }
        }
    }

    fun consumeSavedContact() {
        _uiState.update { it.copy(savedContact = null) }
    }

    private fun validate(state: EditContactUiState, tenantId: String, contactId: String): String? {
        if (tenantId.isBlank() || contactId.isBlank()) return "Missing contact context."
        if (state.firstName.isBlank()) return "Enter a first name."
        if (state.email.isNotBlank() && "@" !in state.email) return "Enter a valid email."
        return null
    }

    private fun CrmContact.toEditState(isLoading: Boolean): EditContactUiState = EditContactUiState(
        loadedContactId = id,
        firstName = firstName,
        lastName = lastName.orEmpty(),
        email = email.orEmpty(),
        phone = phone.orEmpty(),
        companyName = companyName.orEmpty(),
        jobTitle = jobTitle.orEmpty(),
        lifecycleStage = lifecycleStage,
        leadStatus = leadStatus,
        source = source.orEmpty(),
        notes = notes.orEmpty(),
        isLoading = isLoading
    )
}

data class EditContactUiState(
    val loadedContactId: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val companyName: String = "",
    val jobTitle: String = "",
    val lifecycleStage: String = "lead",
    val leadStatus: String = "new",
    val source: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedContact: CrmContact? = null
)

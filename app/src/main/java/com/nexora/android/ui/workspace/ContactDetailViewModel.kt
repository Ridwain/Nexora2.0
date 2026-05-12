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
class ContactDetailViewModel @Inject constructor(
    private val rpcRepository: RpcRepository,
    private val contactsMemoryCache: ContactsMemoryCache
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    fun load(tenantId: String, contactId: String) {
        if (tenantId.isBlank() || contactId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Missing contact context.") }
            return
        }

        val currentState = _uiState.value
        if (currentState.isInitialLoading || currentState.isRefreshing) return

        val cachedContact = contactsMemoryCache.contactFor(tenantId, contactId)
        val hasCachedContact = cachedContact != null

        _uiState.update {
            it.copy(
                contact = cachedContact ?: it.contact,
                isInitialLoading = !hasCachedContact && it.contact == null,
                isRefreshing = hasCachedContact || it.contact != null,
                errorMessage = null,
                refreshErrorMessage = null,
                archived = false
            )
        }

        viewModelScope.launch {
            when (val result = rpcRepository.getCrmContact(tenantId, contactId)) {
                is NexoraResult.Failure -> _uiState.update {
                    if (it.contact == null) {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            errorMessage = result.error.message
                        )
                    } else {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            refreshErrorMessage = result.error.message
                        )
                    }
                }
                is NexoraResult.Success -> {
                    contactsMemoryCache.upsert(tenantId, result.value)
                    _uiState.update {
                        it.copy(
                            contact = result.value,
                            isInitialLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            refreshErrorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun showArchiveDialog() {
        _uiState.update { it.copy(showArchiveDialog = true) }
    }

    fun hideArchiveDialog() {
        _uiState.update { it.copy(showArchiveDialog = false) }
    }

    fun archive(tenantId: String, contactId: String) {
        val state = _uiState.value
        if (state.isArchiving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isArchiving = true, archiveErrorMessage = null) }
            when (val result = rpcRepository.archiveCrmContact(tenantId, contactId)) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(
                        isArchiving = false,
                        archiveErrorMessage = result.error.message,
                        showArchiveDialog = false
                    )
                }
                is NexoraResult.Success -> {
                    contactsMemoryCache.remove(tenantId, contactId)
                    _uiState.update {
                        it.copy(
                            isArchiving = false,
                            showArchiveDialog = false,
                            archived = true,
                            contact = result.value
                        )
                    }
                }
            }
        }
    }
}

data class ContactDetailUiState(
    val contact: CrmContact? = null,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val refreshErrorMessage: String? = null,
    val showArchiveDialog: Boolean = false,
    val isArchiving: Boolean = false,
    val archiveErrorMessage: String? = null,
    val archived: Boolean = false
)

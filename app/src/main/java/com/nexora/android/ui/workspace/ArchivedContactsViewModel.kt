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
class ArchivedContactsViewModel @Inject constructor(
    private val rpcRepository: RpcRepository,
    private val contactsMemoryCache: ContactsMemoryCache
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArchivedContactsUiState())
    val uiState: StateFlow<ArchivedContactsUiState> = _uiState.asStateFlow()

    fun load(tenantId: String) {
        if (tenantId.isBlank()) {
            _uiState.update {
                it.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = "Missing tenant context."
                )
            }
            return
        }

        val currentState = _uiState.value
        if (
            currentState.tenantId == tenantId &&
            (currentState.isInitialLoading || currentState.isRefreshing)
        ) {
            return
        }

        val hasScreenCache = currentState.tenantId == tenantId && currentState.hasLoaded
        val memoryCache = if (hasScreenCache) null else contactsMemoryCache.archivedContactsFor(tenantId)
        val hasCachedData = hasScreenCache || memoryCache != null
        val cachedContacts = if (hasScreenCache) currentState.contacts else memoryCache.orEmpty()

        _uiState.update {
            if (hasCachedData) {
                it.copy(
                    tenantId = tenantId,
                    hasLoaded = true,
                    isInitialLoading = false,
                    isRefreshing = true,
                    contacts = cachedContacts,
                    errorMessage = null,
                    refreshErrorMessage = null
                )
            } else {
                ArchivedContactsUiState(
                    tenantId = tenantId,
                    isInitialLoading = true
                )
            }
        }

        viewModelScope.launch {
            when (val result = rpcRepository.listArchivedCrmContacts(tenantId)) {
                is NexoraResult.Failure -> _uiState.update {
                    if (it.tenantId != tenantId) {
                        it
                    } else if (it.hasLoaded) {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            refreshErrorMessage = result.error.message
                        )
                    } else {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            errorMessage = result.error.message
                        )
                    }
                }
                is NexoraResult.Success -> {
                    contactsMemoryCache.putArchived(tenantId, result.value)
                    _uiState.update {
                        if (it.tenantId != tenantId) {
                            it
                        } else {
                            it.copy(
                                hasLoaded = true,
                                isInitialLoading = false,
                                isRefreshing = false,
                                contacts = result.value,
                                errorMessage = null,
                                refreshErrorMessage = null
                            )
                        }
                    }
                }
            }
        }
    }

    fun showRestoreDialog(contact: CrmContact) {
        _uiState.update { it.copy(contactPendingRestore = contact, restoreErrorMessage = null) }
    }

    fun hideRestoreDialog() {
        _uiState.update { it.copy(contactPendingRestore = null) }
    }

    fun restore(tenantId: String, contactId: String) {
        val state = _uiState.value
        if (state.isRestoring) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, restoreErrorMessage = null) }
            when (val result = rpcRepository.restoreCrmContact(tenantId, contactId)) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(
                        isRestoring = false,
                        restoreErrorMessage = result.error.message
                    )
                }
                is NexoraResult.Success -> {
                    contactsMemoryCache.removeArchived(tenantId, contactId)
                    contactsMemoryCache.upsert(tenantId, result.value)
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            contactPendingRestore = null,
                            contacts = it.contacts.filterNot { contact -> contact.id == contactId },
                            restoreErrorMessage = null
                        )
                    }
                }
            }
        }
    }
}

data class ArchivedContactsUiState(
    val tenantId: String? = null,
    val hasLoaded: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contacts: List<CrmContact> = emptyList(),
    val errorMessage: String? = null,
    val refreshErrorMessage: String? = null,
    val contactPendingRestore: CrmContact? = null,
    val isRestoring: Boolean = false,
    val restoreErrorMessage: String? = null
)

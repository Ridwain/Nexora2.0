package com.nexora.android.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.rpc.RpcRepository
import com.nexora.android.domain.session.ContactTimelineItem
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

        loadTimeline(tenantId, contactId)
    }

    fun loadTimeline(tenantId: String, contactId: String) {
        if (tenantId.isBlank() || contactId.isBlank()) {
            _uiState.update { it.copy(timelineErrorMessage = "Missing contact context.") }
            return
        }

        val state = _uiState.value
        if (state.isTimelineLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isTimelineLoading = true, timelineErrorMessage = null) }
            when (val result = rpcRepository.listContactTimeline(tenantId, contactId)) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(
                        isTimelineLoading = false,
                        timelineErrorMessage = result.error.message
                    )
                }
                is NexoraResult.Success -> _uiState.update {
                    it.copy(
                        isTimelineLoading = false,
                        timelineItems = result.value,
                        timelineErrorMessage = null
                    )
                }
            }
        }
    }

    fun onNoteBodyChanged(body: String) {
        _uiState.update {
            it.copy(
                noteBody = body.take(MaxNoteBodyLength),
                addNoteErrorMessage = null
            )
        }
    }

    fun addNote(tenantId: String, contactId: String) {
        val state = _uiState.value
        val body = state.noteBody.trim()
        if (state.isAddingNote) return
        if (body.isBlank()) {
            _uiState.update { it.copy(addNoteErrorMessage = "Write a note before adding it.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingNote = true, addNoteErrorMessage = null) }
            when (val result = rpcRepository.createContactNote(tenantId, contactId, body)) {
                is NexoraResult.Failure -> _uiState.update {
                    it.copy(
                        isAddingNote = false,
                        addNoteErrorMessage = result.error.message
                    )
                }
                is NexoraResult.Success -> _uiState.update {
                    it.copy(
                        isAddingNote = false,
                        noteBody = "",
                        timelineItems = listOf(result.value) + it.timelineItems,
                        addNoteErrorMessage = null,
                        timelineErrorMessage = null
                    )
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

    companion object {
        const val MaxNoteBodyLength = 2000
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
    val archived: Boolean = false,
    val timelineItems: List<ContactTimelineItem> = emptyList(),
    val isTimelineLoading: Boolean = false,
    val timelineErrorMessage: String? = null,
    val noteBody: String = "",
    val isAddingNote: Boolean = false,
    val addNoteErrorMessage: String? = null
)

package com.nexora.android.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.rpc.RpcRepository
import com.nexora.android.domain.session.CrmContact
import com.nexora.android.domain.session.NexoraResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val rpcRepository: RpcRepository,
    private val contactsMemoryCache: ContactsMemoryCache
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var searchRequestId = 0

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
        val memoryCache = if (hasScreenCache) null else contactsMemoryCache.contactsFor(tenantId)
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
                ContactsUiState(
                    tenantId = tenantId,
                    isInitialLoading = true
                )
            }
        }

        viewModelScope.launch {
            when (val result = rpcRepository.listCrmContacts(tenantId)) {
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
                    contactsMemoryCache.put(tenantId, result.value)
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

    fun refresh(tenantId: String) {
        load(tenantId)
        if (_uiState.value.isSearchActive) {
            scheduleSearch(tenantId = tenantId, immediate = true)
        }
    }

    fun onSearchQueryChanged(tenantId: String, query: String) {
        val safeQuery = query.take(MaxSearchQueryLength)
        _uiState.update { it.copy(searchQuery = safeQuery, searchErrorMessage = null) }
        scheduleSearch(tenantId)
    }

    fun onLifecycleStageSelected(tenantId: String, lifecycleStage: String?) {
        _uiState.update { it.copy(selectedLifecycleStage = lifecycleStage, searchErrorMessage = null) }
        scheduleSearch(tenantId)
    }

    fun onLeadStatusSelected(tenantId: String, leadStatus: String?) {
        _uiState.update { it.copy(selectedLeadStatus = leadStatus, searchErrorMessage = null) }
        scheduleSearch(tenantId)
    }

    fun onSortSelected(tenantId: String, sortOption: ContactSortOption) {
        _uiState.update { it.copy(selectedSort = sortOption, searchErrorMessage = null) }
        scheduleSearch(tenantId)
    }

    fun clearSearch(tenantId: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedLifecycleStage = null,
                selectedLeadStatus = null,
                selectedSort = ContactSortOption.Newest,
                searchContacts = emptyList(),
                isSearchLoading = false,
                searchErrorMessage = null
            )
        }
        if (!_uiState.value.hasLoaded) {
            load(tenantId)
        }
    }

    private fun scheduleSearch(
        tenantId: String,
        immediate: Boolean = false
    ) {
        searchJob?.cancel()

        val criteria = _uiState.value.searchCriteria()
        if (tenantId.isBlank()) {
            _uiState.update { it.copy(searchErrorMessage = "Missing tenant context.") }
            return
        }

        if (!criteria.isActive) {
            _uiState.update {
                it.copy(
                    searchContacts = emptyList(),
                    isSearchLoading = false,
                    searchErrorMessage = null
                )
            }
            return
        }

        val requestId = ++searchRequestId
        _uiState.update { it.copy(isSearchLoading = true, searchErrorMessage = null) }
        searchJob = viewModelScope.launch {
            if (!immediate) {
                delay(SearchDebounceMillis)
            }
            when (
                val result = rpcRepository.searchCrmContacts(
                    tenantId = tenantId,
                    query = criteria.query.takeIf { it.isNotBlank() },
                    lifecycleStage = criteria.lifecycleStage,
                    leadStatus = criteria.leadStatus,
                    sort = criteria.sort.rpcValue,
                    limit = SearchResultLimit
                )
            ) {
                is NexoraResult.Failure -> _uiState.update {
                    if (requestId != searchRequestId) {
                        it
                    } else {
                        it.copy(
                            isSearchLoading = false,
                            searchErrorMessage = result.error.message
                        )
                    }
                }
                is NexoraResult.Success -> _uiState.update {
                    if (requestId != searchRequestId) {
                        it
                    } else {
                        it.copy(
                            isSearchLoading = false,
                            searchContacts = result.value,
                            searchErrorMessage = null
                        )
                    }
                }
            }
        }
    }

    private fun ContactsUiState.searchCriteria(): ContactSearchCriteria {
        return ContactSearchCriteria(
            query = searchQuery.trim(),
            lifecycleStage = selectedLifecycleStage,
            leadStatus = selectedLeadStatus,
            sort = selectedSort
        )
    }

    companion object {
        const val MaxSearchQueryLength = 120
        const val SearchDebounceMillis = 350L
        const val SearchResultLimit = 100
    }
}

data class ContactsUiState(
    val tenantId: String? = null,
    val hasLoaded: Boolean = false,
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contacts: List<CrmContact> = emptyList(),
    val errorMessage: String? = null,
    val refreshErrorMessage: String? = null,
    val searchQuery: String = "",
    val selectedLifecycleStage: String? = null,
    val selectedLeadStatus: String? = null,
    val selectedSort: ContactSortOption = ContactSortOption.Newest,
    val searchContacts: List<CrmContact> = emptyList(),
    val isSearchLoading: Boolean = false,
    val searchErrorMessage: String? = null
) {
    val isSearchActive: Boolean
        get() = searchQuery.isNotBlank() ||
            selectedLifecycleStage != null ||
            selectedLeadStatus != null ||
            selectedSort != ContactSortOption.Newest

    val visibleContacts: List<CrmContact>
        get() = if (isSearchActive) {
            if (isSearchLoading && searchContacts.isEmpty()) contacts else searchContacts
        } else {
            contacts
        }
}

private data class ContactSearchCriteria(
    val query: String,
    val lifecycleStage: String?,
    val leadStatus: String?,
    val sort: ContactSortOption
) {
    val isActive: Boolean
        get() = query.isNotBlank() ||
            lifecycleStage != null ||
            leadStatus != null ||
            sort != ContactSortOption.Newest
}

enum class ContactSortOption(
    val label: String,
    val rpcValue: String
) {
    Newest("Newest", "newest"),
    Oldest("Oldest", "oldest"),
    NameAsc("Name A-Z", "name_asc")
}

data class ContactFilterOption(
    val label: String,
    val value: String?
)

val LifecycleStageFilterOptions = listOf(
    ContactFilterOption("All stages", null),
    ContactFilterOption("Lead", "lead"),
    ContactFilterOption("Subscriber", "subscriber"),
    ContactFilterOption("Customer", "customer"),
    ContactFilterOption("Evangelist", "evangelist"),
    ContactFilterOption("Other", "other")
)

val LeadStatusFilterOptions = listOf(
    ContactFilterOption("All statuses", null),
    ContactFilterOption("New", "new"),
    ContactFilterOption("Open", "open"),
    ContactFilterOption("In progress", "in_progress"),
    ContactFilterOption("Qualified", "qualified"),
    ContactFilterOption("Unqualified", "unqualified")
)

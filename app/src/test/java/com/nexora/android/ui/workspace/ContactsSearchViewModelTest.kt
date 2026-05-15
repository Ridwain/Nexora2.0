package com.nexora.android.ui.workspace

import com.nexora.android.domain.session.CrmContact
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.FakeRpcRepository
import com.nexora.android.ui.auth.testContact
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsSearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Test
    fun queryReducerCapsInputAtMaxLength() = runTest(testDispatcher) {
        val viewModel = ContactsViewModel(FakeRpcRepository(), ContactsMemoryCache())
        val longQuery = "a".repeat(ContactsViewModel.MaxSearchQueryLength + 40)

        viewModel.onSearchQueryChanged("tenant-id", longQuery)

        assertEquals(ContactsViewModel.MaxSearchQueryLength, viewModel.uiState.value.searchQuery.length)
    }

    @Test
    fun typingDoesNotCallSearchBeforeDebounce() = runTest(testDispatcher) {
        val rpcRepository = FakeRpcRepository()
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onSearchQueryChanged("tenant-id", "alex")
        advanceTimeBy(ContactsViewModel.SearchDebounceMillis - 1)

        assertEquals(0, rpcRepository.searchCrmContactsCalls)
        assertTrue(viewModel.uiState.value.isSearchLoading)
    }

    @Test
    fun debouncedQueryCallsBackendSearch() = runTest(testDispatcher) {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.searchCrmContactsResult = NexoraResult.Success(listOf(testContact()))
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onSearchQueryChanged("tenant-id", "contact@example.com")
        advanceTimeBy(ContactsViewModel.SearchDebounceMillis)
        advanceUntilIdle()

        assertEquals(1, rpcRepository.searchCrmContactsCalls)
        assertEquals("contact@example.com", rpcRepository.lastCrmContactSearchQuery)
        assertEquals("newest", rpcRepository.lastCrmContactSort)
        assertEquals(1, viewModel.uiState.value.searchContacts.size)
        assertFalse(viewModel.uiState.value.isSearchLoading)
    }

    @Test
    fun staleSearchResultCannotOverwriteNewerResult() = runTest(testDispatcher) {
        val oldResult = CompletableDeferred<NexoraResult<List<CrmContact>>>()
        val newResult = CompletableDeferred<NexoraResult<List<CrmContact>>>()
        val rpcRepository = FakeRpcRepository()
        rpcRepository.searchCrmContactsHandler = { _, query, _, _, _ ->
            if (query == "old") oldResult.await() else newResult.await()
        }
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onSearchQueryChanged("tenant-id", "old")
        advanceTimeBy(ContactsViewModel.SearchDebounceMillis)

        viewModel.onSearchQueryChanged("tenant-id", "new")
        advanceTimeBy(ContactsViewModel.SearchDebounceMillis)

        oldResult.complete(NexoraResult.Success(listOf(testContact().copy(id = "old-contact"))))
        newResult.complete(NexoraResult.Success(listOf(testContact().copy(id = "new-contact"))))
        advanceUntilIdle()

        assertEquals("new-contact", viewModel.uiState.value.searchContacts.first().id)
    }

    @Test
    fun filtersTriggerBackendSearch() = runTest(testDispatcher) {
        val rpcRepository = FakeRpcRepository()
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onLifecycleStageSelected("tenant-id", "customer")
        viewModel.onLeadStatusSelected("tenant-id", "qualified")
        viewModel.onSortSelected("tenant-id", ContactSortOption.NameAsc)
        advanceTimeBy(ContactsViewModel.SearchDebounceMillis)
        advanceUntilIdle()

        assertEquals(1, rpcRepository.searchCrmContactsCalls)
        assertEquals("customer", rpcRepository.lastCrmContactLifecycleStage)
        assertEquals("qualified", rpcRepository.lastCrmContactLeadStatus)
        assertEquals("name_asc", rpcRepository.lastCrmContactSort)
    }

    @Test
    fun blankQueryWithNoFiltersReturnsToCachedActiveList() = runTest(testDispatcher) {
        val cache = ContactsMemoryCache()
        cache.put("tenant-id", listOf(testContact()))
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listCrmContactsResult = NexoraResult.Success(listOf(testContact()))
        val viewModel = ContactsViewModel(rpcRepository, cache)

        viewModel.load("tenant-id")
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("tenant-id", "alex")
        advanceTimeBy(ContactsViewModel.SearchDebounceMillis)
        advanceUntilIdle()
        viewModel.clearSearch("tenant-id")

        assertFalse(viewModel.uiState.value.isSearchActive)
        assertEquals(1, viewModel.uiState.value.visibleContacts.size)
        assertNull(viewModel.uiState.value.searchErrorMessage)
    }

    @Test
    fun searchResultsDoNotOverwriteDashboardContactCache() = runTest(testDispatcher) {
        val originalContact = testContact().copy(id = "full-list-contact")
        val searchContact = testContact().copy(id = "search-contact")
        val cache = ContactsMemoryCache()
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listCrmContactsResult = NexoraResult.Success(listOf(originalContact))
        rpcRepository.searchCrmContactsResult = NexoraResult.Success(listOf(searchContact))
        val viewModel = ContactsViewModel(rpcRepository, cache)

        viewModel.load("tenant-id")
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("tenant-id", "search")
        advanceTimeBy(ContactsViewModel.SearchDebounceMillis)
        advanceUntilIdle()

        assertEquals("search-contact", viewModel.uiState.value.visibleContacts.first().id)
        assertEquals("full-list-contact", viewModel.uiState.value.contacts.first().id)
        assertEquals("full-list-contact", cache.contactsFor("tenant-id")?.first()?.id)
    }
}

package com.nexora.android.ui.workspace

import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.FakeRpcRepository
import com.nexora.android.ui.auth.testContact
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContactsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadSuccessShowsContacts() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listCrmContactsResult = NexoraResult.Success(listOf(testContact()))
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id")

        assertEquals(1, rpcRepository.listCrmContactsCalls)
        assertEquals("tenant-id", rpcRepository.lastCrmContactTenantId)
        assertEquals(true, viewModel.uiState.value.hasLoaded)
        assertEquals(false, viewModel.uiState.value.isInitialLoading)
        assertEquals(false, viewModel.uiState.value.isRefreshing)
        assertEquals(1, viewModel.uiState.value.contacts.size)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.refreshErrorMessage)
    }

    @Test
    fun firstLoadFailureShowsBlockingError() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listCrmContactsResult = NexoraResult.Failure(NexoraError.Unauthorized("No access"))
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id")

        assertEquals(1, rpcRepository.listCrmContactsCalls)
        assertEquals("No access", viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.refreshErrorMessage)
        assertFalse(viewModel.uiState.value.hasLoaded)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(0, viewModel.uiState.value.contacts.size)
    }

    @Test
    fun refreshAfterCachedDataKeepsContactsVisibleWhileRunning() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listCrmContactsResult = NexoraResult.Success(listOf(testContact()))
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id")

        val deferredResult = CompletableDeferred<NexoraResult<List<com.nexora.android.domain.session.CrmContact>>>()
        rpcRepository.listCrmContactsHandler = { deferredResult.await() }

        viewModel.load("tenant-id")

        assertEquals(2, rpcRepository.listCrmContactsCalls)
        assertTrue(viewModel.uiState.value.hasLoaded)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals(1, viewModel.uiState.value.contacts.size)
        assertEquals("contact-id", viewModel.uiState.value.contacts.first().id)
    }

    @Test
    fun refreshSuccessReplacesCachedContacts() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listCrmContactsResult = NexoraResult.Success(listOf(testContact()))
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id")

        val updatedContact = testContact().copy(id = "updated-contact-id", firstName = "Updated")
        rpcRepository.listCrmContactsResult = NexoraResult.Success(listOf(updatedContact))
        viewModel.load("tenant-id")

        assertEquals(2, rpcRepository.listCrmContactsCalls)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals("updated-contact-id", viewModel.uiState.value.contacts.first().id)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.refreshErrorMessage)
    }

    @Test
    fun refreshFailureKeepsCachedContactsAndShowsNonBlockingError() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listCrmContactsResult = NexoraResult.Success(listOf(testContact()))
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id")

        rpcRepository.listCrmContactsResult = NexoraResult.Failure(NexoraError.Network("Refresh failed"))
        viewModel.load("tenant-id")

        assertEquals(2, rpcRepository.listCrmContactsCalls)
        assertTrue(viewModel.uiState.value.hasLoaded)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(1, viewModel.uiState.value.contacts.size)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals("Refresh failed", viewModel.uiState.value.refreshErrorMessage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun duplicateLoadWhileRunningDoesNotCallNetworkAgain() = runTest {
        val rpcRepository = FakeRpcRepository()
        val deferredResult = CompletableDeferred<NexoraResult<List<com.nexora.android.domain.session.CrmContact>>>()
        rpcRepository.listCrmContactsHandler = { deferredResult.await() }
        val viewModel = ContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id")
        viewModel.load("tenant-id")

        assertEquals(1, rpcRepository.listCrmContactsCalls)
        assertTrue(viewModel.uiState.value.isInitialLoading)

        deferredResult.complete(NexoraResult.Success(listOf(testContact())))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertEquals(1, viewModel.uiState.value.contacts.size)
    }

    @Test
    fun newViewModelUsesMemoryCacheWithoutInitialLoading() = runTest {
        val cache = ContactsMemoryCache()
        cache.put("tenant-id", listOf(testContact()))
        val rpcRepository = FakeRpcRepository()
        val deferredResult = CompletableDeferred<NexoraResult<List<com.nexora.android.domain.session.CrmContact>>>()
        rpcRepository.listCrmContactsHandler = { deferredResult.await() }
        val viewModel = ContactsViewModel(rpcRepository, cache)

        viewModel.load("tenant-id")

        assertEquals(1, rpcRepository.listCrmContactsCalls)
        assertTrue(viewModel.uiState.value.hasLoaded)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals(1, viewModel.uiState.value.contacts.size)
        assertEquals("contact-id", viewModel.uiState.value.contacts.first().id)
    }
}

package com.nexora.android.ui.workspace

import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.FakeRpcRepository
import com.nexora.android.ui.auth.testContact
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContactDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadUsesCachedContactImmediatelyAndRefreshes() = runTest {
        val cache = ContactsMemoryCache()
        cache.put("tenant-id", listOf(testContact()))
        val refreshedContact = testContact().copy(firstName = "Updated")
        val rpcRepository = FakeRpcRepository()
        rpcRepository.getCrmContactResult = NexoraResult.Success(refreshedContact)
        val viewModel = ContactDetailViewModel(rpcRepository, cache)

        viewModel.load("tenant-id", "contact-id")

        assertEquals(1, rpcRepository.getCrmContactCalls)
        assertEquals("tenant-id", rpcRepository.lastCrmContactTenantId)
        assertEquals("contact-id", rpcRepository.lastCrmContactId)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertEquals("Updated", viewModel.uiState.value.contact?.firstName)
        assertEquals("Updated", cache.contactFor("tenant-id", "contact-id")?.firstName)
    }

    @Test
    fun loadFailureWithoutCacheShowsBlockingError() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.getCrmContactResult = NexoraResult.Failure(NexoraError.NotFound("Missing"))
        val viewModel = ContactDetailViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id", "contact-id")

        assertEquals("Missing", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.contact)
        assertFalse(viewModel.uiState.value.isInitialLoading)
    }

    @Test
    fun archiveRequiresDialogAndRemovesContactFromCacheOnSuccess() = runTest {
        val cache = ContactsMemoryCache()
        cache.put("tenant-id", listOf(testContact()))
        val rpcRepository = FakeRpcRepository()
        val viewModel = ContactDetailViewModel(rpcRepository, cache)

        viewModel.showArchiveDialog()
        assertTrue(viewModel.uiState.value.showArchiveDialog)

        viewModel.archive("tenant-id", "contact-id")

        assertEquals(1, rpcRepository.archiveCrmContactCalls)
        assertTrue(viewModel.uiState.value.archived)
        assertNull(cache.contactFor("tenant-id", "contact-id"))
    }
}

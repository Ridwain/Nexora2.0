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

class ArchivedContactsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadArchivedContactsSuccessStoresCache() = runTest {
        val archivedContact = testContact().copy(archivedAt = "2026-05-12T01:00:00Z")
        val cache = ContactsMemoryCache()
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listArchivedCrmContactsResult = NexoraResult.Success(listOf(archivedContact))
        val viewModel = ArchivedContactsViewModel(rpcRepository, cache)

        viewModel.load("tenant-id")

        assertEquals(1, rpcRepository.listArchivedCrmContactsCalls)
        assertTrue(viewModel.uiState.value.hasLoaded)
        assertEquals(listOf(archivedContact), viewModel.uiState.value.contacts)
        assertEquals(listOf(archivedContact), cache.archivedContactsFor("tenant-id"))
    }

    @Test
    fun firstLoadFailureShowsBlockingError() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listArchivedCrmContactsResult = NexoraResult.Failure(NexoraError.Unknown("Failed"))
        val viewModel = ArchivedContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id")

        assertEquals("Failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isInitialLoading)
    }

    @Test
    fun restoreRequiresDialogAndMovesContactToActiveCache() = runTest {
        val archivedContact = testContact().copy(archivedAt = "2026-05-12T01:00:00Z")
        val restoredContact = testContact().copy(archivedAt = null)
        val cache = ContactsMemoryCache()
        cache.putArchived("tenant-id", listOf(archivedContact))
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listArchivedCrmContactsResult = NexoraResult.Success(listOf(archivedContact))
        rpcRepository.restoreCrmContactResult = NexoraResult.Success(restoredContact)
        val viewModel = ArchivedContactsViewModel(rpcRepository, cache)

        viewModel.load("tenant-id")
        viewModel.showRestoreDialog(archivedContact)
        assertEquals(archivedContact, viewModel.uiState.value.contactPendingRestore)

        viewModel.restore("tenant-id", "contact-id")

        assertEquals(1, rpcRepository.restoreCrmContactCalls)
        assertNull(viewModel.uiState.value.contactPendingRestore)
        assertTrue(viewModel.uiState.value.contacts.isEmpty())
        assertNull(cache.archivedContactsFor("tenant-id")?.firstOrNull { it.id == "contact-id" })
        assertEquals(restoredContact, cache.contactFor("tenant-id", "contact-id"))
    }

    @Test
    fun restoreFailureKeepsContactVisible() = runTest {
        val archivedContact = testContact().copy(archivedAt = "2026-05-12T01:00:00Z")
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listArchivedCrmContactsResult = NexoraResult.Success(listOf(archivedContact))
        rpcRepository.restoreCrmContactResult = NexoraResult.Failure(NexoraError.Unknown("Restore failed"))
        val viewModel = ArchivedContactsViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id")
        viewModel.showRestoreDialog(archivedContact)
        viewModel.restore("tenant-id", "contact-id")

        assertEquals("Restore failed", viewModel.uiState.value.restoreErrorMessage)
        assertEquals(listOf(archivedContact), viewModel.uiState.value.contacts)
    }
}

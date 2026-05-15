package com.nexora.android.ui.workspace

import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.FakeRpcRepository
import com.nexora.android.ui.auth.testContact
import com.nexora.android.ui.auth.testTimelineNote
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

    @Test
    fun timelineLoadSuccessShowsItems() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listContactTimelineResult = NexoraResult.Success(listOf(testTimelineNote()))
        val viewModel = ContactDetailViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.loadTimeline("tenant-id", "contact-id")

        assertEquals(1, rpcRepository.listContactTimelineCalls)
        assertEquals(1, viewModel.uiState.value.timelineItems.size)
        assertNull(viewModel.uiState.value.timelineErrorMessage)
    }

    @Test
    fun timelineLoadFailureKeepsContactDetailVisible() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.listContactTimelineResult = NexoraResult.Failure(NexoraError.Network("Timeline failed"))
        val viewModel = ContactDetailViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.load("tenant-id", "contact-id")

        assertEquals(testContact().firstName, viewModel.uiState.value.contact?.firstName)
        assertEquals("Timeline failed", viewModel.uiState.value.timelineErrorMessage)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun noteBodyCapsAtMaxLength() = runTest {
        val viewModel = ContactDetailViewModel(FakeRpcRepository(), ContactsMemoryCache())
        val longNote = "a".repeat(ContactDetailViewModel.MaxNoteBodyLength + 30)

        viewModel.onNoteBodyChanged(longNote)

        assertEquals(ContactDetailViewModel.MaxNoteBodyLength, viewModel.uiState.value.noteBody.length)
    }

    @Test
    fun blankNoteBlocksNetworkCall() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = ContactDetailViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onNoteBodyChanged("   ")
        viewModel.addNote("tenant-id", "contact-id")

        assertEquals(0, rpcRepository.createContactNoteCalls)
        assertEquals("Write a note before adding it.", viewModel.uiState.value.addNoteErrorMessage)
    }

    @Test
    fun validNoteCreatesAndPrependsTimelineItem() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = ContactDetailViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onNoteBodyChanged(" Follow up tomorrow. ")
        viewModel.addNote("tenant-id", "contact-id")

        assertEquals(1, rpcRepository.createContactNoteCalls)
        assertEquals("Follow up tomorrow.", rpcRepository.lastContactNoteBody)
        assertEquals("", viewModel.uiState.value.noteBody)
        assertEquals(testTimelineNote(), viewModel.uiState.value.timelineItems.first())
    }

    @Test
    fun noteCreateFailureKeepsInputAndShowsError() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.createContactNoteResult = NexoraResult.Failure(NexoraError.Unknown("Create failed"))
        val viewModel = ContactDetailViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onNoteBodyChanged("Important note")
        viewModel.addNote("tenant-id", "contact-id")

        assertEquals("Important note", viewModel.uiState.value.noteBody)
        assertEquals("Create failed", viewModel.uiState.value.addNoteErrorMessage)
    }
}

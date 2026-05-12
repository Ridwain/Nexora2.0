package com.nexora.android.ui.workspace

import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.FakeRpcRepository
import com.nexora.android.ui.auth.testContact
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class EditContactViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadPrefillsFromCache() = runTest {
        val cache = ContactsMemoryCache()
        cache.put("tenant-id", listOf(testContact()))
        val viewModel = EditContactViewModel(FakeRpcRepository(), cache)

        viewModel.load("tenant-id", "contact-id")

        assertEquals("Nexora", viewModel.uiState.value.firstName)
        assertEquals("Contact", viewModel.uiState.value.lastName)
        assertEquals("contact@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun reducersCapLongInput() = runTest {
        val viewModel = EditContactViewModel(FakeRpcRepository(), ContactsMemoryCache())
        val longText = "x".repeat(10_000)

        viewModel.onFirstNameChanged(longText)
        viewModel.onLastNameChanged(longText)
        viewModel.onEmailChanged("${longText}@example.com")
        viewModel.onPhoneChanged(longText)
        viewModel.onCompanyNameChanged(longText)
        viewModel.onJobTitleChanged(longText)
        viewModel.onSourceChanged(longText)
        viewModel.onNotesChanged(longText)

        assertEquals(ContactInputRules.NAME_MAX_LENGTH, viewModel.uiState.value.firstName.length)
        assertEquals(ContactInputRules.NAME_MAX_LENGTH, viewModel.uiState.value.lastName.length)
        assertEquals(ContactInputRules.EMAIL_MAX_LENGTH, viewModel.uiState.value.email.length)
        assertEquals(ContactInputRules.PHONE_MAX_LENGTH, viewModel.uiState.value.phone.length)
        assertEquals(ContactInputRules.COMPANY_NAME_MAX_LENGTH, viewModel.uiState.value.companyName.length)
        assertEquals(ContactInputRules.JOB_TITLE_MAX_LENGTH, viewModel.uiState.value.jobTitle.length)
        assertEquals(ContactInputRules.SOURCE_MAX_LENGTH, viewModel.uiState.value.source.length)
        assertEquals(ContactInputRules.NOTES_MAX_LENGTH, viewModel.uiState.value.notes.length)
    }

    @Test
    fun blankFirstNameBlocksNetworkCall() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = EditContactViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.save("tenant-id", "contact-id")

        assertEquals(0, rpcRepository.updateCrmContactCalls)
        assertEquals("Enter a first name.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun invalidEmailBlocksNetworkCall() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = EditContactViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onFirstNameChanged("Nexora")
        viewModel.onEmailChanged("bad-email")
        viewModel.save("tenant-id", "contact-id")

        assertEquals(0, rpcRepository.updateCrmContactCalls)
        assertEquals("Enter a valid email.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun validEditCallsUpdateAndUpdatesCache() = runTest {
        val cache = ContactsMemoryCache()
        val updatedContact = testContact().copy(firstName = "Updated")
        val rpcRepository = FakeRpcRepository()
        rpcRepository.updateCrmContactResult = NexoraResult.Success(updatedContact)
        val viewModel = EditContactViewModel(rpcRepository, cache)

        viewModel.onFirstNameChanged("Updated")
        viewModel.onEmailChanged("updated@example.com")
        viewModel.save("tenant-id", "contact-id")

        assertEquals(1, rpcRepository.updateCrmContactCalls)
        assertEquals("tenant-id", rpcRepository.lastCrmContactTenantId)
        assertEquals("contact-id", rpcRepository.lastCrmContactId)
        assertEquals("Updated", rpcRepository.lastCrmContactFirstName)
        assertEquals("updated@example.com", rpcRepository.lastCrmContactEmail)
        assertEquals("Updated", cache.contactFor("tenant-id", "contact-id")?.firstName)
        assertEquals("Updated", viewModel.uiState.value.savedContact?.firstName)
    }

    @Test
    fun failureKeepsValuesAndShowsError() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.updateCrmContactResult = NexoraResult.Failure(NexoraError.Validation("Update failed"))
        val viewModel = EditContactViewModel(rpcRepository, ContactsMemoryCache())

        viewModel.onFirstNameChanged("Nexora")
        viewModel.save("tenant-id", "contact-id")

        assertEquals("Nexora", viewModel.uiState.value.firstName)
        assertEquals("Update failed", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.savedContact)
    }
}

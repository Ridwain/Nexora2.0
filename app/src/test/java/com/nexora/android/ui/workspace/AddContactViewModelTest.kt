package com.nexora.android.ui.workspace

import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.FakeRpcRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddContactViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun missingFirstNameBlocksNetworkCall() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = AddContactViewModel(rpcRepository)

        viewModel.submit("tenant-id")

        assertEquals(0, rpcRepository.createCrmContactCalls)
        assertEquals("Enter a first name.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun invalidEmailBlocksNetworkCall() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = AddContactViewModel(rpcRepository)

        viewModel.onFirstNameChanged("Nexora")
        viewModel.onEmailChanged("bad-email")
        viewModel.submit("tenant-id")

        assertEquals(0, rpcRepository.createCrmContactCalls)
        assertEquals("Enter a valid email.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun reducersCapLongInputAndDoNotCallNetwork() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = AddContactViewModel(rpcRepository)
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
        assertEquals(0, rpcRepository.createCrmContactCalls)
    }

    @Test
    fun validSubmitCreatesContact() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = AddContactViewModel(rpcRepository)

        viewModel.onFirstNameChanged("  Nexora  ")
        viewModel.onEmailChanged("contact@example.com")
        viewModel.submit("tenant-id")

        assertEquals(1, rpcRepository.createCrmContactCalls)
        assertEquals("tenant-id", rpcRepository.lastCrmContactTenantId)
        assertEquals("Nexora", rpcRepository.lastCrmContactFirstName)
        assertEquals("contact@example.com", rpcRepository.lastCrmContactEmail)
        assertEquals("contact-id", viewModel.uiState.value.createdContact?.id)
    }

    @Test
    fun failureKeepsValuesAndShowsError() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.createCrmContactResult = NexoraResult.Failure(NexoraError.Validation("Create failed"))
        val viewModel = AddContactViewModel(rpcRepository)

        viewModel.onFirstNameChanged("Nexora")
        viewModel.submit("tenant-id")

        assertEquals(1, rpcRepository.createCrmContactCalls)
        assertEquals("Nexora", viewModel.uiState.value.firstName)
        assertEquals("Create failed", viewModel.uiState.value.errorMessage)
    }
}

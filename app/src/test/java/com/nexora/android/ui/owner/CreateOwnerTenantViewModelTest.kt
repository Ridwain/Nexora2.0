package com.nexora.android.ui.owner

import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.FakeRpcRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class CreateOwnerTenantViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun validatesCompanyNameBeforeNetworkCall() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = CreateOwnerTenantViewModel(rpcRepository)

        viewModel.submit()

        assertEquals(0, rpcRepository.createOwnerTenantCalls)
        assertEquals("Enter a company name.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun reducersCapLongInputAndDoNotCallNetwork() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = CreateOwnerTenantViewModel(rpcRepository)
        val longText = "x".repeat(10_000)

        viewModel.onCompanyNameChanged(longText)
        viewModel.onIndustryChanged(longText)
        viewModel.onCountryChanged(longText)
        viewModel.onCompanyEmailChanged("${longText}@example.com")
        viewModel.onCompanyPhoneChanged(longText)

        assertEquals(OwnerInputRules.COMPANY_NAME_MAX_LENGTH, viewModel.uiState.value.companyName.length)
        assertEquals(OwnerInputRules.INDUSTRY_MAX_LENGTH, viewModel.uiState.value.industry.length)
        assertEquals(OwnerInputRules.COUNTRY_MAX_LENGTH, viewModel.uiState.value.country.length)
        assertEquals(OwnerInputRules.EMAIL_MAX_LENGTH, viewModel.uiState.value.companyEmail.length)
        assertEquals(OwnerInputRules.PHONE_MAX_LENGTH, viewModel.uiState.value.companyPhone.length)
        assertEquals(0, rpcRepository.createOwnerTenantCalls)
    }

    @Test
    fun validatesOptionalCompanyEmailBeforeNetworkCall() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = CreateOwnerTenantViewModel(rpcRepository)

        viewModel.onCompanyNameChanged("Nexora Demo")
        viewModel.onCompanyEmailChanged("bad-email")
        viewModel.submit()

        assertEquals(0, rpcRepository.createOwnerTenantCalls)
        assertEquals("Enter a valid company email.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun validSubmitCreatesOwnerTenantWithExpectedValues() = runTest {
        val rpcRepository = FakeRpcRepository()
        val viewModel = CreateOwnerTenantViewModel(rpcRepository)

        viewModel.onCompanyNameChanged("  Nexora Demo  ")
        viewModel.onIndustryChanged("Software")
        viewModel.onCountryChanged("Bangladesh")
        viewModel.onCompanyEmailChanged("owner@nexora.test")
        viewModel.onCompanyPhoneChanged("+8801000000000")
        viewModel.submit()

        assertEquals(1, rpcRepository.createOwnerTenantCalls)
        assertEquals("Nexora Demo", rpcRepository.lastOwnerTenantName)
        assertEquals("Software", rpcRepository.lastOwnerTenantIndustry)
        assertEquals("Bangladesh", rpcRepository.lastOwnerTenantCountry)
        assertEquals("owner@nexora.test", rpcRepository.lastOwnerTenantEmail)
        assertEquals("+8801000000000", rpcRepository.lastOwnerTenantPhone)
        assertNotNull(rpcRepository.lastOwnerTenantTimezone)
        assertEquals("owner:tenant-id", viewModel.uiState.value.createdContext?.contextId)
    }

    @Test
    fun failureKeepsFormValuesAndShowsError() = runTest {
        val rpcRepository = FakeRpcRepository()
        rpcRepository.createOwnerTenantResult = NexoraResult.Failure(NexoraError.Validation("Tenant failed"))
        val viewModel = CreateOwnerTenantViewModel(rpcRepository)

        viewModel.onCompanyNameChanged("Nexora Demo")
        viewModel.submit()

        assertEquals(1, rpcRepository.createOwnerTenantCalls)
        assertEquals("Nexora Demo", viewModel.uiState.value.companyName)
        assertEquals("Tenant failed", viewModel.uiState.value.errorMessage)
        assertEquals(null, viewModel.uiState.value.createdContext)
    }
}

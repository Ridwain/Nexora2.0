package com.nexora.android.ui.welcome

import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.FakeAuthRepository
import com.nexora.android.ui.auth.FakePendingSignupProfileStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WelcomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun observesPendingVerification() = runTest {
        val authRepository = FakeAuthRepository()
        val pendingStore = FakePendingSignupProfileStore()
        val viewModel = WelcomeViewModel(authRepository, pendingStore)

        pendingStore.save("test@example.com", "Nexora User", 123L)

        assertEquals("test@example.com", viewModel.uiState.value.pendingVerification?.email)
    }

    @Test
    fun resendUsesPendingEmail() = runTest {
        val authRepository = FakeAuthRepository()
        val pendingStore = FakePendingSignupProfileStore()
        pendingStore.save("test@example.com", "Nexora User", 123L)
        val viewModel = WelcomeViewModel(authRepository, pendingStore)

        viewModel.resendCode()

        assertEquals(1, authRepository.resendSignupOtpCalls)
        assertEquals("A new code has been sent.", viewModel.uiState.value.infoMessage)
    }

    @Test
    fun clearPendingVerificationClearsStore() = runTest {
        val authRepository = FakeAuthRepository()
        val pendingStore = FakePendingSignupProfileStore()
        pendingStore.save("test@example.com", "Nexora User", 123L)
        val viewModel = WelcomeViewModel(authRepository, pendingStore)

        viewModel.clearPendingVerification()

        assertEquals(1, pendingStore.clearAllCalls)
        assertEquals(null, viewModel.uiState.value.pendingVerification)
    }
}

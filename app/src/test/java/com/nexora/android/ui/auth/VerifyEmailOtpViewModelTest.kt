package com.nexora.android.ui.auth

import androidx.lifecycle.SavedStateHandle
import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VerifyEmailOtpViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun otpInputAcceptsDigitsOnlyAndCapsAtSix() = runTest {
        val viewModel = newViewModel()

        viewModel.onOtpChanged("12ab34567890")

        assertEquals("12345678", viewModel.uiState.value.otp)
        assertEquals(0, viewModel.authRepository.verifySignupOtpCalls)
    }

    @Test
    fun verifyRejectsNonSixDigitOtpBeforeNetworkCall() = runTest {
        val viewModel = newViewModel()

        viewModel.onOtpChanged("123")
        viewModel.verify()

        assertEquals(0, viewModel.authRepository.verifySignupOtpCalls)
        assertEquals("Enter the 8 digit code.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun verifySuccessClearsSessionHidesPendingVerificationAndKeepsDisplayNameForLogin() = runTest {
        val viewModel = newViewModel()
        viewModel.pendingStore.save("test@example.com", "Nexora User", 123L)

        viewModel.onOtpChanged("12345678")
        viewModel.verify()

        assertEquals(1, viewModel.authRepository.verifySignupOtpCalls)
        assertEquals(1, viewModel.sessionRepository.clearSessionCalls)
        assertEquals(1, viewModel.pendingStore.markVerifiedCalls)
        assertEquals(0, viewModel.pendingStore.clearCalls)
        assertEquals("Nexora User", viewModel.pendingStore.displayNameFor("test@example.com"))
        assertEquals(AuthNavigationTarget.Login, viewModel.uiState.value.navigationTarget)
    }

    @Test
    fun verifyFailureKeepsUserOnOtpScreen() = runTest {
        val viewModel = newViewModel()
        viewModel.authRepository.verifySignupOtpResult =
            NexoraResult.Failure(NexoraError.Validation("Invalid code"))

        viewModel.onOtpChanged("12345678")
        viewModel.verify()

        assertEquals(1, viewModel.authRepository.verifySignupOtpCalls)
        assertEquals(null, viewModel.uiState.value.navigationTarget)
        assertEquals("Invalid code", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun resendCallsRepositoryAndDoesNotNavigate() = runTest {
        val viewModel = newViewModel()

        viewModel.resend()

        assertEquals(1, viewModel.authRepository.resendSignupOtpCalls)
        assertEquals(null, viewModel.uiState.value.navigationTarget)
        assertEquals("A new code has been sent.", viewModel.uiState.value.infoMessage)
    }

    private fun newViewModel(): TestVerifyEmailOtpViewModel {
        val authRepository = FakeAuthRepository()
        val sessionRepository = FakeSessionRepository()
        val pendingStore = FakePendingSignupProfileStore()
        val viewModel = VerifyEmailOtpViewModel(
            savedStateHandle = SavedStateHandle(mapOf("email" to "test@example.com")),
            authRepository = authRepository,
            sessionRepository = sessionRepository,
            pendingVerificationStore = pendingStore
        )
        return TestVerifyEmailOtpViewModel(viewModel, authRepository, sessionRepository, pendingStore)
    }

    private class TestVerifyEmailOtpViewModel(
        private val viewModel: VerifyEmailOtpViewModel,
        val authRepository: FakeAuthRepository,
        val sessionRepository: FakeSessionRepository,
        val pendingStore: FakePendingSignupProfileStore
    ) {
        val uiState get() = viewModel.uiState
        fun onOtpChanged(value: String) = viewModel.onOtpChanged(value)
        fun verify() = viewModel.verify()
        fun resend() = viewModel.resend()
    }
}

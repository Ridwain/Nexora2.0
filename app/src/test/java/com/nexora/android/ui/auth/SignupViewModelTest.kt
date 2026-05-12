package com.nexora.android.ui.auth

import com.nexora.android.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SignupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun validatesSignupFieldsBeforeNetworkCall() = runTest {
        val authRepository = FakeAuthRepository()
        val sessionRepository = FakeSessionRepository()
        val pendingStore = FakePendingSignupProfileStore()
        val viewModel = SignupViewModel(authRepository, sessionRepository, pendingStore)

        viewModel.onDisplayNameChanged("")
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")
        viewModel.submit()

        assertEquals(0, authRepository.signUpCalls)
        assertEquals("Enter your name.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun reducersEnforceMaxLengthsForLongInput() = runTest {
        val authRepository = FakeAuthRepository()
        val sessionRepository = FakeSessionRepository()
        val pendingStore = FakePendingSignupProfileStore()
        val viewModel = SignupViewModel(authRepository, sessionRepository, pendingStore)
        val longText = "x".repeat(10_000)

        viewModel.onDisplayNameChanged(longText)
        viewModel.onEmailChanged("${longText}@example.com")
        viewModel.onPasswordChanged(longText)
        viewModel.onConfirmPasswordChanged(longText)

        assertEquals(AuthInputRules.DISPLAY_NAME_MAX_LENGTH, viewModel.uiState.value.displayName.length)
        assertEquals(AuthInputRules.EMAIL_MAX_LENGTH, viewModel.uiState.value.email.length)
        assertEquals(AuthInputRules.PASSWORD_MAX_LENGTH, viewModel.uiState.value.password.length)
        assertEquals(AuthInputRules.PASSWORD_MAX_LENGTH, viewModel.uiState.value.confirmPassword.length)
        assertEquals(0, authRepository.signUpCalls)
    }

    @Test
    fun signupSuccessStoresDisplayNameClearsSessionAndNavigatesToOtp() = runTest {
        val authRepository = FakeAuthRepository()
        val sessionRepository = FakeSessionRepository()
        val pendingStore = FakePendingSignupProfileStore()
        val viewModel = SignupViewModel(authRepository, sessionRepository, pendingStore)

        viewModel.onDisplayNameChanged("Nexora User")
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")
        viewModel.submit()

        assertEquals(1, authRepository.signUpCalls)
        assertEquals(1, pendingStore.saveCalls)
        assertEquals(1, sessionRepository.clearSessionCalls)
        assertEquals(AuthNavigationTarget.VerifyEmailOtp, viewModel.uiState.value.navigationTarget)
    }
}

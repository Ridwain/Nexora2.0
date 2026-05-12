package com.nexora.android.ui.auth

import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun rejectsInvalidInputBeforeNetworkCall() = runTest {
        val authRepository = FakeAuthRepository()
        val rpcRepository = FakeRpcRepository()
        val pendingStore = FakePendingSignupProfileStore()
        val viewModel = LoginViewModel(authRepository, rpcRepository, pendingStore)

        viewModel.onEmailChanged("bad-email")
        viewModel.onPasswordChanged("short")
        viewModel.submit()

        assertEquals(0, authRepository.signInCalls)
        assertEquals(0, rpcRepository.ensureUserProfileCalls)
        assertEquals("Enter a valid email address.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun repeatedInputUpdatesDoNotCallNetwork() = runTest {
        val authRepository = FakeAuthRepository()
        val rpcRepository = FakeRpcRepository()
        val pendingStore = FakePendingSignupProfileStore()
        val viewModel = LoginViewModel(authRepository, rpcRepository, pendingStore)

        repeat(500) {
            viewModel.onEmailChanged("very-long-email-$it@example.com")
            viewModel.onPasswordChanged("very-long-password-$it")
        }

        assertEquals(0, authRepository.signInCalls)
        assertEquals(0, rpcRepository.getUserContextsCalls)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun successSignsInEnsuresProfileLoadsContextsAndNavigates() = runTest {
        val authRepository = FakeAuthRepository()
        val rpcRepository = FakeRpcRepository()
        val pendingStore = FakePendingSignupProfileStore()
        rpcRepository.getUserContextsResult = com.nexora.android.domain.session.NexoraResult.Success(listOf(testContext()))
        val viewModel = LoginViewModel(authRepository, rpcRepository, pendingStore)

        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.submit()

        assertEquals(1, authRepository.signInCalls)
        assertEquals(1, rpcRepository.ensureUserProfileCalls)
        assertEquals(1, rpcRepository.getUserContextsCalls)
        assertEquals(AuthNavigationTarget.ContextPicker, viewModel.uiState.value.navigationTarget)
    }

    @Test
    fun loginUsesPendingDisplayNameAndClearsItAfterProfileSuccess() = runTest {
        val authRepository = FakeAuthRepository()
        val rpcRepository = FakeRpcRepository()
        val pendingStore = FakePendingSignupProfileStore()
        pendingStore.save("test@example.com", "Nexora User")
        val viewModel = LoginViewModel(authRepository, rpcRepository, pendingStore)

        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.submit()

        assertEquals("Nexora User", rpcRepository.lastEnsuredDisplayName)
        assertEquals(1, pendingStore.clearCalls)
    }

    @Test
    fun unverifiedLoginShowsVerifyActionAndDoesNotLoadProfile() = runTest {
        val authRepository = FakeAuthRepository()
        val rpcRepository = FakeRpcRepository()
        val pendingStore = FakePendingSignupProfileStore()
        authRepository.signInResult = NexoraResult.Failure(
            NexoraError.Validation("Email not confirmed", "email_not_confirmed")
        )
        val viewModel = LoginViewModel(authRepository, rpcRepository, pendingStore)

        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.submit()

        assertEquals(1, authRepository.signInCalls)
        assertEquals(0, rpcRepository.ensureUserProfileCalls)
        assertEquals(1, pendingStore.saveCalls)
        assertEquals(true, viewModel.uiState.value.showVerifyEmailAction)
        assertEquals(
            "Email is not verified yet. Enter the email code to continue.",
            viewModel.uiState.value.errorMessage
        )
    }
}

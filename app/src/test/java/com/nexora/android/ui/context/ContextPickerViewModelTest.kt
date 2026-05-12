package com.nexora.android.ui.context

import com.nexora.android.core.deeplink.DeepLinkRepository
import com.nexora.android.domain.session.PendingInvite
import com.nexora.android.domain.session.PendingInviteType
import com.nexora.android.testing.MainDispatcherRule
import com.nexora.android.ui.auth.AuthNavigationTarget
import com.nexora.android.ui.auth.FakeAuthRepository
import com.nexora.android.ui.auth.FakeRpcRepository
import com.nexora.android.ui.auth.testContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContextPickerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsContextsAndPendingInviteOnStart() = runTest {
        val rpcRepository = FakeRpcRepository().apply {
            getUserContextsResult = com.nexora.android.domain.session.NexoraResult.Success(listOf(testContext()))
        }
        val deepLinkRepository = FakeDeepLinkRepository(
            PendingInvite(type = PendingInviteType.Employee, token = "token")
        )

        val viewModel = ContextPickerViewModel(
            rpcRepository = rpcRepository,
            authRepository = FakeAuthRepository(),
            deepLinkRepository = deepLinkRepository
        )

        assertEquals(1, rpcRepository.getUserContextsCalls)
        assertEquals(1, viewModel.uiState.value.contexts.size)
        assertEquals(PendingInviteType.Employee, viewModel.uiState.value.pendingInvite?.type)
    }

    @Test
    fun logoutSignsOutAndNavigatesToWelcome() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = ContextPickerViewModel(
            rpcRepository = FakeRpcRepository(),
            authRepository = authRepository,
            deepLinkRepository = FakeDeepLinkRepository()
        )

        viewModel.logout()

        assertEquals(1, authRepository.signOutCalls)
        assertEquals(AuthNavigationTarget.Welcome, viewModel.uiState.value.navigationTarget)
    }

    private class FakeDeepLinkRepository(
        initialInvite: PendingInvite? = null
    ) : DeepLinkRepository {
        private val state = MutableStateFlow(initialInvite)
        override val pendingInvite: Flow<PendingInvite?> = state

        override suspend fun captureInviteUri(rawUri: String?): PendingInvite? = state.value

        override suspend fun currentInvite(): PendingInvite? = state.value

        override suspend fun clearInvite() {
            state.value = null
        }
    }
}

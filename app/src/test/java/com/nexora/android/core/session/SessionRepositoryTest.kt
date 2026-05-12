package com.nexora.android.core.session

import com.nexora.android.domain.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionRepositoryTest {
    @Test
    fun savesReadsAndClearsSession() = runBlocking {
        val store = FakeSessionStore()
        val repository = DefaultSessionRepository(store)
        val session = UserSession(
            accessToken = "access",
            refreshToken = "refresh",
            tokenType = "bearer",
            expiresAtEpochSeconds = 1234L,
            user = null
        )

        repository.saveSession(session)

        assertEquals(session, repository.currentSessionNow())
        assertEquals(session, repository.currentSession.first())
        assertEquals("Bearer access", repository.authorizationHeader())

        repository.clearSession()

        assertNull(repository.currentSessionNow())
    }

    private class FakeSessionStore : SessionStore {
        private val state = MutableStateFlow<UserSession?>(null)
        override val session = state

        override suspend fun currentSession(): UserSession? = state.value

        override suspend fun saveSession(session: UserSession) {
            state.value = session
        }

        override suspend fun clearSession() {
            state.value = null
        }
    }
}

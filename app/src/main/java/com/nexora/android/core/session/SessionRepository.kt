package com.nexora.android.core.session

import com.nexora.android.domain.session.UserSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SessionRepository {
    val currentSession: Flow<UserSession?>

    suspend fun currentSessionNow(): UserSession?
    suspend fun saveSession(session: UserSession)
    suspend fun clearSession()
    suspend fun authorizationHeader(): String?
}

@Singleton
class DefaultSessionRepository @Inject constructor(
    private val sessionStore: SessionStore
) : SessionRepository {
    override val currentSession: Flow<UserSession?> = sessionStore.session

    override suspend fun currentSessionNow(): UserSession? = sessionStore.currentSession()

    override suspend fun saveSession(session: UserSession) {
        sessionStore.saveSession(session)
    }

    override suspend fun clearSession() {
        sessionStore.clearSession()
    }

    override suspend fun authorizationHeader(): String? = currentSessionNow()?.authorizationHeader
}

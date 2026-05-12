package com.nexora.android.core.session

import com.nexora.android.domain.session.UserSession
import kotlinx.coroutines.flow.Flow

interface SessionStore {
    val session: Flow<UserSession?>

    suspend fun currentSession(): UserSession?
    suspend fun saveSession(session: UserSession)
    suspend fun clearSession()
}

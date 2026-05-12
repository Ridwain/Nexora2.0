package com.nexora.android.data.auth

import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.domain.session.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentSession: Flow<UserSession?>

    suspend fun signUp(email: String, password: String): NexoraResult<UserSession?>
    suspend fun verifySignupOtp(email: String, token: String): NexoraResult<UserSession?>
    suspend fun resendSignupOtp(email: String): NexoraResult<Unit>
    suspend fun signIn(email: String, password: String): NexoraResult<UserSession>
    suspend fun refreshSession(refreshToken: String): NexoraResult<UserSession>
    suspend fun signOut(): NexoraResult<Unit>
}

package com.nexora.android.data.auth

import kotlinx.coroutines.flow.Flow

data class PendingSignupVerification(
    val email: String,
    val displayName: String,
    val createdAtEpochSeconds: Long
)

interface PendingSignupVerificationStore {
    fun pendingVerification(): Flow<PendingSignupVerification?>
    suspend fun pendingFor(email: String): PendingSignupVerification?
    suspend fun save(email: String, displayName: String, createdAtEpochSeconds: Long)
    suspend fun markVerified(email: String)
    suspend fun displayNameFor(email: String): String?
    suspend fun clear(email: String)
    suspend fun clearAll()
}

package com.nexora.android.data.auth

interface PendingSignupProfileStore {
    suspend fun save(email: String, displayName: String)
    suspend fun displayNameFor(email: String): String?
    suspend fun clear(email: String)
}

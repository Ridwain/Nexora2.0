package com.nexora.android.data.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pendingSignupProfileDataStore by preferencesDataStore(name = "nexora_pending_signup_profile")

@Singleton
class DataStorePendingSignupProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) : PendingSignupProfileStore, PendingSignupVerificationStore {
    override suspend fun save(email: String, displayName: String) {
        save(email = email, displayName = displayName, createdAtEpochSeconds = 0L)
    }

    override suspend fun save(email: String, displayName: String, createdAtEpochSeconds: Long) {
        context.pendingSignupProfileDataStore.edit { preferences ->
            preferences[Keys.Email] = email.normalizedEmail()
            preferences[Keys.DisplayName] = displayName.trim()
            preferences[Keys.CreatedAtEpochSeconds] = createdAtEpochSeconds
            preferences[Keys.IsVerificationPending] = true
        }
    }

    override fun pendingVerification(): Flow<PendingSignupVerification?> {
        return context.pendingSignupProfileDataStore.data.map { preferences ->
            val email = preferences[Keys.Email]?.takeIf { it.isNotBlank() } ?: return@map null
            val isVerificationPending = preferences[Keys.IsVerificationPending] ?: true
            if (!isVerificationPending) return@map null
            PendingSignupVerification(
                email = email,
                displayName = preferences[Keys.DisplayName].orEmpty(),
                createdAtEpochSeconds = preferences[Keys.CreatedAtEpochSeconds] ?: 0L
            )
        }
    }

    override suspend fun pendingFor(email: String): PendingSignupVerification? {
        val normalizedEmail = email.normalizedEmail()
        return pendingVerification().map { pending ->
            pending?.takeIf { it.email == normalizedEmail }
        }.firstOrNull()
    }

    override suspend fun markVerified(email: String) {
        val normalizedEmail = email.normalizedEmail()
        context.pendingSignupProfileDataStore.edit { preferences ->
            if (preferences[Keys.Email] == normalizedEmail) {
                preferences[Keys.IsVerificationPending] = false
            }
        }
    }

    override suspend fun displayNameFor(email: String): String? {
        val normalizedEmail = email.normalizedEmail()
        return context.pendingSignupProfileDataStore.data.map { preferences ->
            val storedEmail = preferences[Keys.Email]
            val displayName = preferences[Keys.DisplayName]
            if (storedEmail == normalizedEmail && !displayName.isNullOrBlank()) {
                displayName
            } else {
                null
            }
        }.firstOrNull()
    }

    override suspend fun clear(email: String) {
        val normalizedEmail = email.normalizedEmail()
        context.pendingSignupProfileDataStore.edit { preferences ->
            if (preferences[Keys.Email] == normalizedEmail) {
                preferences.remove(Keys.Email)
                preferences.remove(Keys.DisplayName)
                preferences.remove(Keys.CreatedAtEpochSeconds)
                preferences.remove(Keys.IsVerificationPending)
            }
        }
    }

    override suspend fun clearAll() {
        context.pendingSignupProfileDataStore.edit { preferences ->
            preferences.remove(Keys.Email)
            preferences.remove(Keys.DisplayName)
            preferences.remove(Keys.CreatedAtEpochSeconds)
            preferences.remove(Keys.IsVerificationPending)
        }
    }

    private fun String.normalizedEmail(): String = trim().lowercase()

    private object Keys {
        val Email = stringPreferencesKey("email")
        val DisplayName = stringPreferencesKey("display_name")
        val CreatedAtEpochSeconds = longPreferencesKey("created_at_epoch_seconds")
        val IsVerificationPending = booleanPreferencesKey("is_verification_pending")
    }
}

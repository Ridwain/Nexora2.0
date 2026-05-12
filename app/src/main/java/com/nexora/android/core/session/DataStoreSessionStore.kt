package com.nexora.android.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.nexora.android.domain.session.AuthUser
import com.nexora.android.domain.session.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.nexoraSessionDataStore by preferencesDataStore(name = "nexora_session")

@Singleton
class DataStoreSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : SessionStore {
    override val session: Flow<UserSession?> = context.nexoraSessionDataStore.data.map { preferences ->
        val accessToken = preferences[Keys.AccessToken].orEmpty()
        val refreshToken = preferences[Keys.RefreshToken].orEmpty()
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            null
        } else {
            UserSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = preferences[Keys.TokenType] ?: "bearer",
                expiresAtEpochSeconds = preferences[Keys.ExpiresAt],
                user = preferences[Keys.UserJson]?.toAuthUserOrNull()
            )
        }
    }

    override suspend fun currentSession(): UserSession? = session.firstOrNull()

    override suspend fun saveSession(session: UserSession) {
        context.nexoraSessionDataStore.edit { preferences ->
            preferences[Keys.AccessToken] = session.accessToken
            preferences[Keys.RefreshToken] = session.refreshToken
            preferences[Keys.TokenType] = session.tokenType
            session.expiresAtEpochSeconds?.let { preferences[Keys.ExpiresAt] = it }
                ?: preferences.remove(Keys.ExpiresAt)
            session.user?.let { preferences[Keys.UserJson] = gson.toJson(it) }
                ?: preferences.remove(Keys.UserJson)
        }
    }

    override suspend fun clearSession() {
        context.nexoraSessionDataStore.edit { preferences ->
            preferences.remove(Keys.AccessToken)
            preferences.remove(Keys.RefreshToken)
            preferences.remove(Keys.TokenType)
            preferences.remove(Keys.ExpiresAt)
            preferences.remove(Keys.UserJson)
        }
    }

    private fun String.toAuthUserOrNull(): AuthUser? {
        return try {
            gson.fromJson(this, AuthUser::class.java)
        } catch (_: JsonSyntaxException) {
            null
        }
    }

    private object Keys {
        val AccessToken = stringPreferencesKey("access_token")
        val RefreshToken = stringPreferencesKey("refresh_token")
        val TokenType = stringPreferencesKey("token_type")
        val ExpiresAt = longPreferencesKey("expires_at_epoch_seconds")
        val UserJson = stringPreferencesKey("user_json")
    }
}

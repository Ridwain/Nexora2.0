package com.nexora.android.core.deeplink

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexora.android.domain.session.PendingInvite
import com.nexora.android.domain.session.PendingInviteType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.nexoraInviteDataStore by preferencesDataStore(name = "nexora_pending_invite")

@Singleton
class DataStorePendingInviteStore @Inject constructor(
    @ApplicationContext private val context: Context
) : PendingInviteStore {
    override val pendingInvite: Flow<PendingInvite?> = context.nexoraInviteDataStore.data.map { preferences ->
        val type = preferences[Keys.Type]?.toPendingInviteTypeOrNull()
        val token = preferences[Keys.Token]

        if (type == null || token.isNullOrBlank()) {
            null
        } else {
            PendingInvite(type = type, token = token)
        }
    }

    override suspend fun currentInvite(): PendingInvite? = pendingInvite.firstOrNull()

    override suspend fun saveInvite(invite: PendingInvite) {
        context.nexoraInviteDataStore.edit { preferences ->
            preferences[Keys.Type] = invite.type.name
            preferences[Keys.Token] = invite.token
        }
    }

    override suspend fun clearInvite() {
        context.nexoraInviteDataStore.edit { preferences ->
            preferences.remove(Keys.Type)
            preferences.remove(Keys.Token)
        }
    }

    private fun String.toPendingInviteTypeOrNull(): PendingInviteType? =
        PendingInviteType.entries.firstOrNull { it.name == this }

    private object Keys {
        val Type = stringPreferencesKey("type")
        val Token = stringPreferencesKey("token")
    }
}

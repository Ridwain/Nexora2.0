package com.nexora.android.core.deeplink

import com.nexora.android.domain.session.PendingInvite
import kotlinx.coroutines.flow.Flow

interface PendingInviteStore {
    val pendingInvite: Flow<PendingInvite?>

    suspend fun currentInvite(): PendingInvite?
    suspend fun saveInvite(invite: PendingInvite)
    suspend fun clearInvite()
}

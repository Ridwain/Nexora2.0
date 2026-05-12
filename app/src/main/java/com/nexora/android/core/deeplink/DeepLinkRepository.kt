package com.nexora.android.core.deeplink

import com.nexora.android.domain.session.PendingInvite
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface DeepLinkRepository {
    val pendingInvite: Flow<PendingInvite?>

    suspend fun captureInviteUri(rawUri: String?): PendingInvite?
    suspend fun currentInvite(): PendingInvite?
    suspend fun clearInvite()
}

@Singleton
class DefaultDeepLinkRepository @Inject constructor(
    private val parser: InviteDeepLinkParser,
    private val pendingInviteStore: PendingInviteStore
) : DeepLinkRepository {
    override val pendingInvite: Flow<PendingInvite?> = pendingInviteStore.pendingInvite

    override suspend fun captureInviteUri(rawUri: String?): PendingInvite? {
        val invite = parser.parse(rawUri) ?: return null
        pendingInviteStore.saveInvite(invite)
        return invite
    }

    override suspend fun currentInvite(): PendingInvite? = pendingInviteStore.currentInvite()

    override suspend fun clearInvite() {
        pendingInviteStore.clearInvite()
    }
}

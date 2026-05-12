package com.nexora.android.core.deeplink

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeepLinkModule {
    @Binds
    @Singleton
    abstract fun bindPendingInviteStore(impl: DataStorePendingInviteStore): PendingInviteStore

    @Binds
    @Singleton
    abstract fun bindDeepLinkRepository(impl: DefaultDeepLinkRepository): DeepLinkRepository
}

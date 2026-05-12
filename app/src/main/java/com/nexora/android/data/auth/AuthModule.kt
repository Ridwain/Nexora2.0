package com.nexora.android.data.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPendingSignupProfileStore(
        impl: DataStorePendingSignupProfileStore
    ): PendingSignupProfileStore

    @Binds
    @Singleton
    abstract fun bindPendingSignupVerificationStore(
        impl: DataStorePendingSignupProfileStore
    ): PendingSignupVerificationStore
}

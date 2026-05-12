package com.nexora.android.data.rpc

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RpcModule {
    @Binds
    @Singleton
    abstract fun bindRpcRepository(impl: SupabaseRpcRepository): RpcRepository
}

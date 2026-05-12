package com.nexora.android.core.network

import com.nexora.android.core.config.AppConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseHeadersInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("apikey", AppConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Accept", "application/json")
            .build()

        return chain.proceed(request)
    }
}

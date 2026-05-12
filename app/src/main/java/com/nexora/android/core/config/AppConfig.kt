package com.nexora.android.core.config

import com.nexora.android.BuildConfig

object AppConfig {
    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    val SUPABASE_PUBLISHABLE_KEY: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    val SUPABASE_AUTH_BASE_URL: String = "${SUPABASE_URL.trimEnd('/')}/auth/v1/"
    val SUPABASE_REST_BASE_URL: String = "${SUPABASE_URL.trimEnd('/')}/rest/v1/"
}

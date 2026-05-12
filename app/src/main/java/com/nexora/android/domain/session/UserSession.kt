package com.nexora.android.domain.session

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresAtEpochSeconds: Long?,
    val user: AuthUser?
) {
    val authorizationHeader: String = "Bearer $accessToken"
}

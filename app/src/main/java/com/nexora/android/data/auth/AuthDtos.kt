package com.nexora.android.data.auth

import com.google.gson.annotations.SerializedName

data class SignUpRequest(
    val email: String,
    val password: String
)

data class PasswordTokenRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class VerifyOtpRequest(
    val type: String,
    val email: String,
    val token: String
)

data class ResendOtpRequest(
    val type: String,
    val email: String
)

data class AuthResponseDto(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Long? = null,
    @SerializedName("expires_at") val expiresAt: Long? = null,
    val user: AuthUserDto? = null,
    val id: String? = null,
    val email: String? = null
)

data class AuthUserDto(
    val id: String? = null,
    val email: String? = null
)

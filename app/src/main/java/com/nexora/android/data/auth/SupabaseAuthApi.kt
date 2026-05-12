package com.nexora.android.data.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {
    @POST("signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): AuthResponseDto

    @POST("token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String,
        @Body request: PasswordTokenRequest
    ): AuthResponseDto

    @POST("token")
    suspend fun refreshSession(
        @Query("grant_type") grantType: String,
        @Body request: RefreshTokenRequest
    ): AuthResponseDto

    @POST("verify")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): AuthResponseDto

    @POST("resend")
    suspend fun resendOtp(
        @Body request: ResendOtpRequest
    ): Response<Unit>

    @POST("logout")
    suspend fun signOut(
        @Header("Authorization") authorization: String
    ): Response<Unit>
}

package com.nexora.android.data.auth

import com.nexora.android.core.network.ApiErrorMapper
import com.nexora.android.core.deeplink.DeepLinkRepository
import com.nexora.android.core.network.safeApiCall
import com.nexora.android.core.session.SessionRepository
import com.nexora.android.domain.session.AuthUser
import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.domain.session.UserSession
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val authApi: SupabaseAuthApi,
    private val sessionRepository: SessionRepository,
    private val deepLinkRepository: DeepLinkRepository,
    private val errorMapper: ApiErrorMapper
) : AuthRepository {
    override val currentSession: Flow<UserSession?> = sessionRepository.currentSession

    override suspend fun signUp(email: String, password: String): NexoraResult<UserSession?> {
        return when (val result = safeApiCall(errorMapper) {
            authApi.signUp(SignUpRequest(email = email.trim(), password = password)).toSessionOrNull()
        }) {
            is NexoraResult.Success -> {
                result.value?.let { sessionRepository.saveSession(it) }
                result
            }
            is NexoraResult.Failure -> result
        }
    }

    override suspend fun verifySignupOtp(email: String, token: String): NexoraResult<UserSession?> {
        val signupResult = verifyOtp(email = email, token = token, type = "signup")
        if (signupResult !is NexoraResult.Failure || !signupResult.error.isInvalidOtpTypeError()) {
            return signupResult
        }

        return verifyOtp(email = email, token = token, type = "email")
    }

    override suspend fun resendSignupOtp(email: String): NexoraResult<Unit> {
        return safeApiCall(errorMapper) {
            val response = authApi.resendOtp(ResendOtpRequest(type = "signup", email = email.trim()))
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
            Unit
        }
    }

    override suspend fun signIn(email: String, password: String): NexoraResult<UserSession> {
        return when (val result = safeApiCall(errorMapper) {
            authApi.signInWithPassword(
                grantType = "password",
                request = PasswordTokenRequest(email = email.trim(), password = password)
            ).toSessionOrThrow()
        }) {
            is NexoraResult.Success -> {
                sessionRepository.saveSession(result.value)
                result
            }
            is NexoraResult.Failure -> result
        }
    }

    private suspend fun verifyOtp(email: String, token: String, type: String): NexoraResult<UserSession?> {
        return safeApiCall(errorMapper) {
            authApi.verifyOtp(
                VerifyOtpRequest(
                    type = type,
                    email = email.trim(),
                    token = token
                )
            ).toSessionOrNull()
        }
    }

    private fun NexoraError.isInvalidOtpTypeError(): Boolean {
        val text = listOfNotNull(message, code).joinToString(" ").lowercase()
        return "type" in text && ("invalid" in text || "unsupported" in text || "not supported" in text)
    }

    override suspend fun refreshSession(refreshToken: String): NexoraResult<UserSession> {
        return when (val result = safeApiCall(errorMapper) {
            authApi.refreshSession(
                grantType = "refresh_token",
                request = RefreshTokenRequest(refreshToken = refreshToken)
            ).toSessionOrThrow()
        }) {
            is NexoraResult.Success -> {
                sessionRepository.saveSession(result.value)
                result
            }
            is NexoraResult.Failure -> result
        }
    }

    override suspend fun signOut(): NexoraResult<Unit> {
        val authorization = sessionRepository.authorizationHeader()
        if (authorization.isNullOrBlank()) {
            sessionRepository.clearSession()
            deepLinkRepository.clearInvite()
            return NexoraResult.Success(Unit)
        }

        return when (val result = safeApiCall(errorMapper) {
            authApi.signOut(authorization)
            Unit
        }) {
            is NexoraResult.Success -> {
                sessionRepository.clearSession()
                deepLinkRepository.clearInvite()
                result
            }
            is NexoraResult.Failure -> {
                sessionRepository.clearSession()
                deepLinkRepository.clearInvite()
                result
            }
        }
    }

    private fun AuthResponseDto.toSessionOrThrow(): UserSession =
        toSessionOrNull() ?: throw IllegalStateException("Supabase auth response did not include a session")

    private fun AuthResponseDto.toSessionOrNull(): UserSession? {
        val accessToken = accessToken?.takeIf { it.isNotBlank() } ?: return null
        val refreshToken = refreshToken?.takeIf { it.isNotBlank() } ?: return null
        return UserSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType ?: "bearer",
            expiresAtEpochSeconds = expiresAt ?: expiresIn?.let { Instant.now().epochSecond + it },
            user = user?.toDomain() ?: topLevelUserOrNull()
        )
    }

    private fun AuthUserDto.toDomain(): AuthUser? {
        val userId = id?.takeIf { it.isNotBlank() } ?: return null
        return AuthUser(id = userId, email = email)
    }

    private fun AuthResponseDto.topLevelUserOrNull(): AuthUser? {
        val userId = id?.takeIf { it.isNotBlank() } ?: return null
        return AuthUser(id = userId, email = email)
    }
}

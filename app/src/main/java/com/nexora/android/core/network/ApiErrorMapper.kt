package com.nexora.android.core.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.nexora.android.domain.session.NexoraError
import java.io.IOException
import retrofit2.HttpException

class ApiErrorMapper(
    private val gson: Gson
) {
    fun map(throwable: Throwable): NexoraError {
        return when (throwable) {
            is HttpException -> mapHttpException(throwable)
            is IOException -> NexoraError.Network("Network connection failed")
            is JsonSyntaxException -> NexoraError.Unknown("Unexpected response from server")
            else -> NexoraError.Unknown(throwable.message ?: "Unexpected error")
        }
    }

    private fun mapHttpException(exception: HttpException): NexoraError {
        val statusCode = exception.code()
        val responseError = exception.response()?.errorBody()?.string()?.let(::parseSupabaseError)
        val message = responseError?.bestMessage ?: exception.message()
        val code = responseError?.code ?: statusCode.toString()

        return when (statusCode) {
            401, 403 -> NexoraError.Unauthorized(message, code)
            404 -> NexoraError.NotFound(message, code)
            400, 409, 422 -> NexoraError.Validation(message, code)
            else -> NexoraError.Unknown(message, code)
        }
    }

    private fun parseSupabaseError(raw: String): SupabaseErrorDto? {
        if (raw.isBlank()) return null
        return runCatching { gson.fromJson(raw, SupabaseErrorDto::class.java) }.getOrNull()
    }
}

data class SupabaseErrorDto(
    val code: String? = null,
    val message: String? = null,
    val msg: String? = null,
    val details: String? = null,
    val hint: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("error_description") val errorDescription: String? = null
) {
    val bestMessage: String
        get() = listOf(message, msg, errorDescription, error, details, hint)
            .firstOrNull { !it.isNullOrBlank() }
            ?: "Supabase request failed"
}

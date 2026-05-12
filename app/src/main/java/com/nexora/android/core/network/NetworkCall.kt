package com.nexora.android.core.network

import com.nexora.android.domain.session.NexoraResult

suspend inline fun <T> safeApiCall(
    errorMapper: ApiErrorMapper,
    crossinline block: suspend () -> T
): NexoraResult<T> {
    return try {
        NexoraResult.Success(block())
    } catch (throwable: Throwable) {
        NexoraResult.Failure(errorMapper.map(throwable))
    }
}

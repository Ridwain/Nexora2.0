package com.nexora.android.domain.session

sealed class NexoraError(
    open val message: String,
    open val code: String? = null
) {
    data class Network(override val message: String) : NexoraError(message)
    data class Unauthorized(override val message: String, override val code: String? = null) : NexoraError(message, code)
    data class Validation(override val message: String, override val code: String? = null) : NexoraError(message, code)
    data class NotFound(override val message: String, override val code: String? = null) : NexoraError(message, code)
    data class Unknown(override val message: String, override val code: String? = null) : NexoraError(message, code)
}

sealed interface NexoraResult<out T> {
    data class Success<T>(val value: T) : NexoraResult<T>
    data class Failure(val error: NexoraError) : NexoraResult<Nothing>
}

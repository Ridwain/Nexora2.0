package com.nexora.android.ui.auth

object AuthInputRules {
    const val DISPLAY_NAME_MAX_LENGTH = 80
    const val EMAIL_MAX_LENGTH = 254
    const val PASSWORD_MAX_LENGTH = 128
    const val PASSWORD_MIN_LENGTH = 8
    const val OTP_LENGTH = 8

    fun displayName(value: String): String = value.take(DISPLAY_NAME_MAX_LENGTH)
    fun email(value: String): String = value.trim().take(EMAIL_MAX_LENGTH)
    fun password(value: String): String = value.take(PASSWORD_MAX_LENGTH)
    fun otp(value: String): String = value.filter(Char::isDigit).take(OTP_LENGTH)
}

object AuthFormValidator {
    fun validateLogin(email: String, password: String): String? {
        return when {
            !isValidEmail(email) -> "Enter a valid email address."
            password.length < AuthInputRules.PASSWORD_MIN_LENGTH -> "Password must be at least 8 characters."
            else -> null
        }
    }

    fun validateSignup(
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        return when {
            displayName.isBlank() -> "Enter your name."
            !isValidEmail(email) -> "Enter a valid email address."
            password.length < AuthInputRules.PASSWORD_MIN_LENGTH -> "Password must be at least 8 characters."
            password != confirmPassword -> "Passwords do not match."
            else -> null
        }
    }

    fun validateOtp(token: String): String? {
        return when {
            token.length != AuthInputRules.OTP_LENGTH -> "Enter the 8 digit code."
            !token.all(Char::isDigit) -> "The code can contain digits only."
            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && email.contains("@") && email.length <= AuthInputRules.EMAIL_MAX_LENGTH
}

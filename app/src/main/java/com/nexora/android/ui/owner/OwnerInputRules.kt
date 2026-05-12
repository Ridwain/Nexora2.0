package com.nexora.android.ui.owner

object OwnerInputRules {
    const val COMPANY_NAME_MAX_LENGTH = 120
    const val INDUSTRY_MAX_LENGTH = 80
    const val COUNTRY_MAX_LENGTH = 80
    const val EMAIL_MAX_LENGTH = 254
    const val PHONE_MAX_LENGTH = 32

    fun companyName(value: String): String = value.take(COMPANY_NAME_MAX_LENGTH)
    fun industry(value: String): String = value.take(INDUSTRY_MAX_LENGTH)
    fun country(value: String): String = value.take(COUNTRY_MAX_LENGTH)
    fun email(value: String): String = value.take(EMAIL_MAX_LENGTH)
    fun phone(value: String): String = value.take(PHONE_MAX_LENGTH)
}

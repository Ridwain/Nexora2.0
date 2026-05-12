package com.nexora.android.ui.workspace

object ContactInputRules {
    const val NAME_MAX_LENGTH = 80
    const val EMAIL_MAX_LENGTH = 254
    const val PHONE_MAX_LENGTH = 32
    const val COMPANY_NAME_MAX_LENGTH = 120
    const val JOB_TITLE_MAX_LENGTH = 120
    const val SOURCE_MAX_LENGTH = 80
    const val NOTES_MAX_LENGTH = 1000
    val LIFECYCLE_STAGES = listOf("lead", "subscriber", "customer", "evangelist", "other")
    val LEAD_STATUSES = listOf("new", "open", "in_progress", "qualified", "unqualified")

    fun name(value: String): String = value.take(NAME_MAX_LENGTH)
    fun email(value: String): String = value.take(EMAIL_MAX_LENGTH)
    fun phone(value: String): String = value.take(PHONE_MAX_LENGTH)
    fun companyName(value: String): String = value.take(COMPANY_NAME_MAX_LENGTH)
    fun jobTitle(value: String): String = value.take(JOB_TITLE_MAX_LENGTH)
    fun source(value: String): String = value.take(SOURCE_MAX_LENGTH)
    fun notes(value: String): String = value.take(NOTES_MAX_LENGTH)
    fun lifecycleStage(value: String): String = value.takeIf { it in LIFECYCLE_STAGES } ?: "lead"
    fun leadStatus(value: String): String = value.takeIf { it in LEAD_STATUSES } ?: "new"
}

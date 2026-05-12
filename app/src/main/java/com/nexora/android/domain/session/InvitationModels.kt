package com.nexora.android.domain.session

data class EmployeeInviteResult(
    val invitationId: String,
    val tenantId: String,
    val email: String,
    val token: String,
    val inviteUrl: String,
    val expiresAt: String?
)

data class CustomerInviteResult(
    val invitationId: String,
    val tenantId: String,
    val email: String,
    val token: String,
    val inviteUrl: String,
    val expiresAt: String?
)

data class PendingInvite(
    val type: PendingInviteType,
    val token: String
)

enum class PendingInviteType {
    Employee,
    Customer
}

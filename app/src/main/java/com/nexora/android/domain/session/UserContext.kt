package com.nexora.android.domain.session

data class UserContext(
    val contextId: String,
    val role: UserRole,
    val tenantId: String?,
    val tenantName: String?,
    val membershipId: String?,
    val employeeProfileId: String?,
    val customerIdentityId: String?,
    val customerTenantProfileId: String?,
    val status: String?,
    val createdAt: String?
)

enum class UserRole {
    Owner,
    Employee,
    Customer,
    Unknown
}

data class OwnerTenantContext(
    val context: UserContext
)

data class CrmContact(
    val id: String,
    val tenantId: String,
    val customerTenantProfileId: String?,
    val firstName: String,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val companyName: String?,
    val jobTitle: String?,
    val lifecycleStage: String,
    val leadStatus: String,
    val source: String?,
    val notes: String?,
    val archivedAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class ContactTimelineItem(
    val id: String,
    val type: ContactTimelineItemType,
    val body: String?,
    val eventType: String?,
    val actorUserId: String?,
    val createdAt: String?
)

enum class ContactTimelineItemType {
    Note,
    Event,
    Unknown
}

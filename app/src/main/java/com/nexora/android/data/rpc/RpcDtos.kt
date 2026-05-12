package com.nexora.android.data.rpc

import com.google.gson.annotations.SerializedName
import com.nexora.android.domain.session.CrmContact
import com.nexora.android.domain.session.CustomerInviteResult
import com.nexora.android.domain.session.EmployeeInviteResult
import com.nexora.android.domain.session.UserContext
import com.nexora.android.domain.session.UserRole

data class EnsureUserProfileRequest(
    @SerializedName("p_display_name") val displayName: String? = null,
    @SerializedName("p_phone") val phone: String? = null,
    @SerializedName("p_timezone") val timezone: String? = null
)

data class UserProfileDto(
    @SerializedName("user_id") val userId: String? = null,
    val email: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    val phone: String? = null,
    val timezone: String? = null
)

data class CreateOwnerTenantRequest(
    @SerializedName("p_name") val name: String,
    @SerializedName("p_industry") val industry: String? = null,
    @SerializedName("p_country") val country: String? = null,
    @SerializedName("p_timezone") val timezone: String? = null,
    @SerializedName("p_email") val email: String? = null,
    @SerializedName("p_phone") val phone: String? = null
)

data class EnsureCustomerIdentityRequest(
    @SerializedName("p_first_name") val firstName: String,
    @SerializedName("p_last_name") val lastName: String,
    @SerializedName("p_phone") val phone: String? = null,
    @SerializedName("p_avatar_url") val avatarUrl: String? = null
)

data class CustomerIdentityDto(
    @SerializedName("customer_identity_id") val customerIdentityId: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    val email: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val phone: String? = null,
    val status: String? = null
)

data class CreateEmployeeInvitationRequest(
    @SerializedName("p_tenant_id") val tenantId: String,
    @SerializedName("p_email") val email: String,
    @SerializedName("p_full_name") val fullName: String? = null,
    @SerializedName("p_job_title") val jobTitle: String? = null
)

data class CreateCustomerInvitationRequest(
    @SerializedName("p_tenant_id") val tenantId: String,
    @SerializedName("p_email") val email: String,
    @SerializedName("p_first_name") val firstName: String? = null,
    @SerializedName("p_last_name") val lastName: String? = null
)

data class AcceptEmployeeInvitationRequest(
    @SerializedName("p_token") val token: String,
    @SerializedName("p_full_name") val fullName: String? = null,
    @SerializedName("p_phone") val phone: String? = null
)

data class AcceptCustomerInvitationRequest(
    @SerializedName("p_token") val token: String,
    @SerializedName("p_profile") val profile: Map<String, String?> = emptyMap()
)

data class ListCrmContactsRequest(
    @SerializedName("p_tenant_id") val tenantId: String
)

data class GetCrmContactRequest(
    @SerializedName("p_tenant_id") val tenantId: String,
    @SerializedName("p_contact_id") val contactId: String
)

data class CreateCrmContactRequest(
    @SerializedName("p_tenant_id") val tenantId: String,
    @SerializedName("p_first_name") val firstName: String,
    @SerializedName("p_last_name") val lastName: String? = null,
    @SerializedName("p_email") val email: String? = null,
    @SerializedName("p_phone") val phone: String? = null,
    @SerializedName("p_company_name") val companyName: String? = null,
    @SerializedName("p_job_title") val jobTitle: String? = null,
    @SerializedName("p_source") val source: String? = null,
    @SerializedName("p_notes") val notes: String? = null
)

data class UpdateCrmContactRequest(
    @SerializedName("p_tenant_id") val tenantId: String,
    @SerializedName("p_contact_id") val contactId: String,
    @SerializedName("p_first_name") val firstName: String,
    @SerializedName("p_last_name") val lastName: String? = null,
    @SerializedName("p_email") val email: String? = null,
    @SerializedName("p_phone") val phone: String? = null,
    @SerializedName("p_company_name") val companyName: String? = null,
    @SerializedName("p_job_title") val jobTitle: String? = null,
    @SerializedName("p_lifecycle_stage") val lifecycleStage: String = "lead",
    @SerializedName("p_lead_status") val leadStatus: String = "new",
    @SerializedName("p_source") val source: String? = null,
    @SerializedName("p_notes") val notes: String? = null
)

data class ArchiveCrmContactRequest(
    @SerializedName("p_tenant_id") val tenantId: String,
    @SerializedName("p_contact_id") val contactId: String
)

data class CrmContactDto(
    val id: String? = null,
    @SerializedName("tenant_id") val tenantId: String? = null,
    @SerializedName("customer_tenant_profile_id") val customerTenantProfileId: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("job_title") val jobTitle: String? = null,
    @SerializedName("lifecycle_stage") val lifecycleStage: String? = null,
    @SerializedName("lead_status") val leadStatus: String? = null,
    val source: String? = null,
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(): CrmContact = CrmContact(
        id = id.orEmpty(),
        tenantId = tenantId.orEmpty(),
        customerTenantProfileId = customerTenantProfileId,
        firstName = firstName.orEmpty(),
        lastName = lastName,
        email = email,
        phone = phone,
        companyName = companyName,
        jobTitle = jobTitle,
        lifecycleStage = lifecycleStage ?: "lead",
        leadStatus = leadStatus ?: "new",
        source = source,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

data class UserContextDto(
    @SerializedName("context_id") val contextId: String? = null,
    val role: String? = null,
    @SerializedName("tenant_id") val tenantId: String? = null,
    @SerializedName("tenant_name") val tenantName: String? = null,
    @SerializedName("membership_id") val membershipId: String? = null,
    @SerializedName("employee_profile_id") val employeeProfileId: String? = null,
    @SerializedName("customer_identity_id") val customerIdentityId: String? = null,
    @SerializedName("customer_tenant_profile_id") val customerTenantProfileId: String? = null,
    val status: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    fun toDomain(): UserContext = UserContext(
        contextId = contextId.orEmpty(),
        role = when (role) {
            "owner" -> UserRole.Owner
            "employee" -> UserRole.Employee
            "customer" -> UserRole.Customer
            else -> UserRole.Unknown
        },
        tenantId = tenantId,
        tenantName = tenantName,
        membershipId = membershipId,
        employeeProfileId = employeeProfileId,
        customerIdentityId = customerIdentityId,
        customerTenantProfileId = customerTenantProfileId,
        status = status,
        createdAt = createdAt
    )
}

data class InvitationResultDto(
    @SerializedName("invitation_id") val invitationId: String? = null,
    @SerializedName("tenant_id") val tenantId: String? = null,
    val email: String? = null,
    val token: String? = null,
    @SerializedName("invite_url") val inviteUrl: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null
) {
    fun toEmployeeInviteResult(): EmployeeInviteResult = EmployeeInviteResult(
        invitationId = invitationId.orEmpty(),
        tenantId = tenantId.orEmpty(),
        email = email.orEmpty(),
        token = token.orEmpty(),
        inviteUrl = inviteUrl.orEmpty(),
        expiresAt = expiresAt
    )

    fun toCustomerInviteResult(): CustomerInviteResult = CustomerInviteResult(
        invitationId = invitationId.orEmpty(),
        tenantId = tenantId.orEmpty(),
        email = email.orEmpty(),
        token = token.orEmpty(),
        inviteUrl = inviteUrl.orEmpty(),
        expiresAt = expiresAt
    )
}

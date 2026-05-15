package com.nexora.android.data.rpc

import com.nexora.android.domain.session.CustomerInviteResult
import com.nexora.android.domain.session.CrmContact
import com.nexora.android.domain.session.EmployeeInviteResult
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.domain.session.UserContext

interface RpcRepository {
    suspend fun ensureUserProfile(
        displayName: String? = null,
        phone: String? = null,
        timezone: String? = null
    ): NexoraResult<UserProfileDto>

    suspend fun getUserContexts(): NexoraResult<List<UserContext>>

    suspend fun listCrmContacts(tenantId: String): NexoraResult<List<CrmContact>>

    suspend fun listArchivedCrmContacts(tenantId: String): NexoraResult<List<CrmContact>>

    suspend fun searchCrmContacts(
        tenantId: String,
        query: String? = null,
        lifecycleStage: String? = null,
        leadStatus: String? = null,
        sort: String = "newest",
        limit: Int = 100
    ): NexoraResult<List<CrmContact>>

    suspend fun getCrmContact(
        tenantId: String,
        contactId: String
    ): NexoraResult<CrmContact>

    suspend fun createCrmContact(
        tenantId: String,
        firstName: String,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        companyName: String? = null,
        jobTitle: String? = null,
        source: String? = null,
        notes: String? = null
    ): NexoraResult<CrmContact>

    suspend fun updateCrmContact(
        tenantId: String,
        contactId: String,
        firstName: String,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        companyName: String? = null,
        jobTitle: String? = null,
        lifecycleStage: String = "lead",
        leadStatus: String = "new",
        source: String? = null,
        notes: String? = null
    ): NexoraResult<CrmContact>

    suspend fun archiveCrmContact(
        tenantId: String,
        contactId: String
    ): NexoraResult<CrmContact>

    suspend fun restoreCrmContact(
        tenantId: String,
        contactId: String
    ): NexoraResult<CrmContact>

    suspend fun createOwnerTenant(
        name: String,
        industry: String? = null,
        country: String? = null,
        timezone: String? = null,
        email: String? = null,
        phone: String? = null
    ): NexoraResult<UserContext>

    suspend fun ensureCustomerIdentity(
        firstName: String,
        lastName: String,
        phone: String? = null,
        avatarUrl: String? = null
    ): NexoraResult<CustomerIdentityDto>

    suspend fun createEmployeeInvitation(
        tenantId: String,
        email: String,
        fullName: String? = null,
        jobTitle: String? = null
    ): NexoraResult<EmployeeInviteResult>

    suspend fun acceptEmployeeInvitation(
        token: String,
        fullName: String? = null,
        phone: String? = null
    ): NexoraResult<UserContext>

    suspend fun createCustomerInvitation(
        tenantId: String,
        email: String,
        firstName: String? = null,
        lastName: String? = null
    ): NexoraResult<CustomerInviteResult>

    suspend fun acceptCustomerInvitation(
        token: String,
        profile: Map<String, String?> = emptyMap()
    ): NexoraResult<UserContext>
}

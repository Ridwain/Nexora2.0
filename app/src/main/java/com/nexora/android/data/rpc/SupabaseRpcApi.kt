package com.nexora.android.data.rpc

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseRpcApi {
    @POST("rpc/ensure_user_profile")
    suspend fun ensureUserProfile(
        @Header("Authorization") authorization: String,
        @Body request: EnsureUserProfileRequest
    ): UserProfileDto

    @POST("rpc/get_user_contexts")
    suspend fun getUserContexts(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, String> = emptyMap()
    ): List<UserContextDto>

    @POST("rpc/list_crm_contacts")
    suspend fun listCrmContacts(
        @Header("Authorization") authorization: String,
        @Body request: ListCrmContactsRequest
    ): List<CrmContactDto>

    @POST("rpc/list_archived_crm_contacts")
    suspend fun listArchivedCrmContacts(
        @Header("Authorization") authorization: String,
        @Body request: ListArchivedCrmContactsRequest
    ): List<CrmContactDto>

    @POST("rpc/search_crm_contacts")
    suspend fun searchCrmContacts(
        @Header("Authorization") authorization: String,
        @Body request: SearchCrmContactsRequest
    ): List<CrmContactDto>

    @POST("rpc/get_crm_contact")
    suspend fun getCrmContact(
        @Header("Authorization") authorization: String,
        @Body request: GetCrmContactRequest
    ): CrmContactDto

    @POST("rpc/create_crm_contact")
    suspend fun createCrmContact(
        @Header("Authorization") authorization: String,
        @Body request: CreateCrmContactRequest
    ): CrmContactDto

    @POST("rpc/update_crm_contact")
    suspend fun updateCrmContact(
        @Header("Authorization") authorization: String,
        @Body request: UpdateCrmContactRequest
    ): CrmContactDto

    @POST("rpc/archive_crm_contact")
    suspend fun archiveCrmContact(
        @Header("Authorization") authorization: String,
        @Body request: ArchiveCrmContactRequest
    ): CrmContactDto

    @POST("rpc/restore_crm_contact")
    suspend fun restoreCrmContact(
        @Header("Authorization") authorization: String,
        @Body request: RestoreCrmContactRequest
    ): CrmContactDto

    @POST("rpc/create_owner_tenant")
    suspend fun createOwnerTenant(
        @Header("Authorization") authorization: String,
        @Body request: CreateOwnerTenantRequest
    ): UserContextDto

    @POST("rpc/ensure_customer_identity")
    suspend fun ensureCustomerIdentity(
        @Header("Authorization") authorization: String,
        @Body request: EnsureCustomerIdentityRequest
    ): CustomerIdentityDto

    @POST("rpc/create_employee_invitation")
    suspend fun createEmployeeInvitation(
        @Header("Authorization") authorization: String,
        @Body request: CreateEmployeeInvitationRequest
    ): InvitationResultDto

    @POST("rpc/accept_employee_invitation")
    suspend fun acceptEmployeeInvitation(
        @Header("Authorization") authorization: String,
        @Body request: AcceptEmployeeInvitationRequest
    ): UserContextDto

    @POST("rpc/create_customer_invitation")
    suspend fun createCustomerInvitation(
        @Header("Authorization") authorization: String,
        @Body request: CreateCustomerInvitationRequest
    ): InvitationResultDto

    @POST("rpc/accept_customer_invitation")
    suspend fun acceptCustomerInvitation(
        @Header("Authorization") authorization: String,
        @Body request: AcceptCustomerInvitationRequest
    ): UserContextDto
}

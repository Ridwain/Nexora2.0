package com.nexora.android.data.rpc

import com.nexora.android.core.deeplink.DeepLinkRepository
import com.nexora.android.core.network.ApiErrorMapper
import com.nexora.android.core.network.safeApiCall
import com.nexora.android.core.session.SessionRepository
import com.nexora.android.domain.session.CustomerInviteResult
import com.nexora.android.domain.session.CrmContact
import com.nexora.android.domain.session.EmployeeInviteResult
import com.nexora.android.domain.session.NexoraError
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.domain.session.PendingInviteType
import com.nexora.android.domain.session.UserContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseRpcRepository @Inject constructor(
    private val rpcApi: SupabaseRpcApi,
    private val sessionRepository: SessionRepository,
    private val deepLinkRepository: DeepLinkRepository,
    private val errorMapper: ApiErrorMapper
) : RpcRepository {
    override suspend fun ensureUserProfile(
        displayName: String?,
        phone: String?,
        timezone: String?
    ): NexoraResult<UserProfileDto> = authenticatedCall { authorization ->
        rpcApi.ensureUserProfile(
            authorization = authorization,
            request = EnsureUserProfileRequest(displayName = displayName, phone = phone, timezone = timezone)
        )
    }

    override suspend fun getUserContexts(): NexoraResult<List<UserContext>> = authenticatedCall { authorization ->
        rpcApi.getUserContexts(authorization = authorization).map(UserContextDto::toDomain)
    }

    override suspend fun listCrmContacts(tenantId: String): NexoraResult<List<CrmContact>> =
        authenticatedCall { authorization ->
            rpcApi.listCrmContacts(
                authorization = authorization,
                request = ListCrmContactsRequest(tenantId = tenantId)
            ).map(CrmContactDto::toDomain)
        }

    override suspend fun listArchivedCrmContacts(tenantId: String): NexoraResult<List<CrmContact>> =
        authenticatedCall { authorization ->
            rpcApi.listArchivedCrmContacts(
                authorization = authorization,
                request = ListArchivedCrmContactsRequest(tenantId = tenantId)
            ).map(CrmContactDto::toDomain)
        }

    override suspend fun getCrmContact(
        tenantId: String,
        contactId: String
    ): NexoraResult<CrmContact> = authenticatedCall { authorization ->
        rpcApi.getCrmContact(
            authorization = authorization,
            request = GetCrmContactRequest(tenantId = tenantId, contactId = contactId)
        ).toDomain()
    }

    override suspend fun createCrmContact(
        tenantId: String,
        firstName: String,
        lastName: String?,
        email: String?,
        phone: String?,
        companyName: String?,
        jobTitle: String?,
        source: String?,
        notes: String?
    ): NexoraResult<CrmContact> = authenticatedCall { authorization ->
        rpcApi.createCrmContact(
            authorization = authorization,
            request = CreateCrmContactRequest(
                tenantId = tenantId,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                companyName = companyName,
                jobTitle = jobTitle,
                source = source,
                notes = notes
            )
        ).toDomain()
    }

    override suspend fun updateCrmContact(
        tenantId: String,
        contactId: String,
        firstName: String,
        lastName: String?,
        email: String?,
        phone: String?,
        companyName: String?,
        jobTitle: String?,
        lifecycleStage: String,
        leadStatus: String,
        source: String?,
        notes: String?
    ): NexoraResult<CrmContact> = authenticatedCall { authorization ->
        rpcApi.updateCrmContact(
            authorization = authorization,
            request = UpdateCrmContactRequest(
                tenantId = tenantId,
                contactId = contactId,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                companyName = companyName,
                jobTitle = jobTitle,
                lifecycleStage = lifecycleStage,
                leadStatus = leadStatus,
                source = source,
                notes = notes
            )
        ).toDomain()
    }

    override suspend fun archiveCrmContact(
        tenantId: String,
        contactId: String
    ): NexoraResult<CrmContact> = authenticatedCall { authorization ->
        rpcApi.archiveCrmContact(
            authorization = authorization,
            request = ArchiveCrmContactRequest(tenantId = tenantId, contactId = contactId)
        ).toDomain()
    }

    override suspend fun restoreCrmContact(
        tenantId: String,
        contactId: String
    ): NexoraResult<CrmContact> = authenticatedCall { authorization ->
        rpcApi.restoreCrmContact(
            authorization = authorization,
            request = RestoreCrmContactRequest(tenantId = tenantId, contactId = contactId)
        ).toDomain()
    }

    override suspend fun createOwnerTenant(
        name: String,
        industry: String?,
        country: String?,
        timezone: String?,
        email: String?,
        phone: String?
    ): NexoraResult<UserContext> = authenticatedCall { authorization ->
        rpcApi.createOwnerTenant(
            authorization = authorization,
            request = CreateOwnerTenantRequest(
                name = name,
                industry = industry,
                country = country,
                timezone = timezone,
                email = email,
                phone = phone
            )
        ).toDomain()
    }

    override suspend fun ensureCustomerIdentity(
        firstName: String,
        lastName: String,
        phone: String?,
        avatarUrl: String?
    ): NexoraResult<CustomerIdentityDto> = authenticatedCall { authorization ->
        rpcApi.ensureCustomerIdentity(
            authorization = authorization,
            request = EnsureCustomerIdentityRequest(
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                avatarUrl = avatarUrl
            )
        )
    }

    override suspend fun createEmployeeInvitation(
        tenantId: String,
        email: String,
        fullName: String?,
        jobTitle: String?
    ): NexoraResult<EmployeeInviteResult> = authenticatedCall { authorization ->
        rpcApi.createEmployeeInvitation(
            authorization = authorization,
            request = CreateEmployeeInvitationRequest(
                tenantId = tenantId,
                email = email,
                fullName = fullName,
                jobTitle = jobTitle
            )
        ).toEmployeeInviteResult()
    }

    override suspend fun acceptEmployeeInvitation(
        token: String,
        fullName: String?,
        phone: String?
    ): NexoraResult<UserContext> = authenticatedCall { authorization ->
        rpcApi.acceptEmployeeInvitation(
            authorization = authorization,
            request = AcceptEmployeeInvitationRequest(token = token, fullName = fullName, phone = phone)
        ).toDomain().also {
            clearPendingInviteIfMatching(PendingInviteType.Employee, token)
        }
    }

    override suspend fun createCustomerInvitation(
        tenantId: String,
        email: String,
        firstName: String?,
        lastName: String?
    ): NexoraResult<CustomerInviteResult> = authenticatedCall { authorization ->
        rpcApi.createCustomerInvitation(
            authorization = authorization,
            request = CreateCustomerInvitationRequest(
                tenantId = tenantId,
                email = email,
                firstName = firstName,
                lastName = lastName
            )
        ).toCustomerInviteResult()
    }

    override suspend fun acceptCustomerInvitation(
        token: String,
        profile: Map<String, String?>
    ): NexoraResult<UserContext> = authenticatedCall { authorization ->
        rpcApi.acceptCustomerInvitation(
            authorization = authorization,
            request = AcceptCustomerInvitationRequest(token = token, profile = profile)
        ).toDomain().also {
            clearPendingInviteIfMatching(PendingInviteType.Customer, token)
        }
    }

    private suspend fun <T> authenticatedCall(block: suspend (String) -> T): NexoraResult<T> {
        val authorization = sessionRepository.authorizationHeader()
            ?: return NexoraResult.Failure(NexoraError.Unauthorized("No active session"))

        return safeApiCall(errorMapper) { block(authorization) }
    }

    private suspend fun clearPendingInviteIfMatching(type: PendingInviteType, token: String) {
        val pendingInvite = deepLinkRepository.currentInvite()
        if (pendingInvite?.type == type && pendingInvite.token == token) {
            deepLinkRepository.clearInvite()
        }
    }
}

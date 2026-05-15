package com.nexora.android.ui.auth

import com.nexora.android.core.session.SessionRepository
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.auth.PendingSignupProfileStore
import com.nexora.android.data.auth.PendingSignupVerification
import com.nexora.android.data.auth.PendingSignupVerificationStore
import com.nexora.android.data.rpc.CustomerIdentityDto
import com.nexora.android.data.rpc.RpcRepository
import com.nexora.android.data.rpc.UserProfileDto
import com.nexora.android.domain.session.CrmContact
import com.nexora.android.domain.session.CustomerInviteResult
import com.nexora.android.domain.session.EmployeeInviteResult
import com.nexora.android.domain.session.NexoraResult
import com.nexora.android.domain.session.UserContext
import com.nexora.android.domain.session.UserRole
import com.nexora.android.domain.session.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    private val sessionState = MutableStateFlow<UserSession?>(null)
    override val currentSession: Flow<UserSession?> = sessionState

    var signInCalls = 0
    var signUpCalls = 0
    var signOutCalls = 0
    var verifySignupOtpCalls = 0
    var resendSignupOtpCalls = 0
    var signInResult: NexoraResult<UserSession> = NexoraResult.Success(testSession())
    var signUpResult: NexoraResult<UserSession?> = NexoraResult.Success(testSession())
    var verifySignupOtpResult: NexoraResult<UserSession?> = NexoraResult.Success(testSession())
    var resendSignupOtpResult: NexoraResult<Unit> = NexoraResult.Success(Unit)

    override suspend fun signUp(email: String, password: String): NexoraResult<UserSession?> {
        signUpCalls++
        return signUpResult
    }

    override suspend fun signIn(email: String, password: String): NexoraResult<UserSession> {
        signInCalls++
        return signInResult
    }

    override suspend fun verifySignupOtp(email: String, token: String): NexoraResult<UserSession?> {
        verifySignupOtpCalls++
        return verifySignupOtpResult
    }

    override suspend fun resendSignupOtp(email: String): NexoraResult<Unit> {
        resendSignupOtpCalls++
        return resendSignupOtpResult
    }

    override suspend fun refreshSession(refreshToken: String): NexoraResult<UserSession> =
        NexoraResult.Success(testSession())

    override suspend fun signOut(): NexoraResult<Unit> {
        signOutCalls++
        sessionState.value = null
        return NexoraResult.Success(Unit)
    }
}

class FakePendingSignupProfileStore : PendingSignupProfileStore, PendingSignupVerificationStore {
    private val pendingState = MutableStateFlow<PendingSignupVerification?>(null)
    private val visiblePendingState = MutableStateFlow<PendingSignupVerification?>(null)
    var saveCalls = 0
    var markVerifiedCalls = 0
    var clearCalls = 0
    var clearAllCalls = 0

    override suspend fun save(email: String, displayName: String) {
        save(email = email, displayName = displayName, createdAtEpochSeconds = 0L)
    }

    override fun pendingVerification(): Flow<PendingSignupVerification?> = visiblePendingState

    override suspend fun pendingFor(email: String): PendingSignupVerification? {
        val normalizedEmail = email.trim().lowercase()
        return visiblePendingState.value?.takeIf { it.email == normalizedEmail }
    }

    override suspend fun save(email: String, displayName: String, createdAtEpochSeconds: Long) {
        saveCalls++
        val pending = PendingSignupVerification(
            email = email.trim().lowercase(),
            displayName = displayName,
            createdAtEpochSeconds = createdAtEpochSeconds
        )
        pendingState.value = pending
        visiblePendingState.value = pending
    }

    override suspend fun markVerified(email: String) {
        markVerifiedCalls++
        val normalizedEmail = email.trim().lowercase()
        if (pendingState.value?.email == normalizedEmail) {
            visiblePendingState.value = null
        }
    }

    override suspend fun displayNameFor(email: String): String? {
        val normalizedEmail = email.trim().lowercase()
        return pendingState.value
            ?.takeIf { it.email == normalizedEmail && it.displayName.isNotBlank() }
            ?.displayName
    }

    override suspend fun clear(email: String) {
        clearCalls++
        val normalizedEmail = email.trim().lowercase()
        if (pendingState.value?.email == normalizedEmail) {
            pendingState.value = null
            visiblePendingState.value = null
        }
    }

    override suspend fun clearAll() {
        clearAllCalls++
        pendingState.value = null
        visiblePendingState.value = null
    }
}

class FakeSessionRepository : SessionRepository {
    private val sessionState = MutableStateFlow<UserSession?>(testSession())
    override val currentSession: Flow<UserSession?> = sessionState

    var clearSessionCalls = 0

    override suspend fun currentSessionNow(): UserSession? = sessionState.value

    override suspend fun saveSession(session: UserSession) {
        sessionState.value = session
    }

    override suspend fun clearSession() {
        clearSessionCalls++
        sessionState.value = null
    }

    override suspend fun authorizationHeader(): String? = sessionState.value?.authorizationHeader
}

class FakeRpcRepository : RpcRepository {
    var ensureUserProfileCalls = 0
    var lastEnsuredDisplayName: String? = null
    var getUserContextsCalls = 0
    var createOwnerTenantCalls = 0
    var lastOwnerTenantName: String? = null
    var lastOwnerTenantIndustry: String? = null
    var lastOwnerTenantCountry: String? = null
    var lastOwnerTenantTimezone: String? = null
    var lastOwnerTenantEmail: String? = null
    var lastOwnerTenantPhone: String? = null
    var listCrmContactsCalls = 0
    var listArchivedCrmContactsCalls = 0
    var searchCrmContactsCalls = 0
    var getCrmContactCalls = 0
    var createCrmContactCalls = 0
    var updateCrmContactCalls = 0
    var archiveCrmContactCalls = 0
    var restoreCrmContactCalls = 0
    var lastCrmContactTenantId: String? = null
    var lastCrmContactId: String? = null
    var lastCrmContactFirstName: String? = null
    var lastCrmContactEmail: String? = null
    var lastCrmContactSearchQuery: String? = null
    var lastCrmContactLifecycleStage: String? = null
    var lastCrmContactLeadStatus: String? = null
    var lastCrmContactSort: String? = null
    var getUserContextsResult: NexoraResult<List<UserContext>> = NexoraResult.Success(emptyList())
    var createOwnerTenantResult: NexoraResult<UserContext> = NexoraResult.Success(testContext())
    var listCrmContactsResult: NexoraResult<List<CrmContact>> = NexoraResult.Success(emptyList())
    var listArchivedCrmContactsResult: NexoraResult<List<CrmContact>> = NexoraResult.Success(emptyList())
    var listCrmContactsHandler: (suspend (String) -> NexoraResult<List<CrmContact>>)? = null
    var searchCrmContactsResult: NexoraResult<List<CrmContact>> = NexoraResult.Success(emptyList())
    var searchCrmContactsHandler: (suspend (
        tenantId: String,
        query: String?,
        lifecycleStage: String?,
        leadStatus: String?,
        sort: String
    ) -> NexoraResult<List<CrmContact>>)? = null
    var getCrmContactResult: NexoraResult<CrmContact> = NexoraResult.Success(testContact())
    var createCrmContactResult: NexoraResult<CrmContact> = NexoraResult.Success(testContact())
    var updateCrmContactResult: NexoraResult<CrmContact> = NexoraResult.Success(testContact())
    var archiveCrmContactResult: NexoraResult<CrmContact> = NexoraResult.Success(testContact())
    var restoreCrmContactResult: NexoraResult<CrmContact> = NexoraResult.Success(testContact())

    override suspend fun ensureUserProfile(
        displayName: String?,
        phone: String?,
        timezone: String?
    ): NexoraResult<UserProfileDto> {
        ensureUserProfileCalls++
        lastEnsuredDisplayName = displayName
        return NexoraResult.Success(UserProfileDto(userId = "user-id", email = "test@example.com"))
    }

    override suspend fun getUserContexts(): NexoraResult<List<UserContext>> {
        getUserContextsCalls++
        return getUserContextsResult
    }

    override suspend fun listCrmContacts(tenantId: String): NexoraResult<List<CrmContact>> {
        listCrmContactsCalls++
        lastCrmContactTenantId = tenantId
        listCrmContactsHandler?.let { handler ->
            return handler(tenantId)
        }
        return listCrmContactsResult
    }

    override suspend fun listArchivedCrmContacts(tenantId: String): NexoraResult<List<CrmContact>> {
        listArchivedCrmContactsCalls++
        lastCrmContactTenantId = tenantId
        return listArchivedCrmContactsResult
    }

    override suspend fun searchCrmContacts(
        tenantId: String,
        query: String?,
        lifecycleStage: String?,
        leadStatus: String?,
        sort: String,
        limit: Int
    ): NexoraResult<List<CrmContact>> {
        searchCrmContactsCalls++
        lastCrmContactTenantId = tenantId
        lastCrmContactSearchQuery = query
        lastCrmContactLifecycleStage = lifecycleStage
        lastCrmContactLeadStatus = leadStatus
        lastCrmContactSort = sort
        searchCrmContactsHandler?.let { handler ->
            return handler(tenantId, query, lifecycleStage, leadStatus, sort)
        }
        return searchCrmContactsResult
    }

    override suspend fun getCrmContact(tenantId: String, contactId: String): NexoraResult<CrmContact> {
        getCrmContactCalls++
        lastCrmContactTenantId = tenantId
        lastCrmContactId = contactId
        return getCrmContactResult
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
    ): NexoraResult<CrmContact> {
        createCrmContactCalls++
        lastCrmContactTenantId = tenantId
        lastCrmContactFirstName = firstName
        lastCrmContactEmail = email
        return createCrmContactResult
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
    ): NexoraResult<CrmContact> {
        updateCrmContactCalls++
        lastCrmContactTenantId = tenantId
        lastCrmContactId = contactId
        lastCrmContactFirstName = firstName
        lastCrmContactEmail = email
        return updateCrmContactResult
    }

    override suspend fun archiveCrmContact(tenantId: String, contactId: String): NexoraResult<CrmContact> {
        archiveCrmContactCalls++
        lastCrmContactTenantId = tenantId
        lastCrmContactId = contactId
        return archiveCrmContactResult
    }

    override suspend fun restoreCrmContact(tenantId: String, contactId: String): NexoraResult<CrmContact> {
        restoreCrmContactCalls++
        lastCrmContactTenantId = tenantId
        lastCrmContactId = contactId
        return restoreCrmContactResult
    }

    override suspend fun createOwnerTenant(
        name: String,
        industry: String?,
        country: String?,
        timezone: String?,
        email: String?,
        phone: String?
    ): NexoraResult<UserContext> {
        createOwnerTenantCalls++
        lastOwnerTenantName = name
        lastOwnerTenantIndustry = industry
        lastOwnerTenantCountry = country
        lastOwnerTenantTimezone = timezone
        lastOwnerTenantEmail = email
        lastOwnerTenantPhone = phone
        return createOwnerTenantResult
    }

    override suspend fun ensureCustomerIdentity(
        firstName: String,
        lastName: String,
        phone: String?,
        avatarUrl: String?
    ): NexoraResult<CustomerIdentityDto> = NexoraResult.Success(CustomerIdentityDto())

    override suspend fun createEmployeeInvitation(
        tenantId: String,
        email: String,
        fullName: String?,
        jobTitle: String?
    ): NexoraResult<EmployeeInviteResult> = error("Not used")

    override suspend fun acceptEmployeeInvitation(
        token: String,
        fullName: String?,
        phone: String?
    ): NexoraResult<UserContext> = NexoraResult.Success(testContext())

    override suspend fun createCustomerInvitation(
        tenantId: String,
        email: String,
        firstName: String?,
        lastName: String?
    ): NexoraResult<CustomerInviteResult> = error("Not used")

    override suspend fun acceptCustomerInvitation(
        token: String,
        profile: Map<String, String?>
    ): NexoraResult<UserContext> = NexoraResult.Success(testContext())
}

fun testSession(): UserSession = UserSession(
    accessToken = "access",
    refreshToken = "refresh",
    tokenType = "bearer",
    expiresAtEpochSeconds = 1234L,
    user = null
)

fun testContext(): UserContext = UserContext(
    contextId = "owner:tenant-id",
    role = UserRole.Owner,
    tenantId = "tenant-id",
    tenantName = "Nexora Demo",
    membershipId = "membership-id",
    employeeProfileId = null,
    customerIdentityId = null,
    customerTenantProfileId = null,
    status = "active",
    createdAt = "2026-05-05T00:00:00Z"
)

fun testContact(): CrmContact = CrmContact(
    id = "contact-id",
    tenantId = "tenant-id",
    customerTenantProfileId = null,
    firstName = "Nexora",
    lastName = "Contact",
    email = "contact@example.com",
    phone = null,
    companyName = "Example Co",
    jobTitle = "Manager",
    lifecycleStage = "lead",
    leadStatus = "new",
    source = "Website",
    notes = null,
    archivedAt = null,
    createdAt = "2026-05-12T00:00:00Z",
    updatedAt = "2026-05-12T00:00:00Z"
)

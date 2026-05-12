package com.nexora.android.data.rpc

import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.http.POST

class SupabaseRpcApiTest {
    @Test
    fun exposesExpectedSectionThreeRpcFunctionNames() {
        val expectedRoutes = mapOf(
            "ensureUserProfile" to "rpc/ensure_user_profile",
            "getUserContexts" to "rpc/get_user_contexts",
            "createOwnerTenant" to "rpc/create_owner_tenant",
            "ensureCustomerIdentity" to "rpc/ensure_customer_identity",
            "createEmployeeInvitation" to "rpc/create_employee_invitation",
            "acceptEmployeeInvitation" to "rpc/accept_employee_invitation",
            "createCustomerInvitation" to "rpc/create_customer_invitation",
            "acceptCustomerInvitation" to "rpc/accept_customer_invitation"
        )

        expectedRoutes.forEach { (methodName, expectedRoute) ->
            val method = SupabaseRpcApi::class.java.methods.first { it.name == methodName }
            assertEquals(expectedRoute, method.getAnnotation(POST::class.java)?.value)
        }
    }
}

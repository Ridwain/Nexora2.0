package com.nexora.android.core.deeplink

import com.nexora.android.domain.session.PendingInviteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InviteDeepLinkParserTest {
    private val parser = InviteDeepLinkParser()

    @Test
    fun parsesEmployeeInviteLink() {
        val invite = parser.parse("nexora://employee-invite/token-123")

        assertEquals(PendingInviteType.Employee, invite?.type)
        assertEquals("token-123", invite?.token)
    }

    @Test
    fun parsesCustomerInviteLink() {
        val invite = parser.parse("nexora://customer-invite/customer-token")

        assertEquals(PendingInviteType.Customer, invite?.type)
        assertEquals("customer-token", invite?.token)
    }

    @Test
    fun rejectsInvalidInviteLinks() {
        assertNull(parser.parse("https://employee-invite/token-123"))
        assertNull(parser.parse("nexora://unknown/token-123"))
        assertNull(parser.parse("nexora://employee-invite/"))
        assertNull(parser.parse(null))
    }
}

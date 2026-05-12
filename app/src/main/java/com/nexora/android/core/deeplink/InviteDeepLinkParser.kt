package com.nexora.android.core.deeplink

import com.nexora.android.domain.session.PendingInvite
import com.nexora.android.domain.session.PendingInviteType
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InviteDeepLinkParser @Inject constructor() {
    fun parse(rawUri: String?): PendingInvite? {
        if (rawUri.isNullOrBlank()) return null

        val uri = try {
            URI(rawUri)
        } catch (_: URISyntaxException) {
            return null
        }

        if (uri.scheme != "nexora") return null

        val type = when (uri.host) {
            "employee-invite" -> PendingInviteType.Employee
            "customer-invite" -> PendingInviteType.Customer
            else -> return null
        }

        val token = uri.path
            ?.trim('/')
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return PendingInvite(type = type, token = token)
    }
}

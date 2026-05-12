package com.nexora.android.ui.workspace

import com.nexora.android.domain.session.UserRole

data class SelectedWorkspaceContext(
    val contextId: String,
    val role: UserRole,
    val tenantId: String,
    val tenantName: String
)

enum class CrmWorkspaceTab(
    val label: String
) {
    Dashboard("Home"),
    Contacts("Contacts"),
    Companies("Company"),
    Deals("Deals"),
    Tickets("Tickets"),
    Settings("Settings")
}

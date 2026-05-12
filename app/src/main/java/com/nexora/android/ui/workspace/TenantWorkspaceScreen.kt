package com.nexora.android.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nexora.android.domain.session.CrmContact
import com.nexora.android.domain.session.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantWorkspaceScreen(
    contextId: String,
    role: String,
    tenantId: String,
    tenantName: String,
    initialTab: String,
    onAddContact: (tenantId: String, tenantName: String) -> Unit,
    onOpenArchivedContacts: (tenantId: String, tenantName: String) -> Unit,
    onOpenContact: (tenantId: String, tenantName: String, contactId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedContext = SelectedWorkspaceContext(
        contextId = contextId,
        role = role.toUserRole(),
        tenantId = tenantId,
        tenantName = tenantName
    )
    var selectedTab by rememberSaveable(tenantId, initialTab) {
        mutableStateOf(
            CrmWorkspaceTab.entries.firstOrNull { it.name == initialTab } ?: CrmWorkspaceTab.Dashboard
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Nexora CRM") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                CrmWorkspaceTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon(),
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WorkspaceHeader(context = selectedContext)
                    when (selectedTab) {
                        CrmWorkspaceTab.Dashboard -> DashboardTab(
                            tenantId = tenantId,
                            tenantName = tenantName,
                            onAddContact = onAddContact,
                            onOpenContact = onOpenContact
                        )
                        CrmWorkspaceTab.Contacts -> ContactsTab(
                            tenantId = tenantId,
                            tenantName = tenantName,
                            onAddContact = onAddContact,
                            onOpenArchivedContacts = onOpenArchivedContacts,
                            onOpenContact = onOpenContact
                        )
                        CrmWorkspaceTab.Companies -> ModulePlaceholder(
                            icon = Icons.Outlined.Business,
                            title = "Companies",
                            subtitle = "Company records here will stay inside this workspace.",
                            nextStep = "Future CRM sections can add associated companies, domains, owners, and notes."
                        )
                        CrmWorkspaceTab.Deals -> ModulePlaceholder(
                            icon = Icons.Outlined.Handshake,
                            title = "Deals",
                            subtitle = "Deals and pipelines here will be scoped to $tenantName only.",
                            nextStep = "Future work can add pipelines, stages, deal values, and owner assignment."
                        )
                        CrmWorkspaceTab.Tickets -> ModulePlaceholder(
                            icon = Icons.Outlined.ConfirmationNumber,
                            title = "Tickets",
                            subtitle = "Support tickets here will be visible only to this company workspace.",
                            nextStep = "Future work can add customer support queues, priorities, and status workflows."
                        )
                        CrmWorkspaceTab.Settings -> ModulePlaceholder(
                            icon = Icons.Outlined.Settings,
                            title = "Workspace settings",
                            subtitle = "Settings will control this tenant without affecting other companies.",
                            nextStep = "Future work can add team permissions, billing, branding, and tenant security."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceHeader(context: SelectedWorkspaceContext) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(text = "${context.role.name} workspace") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Apartment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Text(
                    text = context.tenantName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "A tenant-scoped CRM command center. Data created here belongs only to this company workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun DashboardTab(
    tenantId: String,
    tenantName: String,
    onAddContact: (tenantId: String, tenantName: String) -> Unit,
    onOpenContact: (tenantId: String, tenantName: String, contactId: String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(tenantId) {
        viewModel.load(tenantId)
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, tenantId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load(tenantId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ContactSummaryCard(
            contactCount = uiState.contacts.size,
            isInitialLoading = uiState.isInitialLoading,
            tenantName = tenantName
        )
        QuickActionsCard(
            tenantId = tenantId,
            tenantName = tenantName,
            onAddContact = onAddContact
        )
        RecentContactsCard(
            uiState = uiState,
            tenantName = tenantName,
            onRetry = { viewModel.load(tenantId) },
            onAddContact = { onAddContact(tenantId, tenantName) },
            onOpenContact = { contactId -> onOpenContact(tenantId, tenantName, contactId) }
        )
    }
}

@Composable
private fun ContactSummaryCard(
    contactCount: Int,
    isInitialLoading: Boolean,
    tenantName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Contacts overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isInitialLoading) "Loading..." else contactCount.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (contactCount == 1) {
                    "1 CRM contact belongs only to $tenantName."
                } else {
                    "$contactCount CRM contacts belong only to $tenantName."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    tenantId: String,
    tenantName: String,
    onAddContact: (tenantId: String, tenantName: String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Create CRM records inside $tenantName only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AssistChip(
                onClick = { onAddContact(tenantId, tenantName) },
                label = { Text(text = "Add contact") },
                leadingIcon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun RecentContactsCard(
    uiState: ContactsUiState,
    tenantName: String,
    onRetry: () -> Unit,
    onAddContact: () -> Unit,
    onOpenContact: (contactId: String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recent contacts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            when {
                uiState.isInitialLoading -> {
                    Text(
                        text = "Loading recent contacts...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = "Could not load contacts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    AssistChip(
                        onClick = onRetry,
                        label = { Text(text = "Retry") }
                    )
                }
                uiState.refreshErrorMessage != null && uiState.contacts.isNotEmpty() -> {
                    Text(
                        text = "Showing saved contacts. Refresh failed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    AssistChip(
                        onClick = onRetry,
                        label = { Text(text = "Retry") }
                    )
                    uiState.contacts.take(3).forEach { contact ->
                        CompactContactRow(
                            contact = contact,
                            onClick = { onOpenContact(contact.id) }
                        )
                    }
                }
                uiState.contacts.isEmpty() -> {
                    Text(
                        text = "No contacts yet. Add the first CRM contact for $tenantName.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onAddContact) {
                        Text(text = "Add contact")
                    }
                }
                else -> {
                    uiState.contacts.take(3).forEach { contact ->
                        CompactContactRow(
                            contact = contact,
                            onClick = { onOpenContact(contact.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactContactRow(
    contact: CrmContact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = listOf(contact.firstName, contact.lastName.orEmpty())
                    .joinToString(" ")
                    .trim(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val subtitle = contact.email ?: contact.phone ?: contact.companyName
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContactsTab(
    tenantId: String,
    tenantName: String,
    onAddContact: (tenantId: String, tenantName: String) -> Unit,
    onOpenArchivedContacts: (tenantId: String, tenantName: String) -> Unit,
    onOpenContact: (tenantId: String, tenantName: String, contactId: String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(tenantId) {
        viewModel.load(tenantId)
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, tenantId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load(tenantId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Contacts belong only to $tenantName. They are CRM records, not customer app accounts yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onAddContact(tenantId, tenantName) }) {
                        Text(text = "Add contact")
                    }
                    AssistChip(
                        onClick = { onOpenArchivedContacts(tenantId, tenantName) },
                        label = { Text(text = "Archived") },
                        leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { viewModel.load(tenantId) },
                        label = { Text(text = "Refresh") }
                    )
                }
            }
        }

        val errorMessage = uiState.errorMessage
        when {
            uiState.isInitialLoading -> LoadingContacts()
            errorMessage != null -> ContactsError(
                error = errorMessage,
                onRetry = { viewModel.load(tenantId) }
            )
            uiState.contacts.isEmpty() -> EmptyContacts(
                tenantName = tenantName,
                onAddContact = { onAddContact(tenantId, tenantName) }
            )
            else -> ContactsList(
                contacts = uiState.contacts,
                refreshErrorMessage = uiState.refreshErrorMessage,
                onRetry = { viewModel.load(tenantId) },
                onOpenContact = { contactId -> onOpenContact(tenantId, tenantName, contactId) }
            )
        }
    }
}

@Composable
private fun LoadingContacts() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(text = "Loading contacts...")
        }
    }
}

@Composable
private fun ContactsError(
    error: String,
    onRetry: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text(text = "Retry")
            }
        }
    }
}

@Composable
private fun EmptyContacts(
    tenantName: String,
    onAddContact: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = "No contacts yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Add the first CRM contact for $tenantName. It will not appear in other company workspaces.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAddContact) {
                Text(text = "Add contact")
            }
        }
    }
}

@Composable
private fun ContactsList(
    contacts: List<CrmContact>,
    refreshErrorMessage: String?,
    onRetry: () -> Unit,
    onOpenContact: (contactId: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (refreshErrorMessage != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Refresh failed. Showing saved contacts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = onRetry,
                        label = { Text(text = "Retry") }
                    )
                }
            }
        }
        contacts.forEach { contact ->
            ContactCard(
                contact = contact,
                onClick = { onOpenContact(contact.id) }
            )
        }
    }
}

@Composable
private fun ContactCard(
    contact: CrmContact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = listOf(contact.firstName, contact.lastName.orEmpty())
                        .joinToString(" ")
                        .trim(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                contact.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val meta = listOfNotNull(contact.companyName, contact.jobTitle).joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = contact.leadStatus.replace("_", " "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ModulePlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String,
    nextStep: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = nextStep,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun CrmWorkspaceTab.icon(): ImageVector {
    return when (this) {
        CrmWorkspaceTab.Dashboard -> Icons.Outlined.Dashboard
        CrmWorkspaceTab.Contacts -> Icons.Outlined.Groups
        CrmWorkspaceTab.Companies -> Icons.Outlined.Business
        CrmWorkspaceTab.Deals -> Icons.Outlined.Handshake
        CrmWorkspaceTab.Tickets -> Icons.Outlined.ConfirmationNumber
        CrmWorkspaceTab.Settings -> Icons.Outlined.Settings
    }
}

private fun String.toUserRole(): UserRole {
    return UserRole.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: UserRole.Unknown
}

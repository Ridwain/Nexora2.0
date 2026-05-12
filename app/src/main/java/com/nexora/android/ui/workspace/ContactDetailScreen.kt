package com.nexora.android.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nexora.android.domain.session.CrmContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    tenantId: String,
    tenantName: String,
    contactId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onArchived: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(tenantId, contactId) {
        viewModel.load(tenantId, contactId)
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, tenantId, contactId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load(tenantId, contactId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.archived) {
        if (uiState.archived) {
            onArchived()
        }
    }

    if (uiState.showArchiveDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideArchiveDialog,
            title = { Text(text = "Archive contact?") },
            text = { Text(text = "This will hide the contact from active lists in this workspace.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.archive(tenantId, contactId) },
                    enabled = !uiState.isArchiving
                ) {
                    Text(text = if (uiState.isArchiving) "Archiving..." else "Archive")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::hideArchiveDialog,
                    enabled = !uiState.isArchiving
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEdit,
                        enabled = uiState.contact != null && !uiState.isArchiving
                    ) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = viewModel::showArchiveDialog,
                        enabled = uiState.contact != null && !uiState.isArchiving
                    ) {
                        Icon(imageVector = Icons.Outlined.Archive, contentDescription = "Archive")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 620.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        uiState.isInitialLoading -> LoadingDetail()
                        uiState.errorMessage != null -> DetailError(
                            error = uiState.errorMessage.orEmpty(),
                            onRetry = { viewModel.load(tenantId, contactId) }
                        )
                        uiState.contact != null -> ContactDetailContent(
                            contact = uiState.contact!!,
                            tenantName = tenantName,
                            archiveError = uiState.archiveErrorMessage,
                            refreshError = uiState.refreshErrorMessage,
                            onEdit = onEdit,
                            onArchive = viewModel::showArchiveDialog
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingDetail() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(text = "Loading contact...")
        }
    }
}

@Composable
private fun DetailError(
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
private fun ContactDetailContent(
    contact: CrmContact,
    tenantName: String,
    archiveError: String?,
    refreshError: String?,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = contact.displayName(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "This contact belongs only to $tenantName.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(text = contact.lifecycleStage.humanize()) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(text = contact.leadStatus.humanize()) }
                )
            }
            if (archiveError != null) {
                Text(
                    text = archiveError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (refreshError != null) {
                Text(
                    text = "Showing saved contact. Refresh failed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onEdit) {
                    Text(text = "Edit")
                }
                OutlinedButton(onClick = onArchive) {
                    Text(text = "Archive")
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ContactDetailRow(label = "Email", value = contact.email)
            ContactDetailRow(label = "Phone", value = contact.phone)
            ContactDetailRow(label = "Company", value = contact.companyName)
            ContactDetailRow(label = "Job title", value = contact.jobTitle)
            ContactDetailRow(label = "Source", value = contact.source)
            ContactDetailRow(label = "Notes", value = contact.notes)
            ContactDetailRow(label = "Created", value = contact.createdAt)
            ContactDetailRow(label = "Updated", value = contact.updatedAt)
        }
    }
}

@Composable
private fun ContactDetailRow(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "Not set",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun CrmContact.displayName(): String {
    return listOf(firstName, lastName.orEmpty()).joinToString(" ").trim()
}

internal fun String.humanize(): String {
    return replace("_", " ").replaceFirstChar { it.uppercase() }
}

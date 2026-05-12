package com.nexora.android.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    tenantId: String,
    contactId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditContactViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(tenantId, contactId) {
        viewModel.load(tenantId, contactId)
    }

    LaunchedEffect(uiState.savedContact?.id) {
        if (uiState.savedContact != null) {
            viewModel.consumeSavedContact()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                    Text(text = "Loading contact...")
                } else {
                    ContactFormFields(
                        uiState = uiState,
                        viewModel = viewModel,
                        isEnabled = !uiState.isSaving
                    )
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = { viewModel.save(tenantId, contactId) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 10.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(text = if (uiState.isSaving) "Saving..." else "Save contact")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactFormFields(
    uiState: EditContactUiState,
    viewModel: EditContactViewModel,
    isEnabled: Boolean
) {
    OutlinedTextField(
        value = uiState.firstName,
        onValueChange = viewModel::onFirstNameChanged,
        label = { Text(text = "First name") },
        singleLine = true,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.lastName,
        onValueChange = viewModel::onLastNameChanged,
        label = { Text(text = "Last name (optional)") },
        singleLine = true,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.email,
        onValueChange = viewModel::onEmailChanged,
        label = { Text(text = "Email (optional)") },
        singleLine = true,
        enabled = isEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.phone,
        onValueChange = viewModel::onPhoneChanged,
        label = { Text(text = "Phone (optional)") },
        singleLine = true,
        enabled = isEnabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.companyName,
        onValueChange = viewModel::onCompanyNameChanged,
        label = { Text(text = "Company name (optional)") },
        singleLine = true,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.jobTitle,
        onValueChange = viewModel::onJobTitleChanged,
        label = { Text(text = "Job title (optional)") },
        singleLine = true,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth()
    )
    ChoiceGroup(
        title = "Lifecycle stage",
        selected = uiState.lifecycleStage,
        values = ContactInputRules.LIFECYCLE_STAGES,
        onSelected = viewModel::onLifecycleStageChanged,
        enabled = isEnabled
    )
    ChoiceGroup(
        title = "Lead status",
        selected = uiState.leadStatus,
        values = ContactInputRules.LEAD_STATUSES,
        onSelected = viewModel::onLeadStatusChanged,
        enabled = isEnabled
    )
    OutlinedTextField(
        value = uiState.source,
        onValueChange = viewModel::onSourceChanged,
        label = { Text(text = "Source (optional)") },
        singleLine = true,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = uiState.notes,
        onValueChange = viewModel::onNotesChanged,
        label = { Text(text = "Notes (optional)") },
        enabled = isEnabled,
        minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChoiceGroup(
    title: String,
    selected: String,
    values: List<String>,
    onSelected: (String) -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value ->
                AssistChip(
                    onClick = { if (enabled) onSelected(value) },
                    label = { Text(text = if (selected == value) "${value.humanize()} selected" else value.humanize()) },
                    enabled = enabled
                )
            }
        }
    }
}

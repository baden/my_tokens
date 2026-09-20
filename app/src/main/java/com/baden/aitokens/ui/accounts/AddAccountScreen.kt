package com.baden.aitokens.ui.accounts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baden.aitokens.data.model.Provider
import com.baden.aitokens.data.remote.provider.CopilotPlans

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AccountsViewModel = viewModel(),
) {
    var provider by remember { mutableStateOf(Provider.DEEPSEEK) }
    var label by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("global") }
    var plan by remember { mutableStateOf("pro+") }
    var showCredential by remember { mutableStateOf(false) }

    val canSave = credential.isNotBlank() && (!provider.needsUsername || username.isNotBlank())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новий акаунт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Провайдер", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Provider.entries.forEach { candidate ->
                    FilterChip(
                        selected = provider == candidate,
                        onClick = { provider = candidate },
                        label = { Text(candidate.displayName) },
                    )
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Назва (необов'язково)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (provider.needsUsername) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("GitHub username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (provider.needsRegion) {
                Text("Регіон", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = region == "global",
                        onClick = { region = "global" },
                        label = { Text("Global (.io)") },
                    )
                    FilterChip(
                        selected = region == "cn",
                        onClick = { region = "cn" },
                        label = { Text("China") },
                    )
                }
            }

            if (provider.needsPlan) {
                Text("План Copilot", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CopilotPlans.ORDER.forEach { candidate ->
                        FilterChip(
                            selected = plan == candidate,
                            onClick = { plan = candidate },
                            label = { Text(CopilotPlans.display(candidate)) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = credential,
                onValueChange = { credential = it },
                label = { Text(provider.credentialLabel) },
                placeholder = { Text(provider.credentialHint) },
                singleLine = true,
                visualTransformation = if (showCredential) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showCredential = !showCredential }) {
                        Icon(
                            imageVector = if (showCredential) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showCredential) "Сховати" else "Показати",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (provider == Provider.COPILOT) {
                Text(
                    text = "Потрібен fine-grained PAT із правом Account permissions → Plan: Read-only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    viewModel.add(
                        provider = provider,
                        label = label,
                        credential = credential,
                        username = username.takeIf { provider.needsUsername },
                        region = region.takeIf { provider.needsRegion },
                        plan = plan.takeIf { provider.needsPlan },
                    )
                    onSaved()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Зберегти")
            }
        }
    }
}

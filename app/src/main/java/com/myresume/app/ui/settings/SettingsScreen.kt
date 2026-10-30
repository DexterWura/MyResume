package com.myresume.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myresume.app.AppContainer
import com.myresume.app.BuildConfig
import com.myresume.app.R
import com.myresume.app.billing.Entitlements
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onPaywall: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entitlements by container.entitlementStore.entitlements
        .collectAsStateWithLifecycle(initialValue = Entitlements())
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium)
            Text(
                "Resume content stays in this app’s private storage on your phone. " +
                    "The app does not create an account. Network access is used for Google Play Billing " +
                    "and, if you unlock AI, for downloading the on-device model file."
            )
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open privacy policy")
            }

            Text(stringResource(R.string.ai_disclosure_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.ai_disclosure_body))

            Text("Purchases", style = MaterialTheme.typography.titleMedium)
            Text(
                "Premium templates: ${if (entitlements.premiumTemplates) "unlocked" else "locked"}\n" +
                    "AI assistant: ${if (entitlements.aiAssistant) "unlocked" else "locked"}"
            )
            Button(onClick = onPaywall, modifier = Modifier.fillMaxWidth()) {
                Text("Manage unlocks")
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            container.billing.refreshPurchases()
                            message = "Purchases refreshed from Google Play"
                        }.onFailure {
                            message = it.message ?: "Restore failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore purchases")
            }

            Text("Data on this device", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    scope.launch {
                        container.resumeRepository.deleteAll()
                        message = "All resumes deleted"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete all resumes")
            }
            OutlinedButton(
                onClick = {
                    container.modelDownloader.deleteModel()
                    container.resumeAssistant.close()
                    message = "Downloaded AI model deleted"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete downloaded AI model")
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

package com.myresume.app.ui.paywall

import android.app.Activity
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.myresume.app.R
import com.myresume.app.billing.Entitlements
import com.myresume.app.billing.PlayProducts
import com.myresume.app.billing.ProductDetailsUi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    container: AppContainer,
    highlightProduct: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val entitlements by container.entitlementStore.entitlements
        .collectAsStateWithLifecycle(initialValue = Entitlements())
    var products by remember { mutableStateOf<List<ProductDetailsUi>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            container.billing.start()
            products = container.billing.queryProducts()
            container.billing.refreshPurchases()
        }.onFailure {
            message = it.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock features") },
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
            Text(stringResource(R.string.free_features), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.paid_features), style = MaterialTheme.typography.bodyMedium)

            ProductCard(
                title = "Premium templates",
                description = "Unlock the full catalog of resume designs.",
                price = products.find { it.productId == PlayProducts.PREMIUM_TEMPLATES }?.formattedPrice
                    ?: "See price in Google Play",
                owned = entitlements.premiumTemplates,
                highlighted = highlightProduct == PlayProducts.PREMIUM_TEMPLATES,
                onBuy = {
                    if (activity == null) {
                        message = "Purchase requires an Activity"
                    } else {
                        scope.launch {
                            container.billing.queryProducts()
                            container.billing.launchPurchase(activity, PlayProducts.PREMIUM_TEMPLATES)
                        }
                    }
                }
            )

            ProductCard(
                title = "AI assistant",
                description = "On-device help for summaries, bullets, and job tailoring. Requires a one-time model download after purchase.",
                price = products.find { it.productId == PlayProducts.AI_ASSISTANT }?.formattedPrice
                    ?: "See price in Google Play",
                owned = entitlements.aiAssistant,
                highlighted = highlightProduct == PlayProducts.AI_ASSISTANT,
                onBuy = {
                    if (activity == null) {
                        message = "Purchase requires an Activity"
                    } else {
                        scope.launch {
                            container.billing.queryProducts()
                            container.billing.launchPurchase(activity, PlayProducts.AI_ASSISTANT)
                        }
                    }
                }
            )

            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            container.billing.refreshPurchases()
                            message = "Purchases restored from Google Play"
                        }.onFailure {
                            message = it.message ?: "Restore failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore purchases")
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun ProductCard(
    title: String,
    description: String,
    price: String,
    owned: Boolean,
    highlighted: Boolean,
    onBuy: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (highlighted) {
                Text("Suggested for your next step", color = MaterialTheme.colorScheme.primary)
            }
            Text(description, style = MaterialTheme.typography.bodyMedium)
            Text(if (owned) "Owned" else price, style = MaterialTheme.typography.titleSmall)
            Button(
                onClick = onBuy,
                enabled = !owned,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (owned) "Unlocked" else "Buy with Google Play")
            }
        }
    }
}

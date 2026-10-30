package com.myresume.app.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Debug-only purchase simulator so UI and unlocks can be tested without Play Console products.
 */
class DebugBillingGateway(
    context: Context,
    private val entitlementStore: EntitlementStore
) : BillingGateway {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("debug_billing") }
    )
    private val premiumKey = booleanPreferencesKey(PlayProducts.PREMIUM_TEMPLATES)
    private val aiKey = booleanPreferencesKey(PlayProducts.AI_ASSISTANT)

    private val _connectionReady = MutableStateFlow(true)
    override val connectionReady: StateFlow<Boolean> = _connectionReady.asStateFlow()

    override suspend fun start() {
        refreshPurchases()
    }

    override fun end() = Unit

    override suspend fun queryProducts(): List<ProductDetailsUi> = listOf(
        ProductDetailsUi(
            productId = PlayProducts.PREMIUM_TEMPLATES,
            title = "Premium templates (debug)",
            description = "Unlock the full template catalog",
            formattedPrice = "$4.99"
        ),
        ProductDetailsUi(
            productId = PlayProducts.AI_ASSISTANT,
            title = "AI assistant (debug)",
            description = "Unlock on-device resume AI",
            formattedPrice = "$6.99"
        )
    )

    override suspend fun refreshPurchases(): Set<String> {
        val prefs = dataStore.data.first()
        val owned = buildSet {
            if (prefs[premiumKey] == true) add(PlayProducts.PREMIUM_TEMPLATES)
            if (prefs[aiKey] == true) add(PlayProducts.AI_ASSISTANT)
        }
        entitlementStore.applyOwnedProducts(owned)
        return owned
    }

    override fun launchPurchase(activity: Activity, productId: String) {
        // Fire-and-forget coroutine not available here; use blocking edit via runBlocking-free path:
        // Activity launches purchase; we update asynchronously from a thread.
        Thread {
            kotlinx.coroutines.runBlocking {
                dataStore.edit { prefs ->
                    when (productId) {
                        PlayProducts.PREMIUM_TEMPLATES -> prefs[premiumKey] = true
                        PlayProducts.AI_ASSISTANT -> prefs[aiKey] = true
                    }
                }
                refreshPurchases()
            }
        }.start()
    }

    suspend fun revokeAll() {
        dataStore.edit {
            it[premiumKey] = false
            it[aiKey] = false
        }
        refreshPurchases()
    }
}

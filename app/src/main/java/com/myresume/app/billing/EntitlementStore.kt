package com.myresume.app.billing

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Entitlements(
    val premiumTemplates: Boolean = false,
    val aiAssistant: Boolean = false
)

class EntitlementStore(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("entitlements") }
    )

    private val premiumKey = booleanPreferencesKey("premium_templates")
    private val aiKey = booleanPreferencesKey("ai_assistant")

    val entitlements: Flow<Entitlements> = dataStore.data.map { prefs ->
        Entitlements(
            premiumTemplates = prefs[premiumKey] == true,
            aiAssistant = prefs[aiKey] == true
        )
    }

    suspend fun setPremiumTemplates(owned: Boolean) {
        dataStore.edit { it[premiumKey] = owned }
    }

    suspend fun setAiAssistant(owned: Boolean) {
        dataStore.edit { it[aiKey] = owned }
    }

    suspend fun applyOwnedProducts(ownedProductIds: Set<String>) {
        dataStore.edit { prefs ->
            prefs[premiumKey] = PlayProducts.PREMIUM_TEMPLATES in ownedProductIds
            prefs[aiKey] = PlayProducts.AI_ASSISTANT in ownedProductIds
        }
    }
}

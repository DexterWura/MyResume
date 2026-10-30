package com.myresume.app.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

data class ProductDetailsUi(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String
)

interface BillingGateway {
    val connectionReady: StateFlow<Boolean>
    suspend fun start()
    fun end()
    suspend fun queryProducts(): List<ProductDetailsUi>
    suspend fun refreshPurchases(): Set<String>
    fun launchPurchase(activity: Activity, productId: String)
}

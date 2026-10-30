package com.myresume.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlayBillingManager(
    context: Context,
    private val entitlementStore: EntitlementStore,
    private val onPurchasesChanged: (Set<String>) -> Unit = {}
) : BillingGateway, PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _connectionReady = MutableStateFlow(false)
    override val connectionReady: StateFlow<Boolean> = _connectionReady.asStateFlow()

    private var productDetails: Map<String, ProductDetails> = emptyMap()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    override suspend fun start() {
        if (billingClient.isReady) {
            _connectionReady.value = true
            refreshPurchases()
            return
        }
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    _connectionReady.value = result.responseCode == BillingClient.BillingResponseCode.OK
                    if (cont.isActive) cont.resume(Unit)
                    if (_connectionReady.value) {
                        scope.launch { refreshPurchases() }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _connectionReady.value = false
                }
            })
        }
    }

    override fun end() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        _connectionReady.value = false
    }

    override suspend fun queryProducts(): List<ProductDetailsUi> {
        if (!billingClient.isReady) return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                PlayProducts.all.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()
        val detailsResult: ProductDetailsResult = billingClient.queryProductDetails(params)
        if (detailsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return emptyList()
        }
        val list = detailsResult.productDetailsList.orEmpty()
        productDetails = list.associateBy { it.productId }
        return list.map { details ->
            val offer = details.oneTimePurchaseOfferDetails
            ProductDetailsUi(
                productId = details.productId,
                title = details.title,
                description = details.description,
                formattedPrice = offer?.formattedPrice.orEmpty()
            )
        }
    }

    override suspend fun refreshPurchases(): Set<String> {
        if (!billingClient.isReady) return emptySet()
        val purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val purchases = if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchasesResult.purchasesList
        } else {
            emptyList()
        }
        return processPurchases(purchases)
    }

    override fun launchPurchase(activity: Activity, productId: String) {
        val details = productDetails[productId] ?: return
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { processPurchases(purchases) }
        }
    }

    private suspend fun processPurchases(purchases: List<Purchase>): Set<String> {
        val owned = linkedSetOf<String>()
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { purchase ->
            owned.addAll(purchase.products)
            if (!purchase.isAcknowledged) {
                val ack = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ack)
            }
        }
        entitlementStore.applyOwnedProducts(owned)
        onPurchasesChanged(owned)
        return owned
    }
}

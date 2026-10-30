package com.myresume.app.billing

import android.content.Context

object BillingFactory {
    fun create(context: Context, entitlementStore: EntitlementStore): BillingGateway =
        DebugBillingGateway(context, entitlementStore)
}

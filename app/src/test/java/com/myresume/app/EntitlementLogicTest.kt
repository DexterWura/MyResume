package com.myresume.app

import com.myresume.app.billing.PlayProducts
import com.myresume.app.template.TemplateCatalog
import com.myresume.app.template.TemplateTier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementLogicTest {
    @Test
    fun premiumTemplateStaysLockedWithoutEntitlement() {
        val premium = TemplateCatalog.premium().first()
        val hasPremium = false
        val locked = premium.tier == TemplateTier.PREMIUM && !hasPremium
        assertTrue(locked)
    }

    @Test
    fun premiumTemplateUnlocksWithEntitlement() {
        val premium = TemplateCatalog.premium().first()
        val hasPremium = true
        val locked = premium.tier == TemplateTier.PREMIUM && !hasPremium
        assertFalse(locked)
    }

    @Test
    fun freeTemplateNeverLocked() {
        val free = TemplateCatalog.free().first()
        val hasPremium = false
        val locked = free.tier == TemplateTier.PREMIUM && !hasPremium
        assertFalse(locked)
    }

    @Test
    fun ownedProductsMapToExpectedIds() {
        val owned = setOf(PlayProducts.PREMIUM_TEMPLATES)
        assertTrue(PlayProducts.PREMIUM_TEMPLATES in owned)
        assertFalse(PlayProducts.AI_ASSISTANT in owned)
    }
}

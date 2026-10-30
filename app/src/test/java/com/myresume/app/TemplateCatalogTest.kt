package com.myresume.app

import com.myresume.app.template.TemplateCatalog
import com.myresume.app.template.TemplateTier
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateCatalogTest {
    @Test
    fun catalogHasAtLeast200Presets() {
        assertTrue(TemplateCatalog.all.size >= 200)
    }

    @Test
    fun catalogHasFreeSubset() {
        val free = TemplateCatalog.free()
        assertTrue(free.size >= 35)
        assertTrue(free.all { it.tier == TemplateTier.FREE })
    }

    @Test
    fun classicAtsNavyIsFree() {
        val preset = TemplateCatalog.byId("classic_ats_navy")
        assertTrue(preset.tier == TemplateTier.FREE)
        assertTrue(preset.id == "classic_ats_navy")
    }

    @Test
    fun premiumExistsAndIsLockedConceptually() {
        val premium = TemplateCatalog.premium()
        assertTrue(premium.isNotEmpty())
        assertTrue(premium.all { it.tier == TemplateTier.PREMIUM })
    }
}

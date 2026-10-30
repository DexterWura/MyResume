package com.myresume.app.template

object TemplateCatalog {
    /** ARGB int from #RRGGBB — JVM-safe (no android.graphics.Color). */
    private fun rgb(hex: String): Int {
        val cleaned = hex.removePrefix("#")
        require(cleaned.length == 6) { "Expected #RRGGBB, got $hex" }
        return (0xFF shl 24) or cleaned.toInt(16)
    }

    private val WHITE = 0xFFFFFFFF.toInt()

    private val palettes = listOf(
        "navy" to TemplatePalette(rgb("#1B4F72"), rgb("#2874A6"), rgb("#1C2833"), rgb("#5D6D7E"), WHITE, rgb("#EBF5FB")),
        "charcoal" to TemplatePalette(rgb("#212F3D"), rgb("#566573"), rgb("#1C2833"), rgb("#7F8C8D"), WHITE, rgb("#F4F6F7")),
        "teal" to TemplatePalette(rgb("#0E6655"), rgb("#148F77"), rgb("#1C2833"), rgb("#5D6D7E"), WHITE, rgb("#E8F8F5")),
        "burgundy" to TemplatePalette(rgb("#7B241C"), rgb("#A93226"), rgb("#1C2833"), rgb("#7F8C8D"), WHITE, rgb("#F9EBEA")),
        "forest" to TemplatePalette(rgb("#145A32"), rgb("#1E8449"), rgb("#1C2833"), rgb("#5D6D7E"), WHITE, rgb("#E9F7EF")),
        "indigo" to TemplatePalette(rgb("#1A237E"), rgb("#3949AB"), rgb("#1C2833"), rgb("#5D6D7E"), WHITE, rgb("#E8EAF6")),
        "slate" to TemplatePalette(rgb("#34495E"), rgb("#5D6D7E"), rgb("#1C2833"), rgb("#7F8C8D"), WHITE, rgb("#F2F3F4")),
        "copper" to TemplatePalette(rgb("#A04000"), rgb("#D35400"), rgb("#1C2833"), rgb("#7F8C8D"), WHITE, rgb("#FDF2E9")),
        "ocean" to TemplatePalette(rgb("#154360"), rgb("#1A5276"), rgb("#1C2833"), rgb("#5D6D7E"), WHITE, rgb("#EAF2F8")),
        "plum" to TemplatePalette(rgb("#4A235A"), rgb("#6C3483"), rgb("#1C2833"), rgb("#7F8C8D"), WHITE, rgb("#F5EEF8")),
        "graphite" to TemplatePalette(rgb("#2C3E50"), rgb("#7F8C8D"), rgb("#1C2833"), rgb("#95A5A6"), WHITE, rgb("#ECF0F1")),
        "emerald" to TemplatePalette(rgb("#0B5345"), rgb("#117A65"), rgb("#1C2833"), rgb("#5D6D7E"), WHITE, rgb("#E8F6F3")),
        "crimson" to TemplatePalette(rgb("#922B21"), rgb("#C0392B"), rgb("#1C2833"), rgb("#7F8C8D"), WHITE, rgb("#FDEDEC")),
        "sky" to TemplatePalette(rgb("#1A5276"), rgb("#2980B9"), rgb("#1C2833"), rgb("#5D6D7E"), WHITE, rgb("#EBF5FB")),
        "olive" to TemplatePalette(rgb("#7D6608"), rgb("#B7950B"), rgb("#1C2833"), rgb("#7F8C8D"), WHITE, rgb("#FEF9E7")),
        "midnight" to TemplatePalette(rgb("#0B1C2C"), rgb("#1F618D"), rgb("#1C2833"), rgb("#5D6D7E"), WHITE, rgb("#EBF5FB"))
    )

    private val layoutNames = mapOf(
        LayoutClass.SINGLE_COLUMN to "Classic",
        LayoutClass.LEFT_SIDEBAR to "Sidebar Left",
        LayoutClass.RIGHT_SIDEBAR to "Sidebar Right",
        LayoutClass.BAND_HEADER to "Banner",
        LayoutClass.TIMELINE to "Timeline",
        LayoutClass.COMPACT_TWO_COLUMN to "Compact"
    )

    private val freeIds = setOf(
        "classic_ats_navy",
        "classic_ats_charcoal",
        "classic_ats_slate",
        "classic_ats_graphite",
        "modern_single_teal",
        "modern_single_indigo",
        "banner_navy",
        "banner_charcoal",
        "sidebar_left_navy",
        "sidebar_left_teal",
        "sidebar_right_slate",
        "sidebar_right_indigo",
        "timeline_navy",
        "timeline_forest",
        "compact_charcoal",
        "compact_ocean",
        "serif_single_navy",
        "serif_single_burgundy",
        "technical_single_graphite",
        "technical_compact_slate",
        "classic_ats_teal",
        "classic_ats_ocean",
        "modern_single_sky",
        "modern_single_emerald",
        "banner_teal",
        "banner_indigo",
        "sidebar_left_charcoal",
        "sidebar_left_ocean",
        "sidebar_right_navy",
        "sidebar_right_forest",
        "timeline_slate",
        "timeline_teal",
        "compact_navy",
        "compact_indigo",
        "serif_banner_charcoal",
        "serif_sidebar_left_navy",
        "technical_timeline_graphite",
        "technical_sidebar_right_slate",
        "classic_photo_navy",
        "modern_photo_teal"
    )

    val all: List<TemplatePreset> by lazy { buildCatalog() }

    fun byId(id: String): TemplatePreset =
        all.firstOrNull { it.id == id } ?: all.first()

    fun free(): List<TemplatePreset> = all.filter { it.tier == TemplateTier.FREE }

    fun premium(): List<TemplatePreset> = all.filter { it.tier == TemplateTier.PREMIUM }

    private fun buildCatalog(): List<TemplatePreset> {
        val presets = linkedMapOf<String, TemplatePreset>()

        fun put(
            id: String,
            name: String,
            layout: LayoutClass,
            paletteKey: String,
            typeStyle: TypeStyle,
            density: Density,
            showPhoto: Boolean,
            tags: List<String>
        ) {
            if (presets.containsKey(id)) return
            val palette = palettes.first { it.first == paletteKey }.second
            presets[id] = TemplatePreset(
                id = id,
                name = name,
                layout = layout,
                palette = palette,
                typeStyle = typeStyle,
                density = density,
                showPhoto = showPhoto,
                tier = if (id in freeIds) TemplateTier.FREE else TemplateTier.PREMIUM,
                tags = tags
            )
        }

        // Seed explicit free / named presets first
        put("classic_ats_navy", "ATS Navy", LayoutClass.SINGLE_COLUMN, "navy", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("ats", "free"))
        put("classic_ats_charcoal", "ATS Charcoal", LayoutClass.SINGLE_COLUMN, "charcoal", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("ats"))
        put("classic_ats_slate", "ATS Slate", LayoutClass.SINGLE_COLUMN, "slate", TypeStyle.CLASSIC, Density.COMPACT, false, listOf("ats"))
        put("classic_ats_graphite", "ATS Graphite", LayoutClass.SINGLE_COLUMN, "graphite", TypeStyle.TECHNICAL, Density.COMPACT, false, listOf("ats"))
        put("classic_ats_teal", "ATS Teal", LayoutClass.SINGLE_COLUMN, "teal", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("ats"))
        put("classic_ats_ocean", "ATS Ocean", LayoutClass.SINGLE_COLUMN, "ocean", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("ats"))
        put("classic_photo_navy", "Classic Navy Photo", LayoutClass.SINGLE_COLUMN, "navy", TypeStyle.CLASSIC, Density.NORMAL, true, listOf("photo"))
        put("modern_photo_teal", "Modern Teal Photo", LayoutClass.SINGLE_COLUMN, "teal", TypeStyle.MODERN, Density.NORMAL, true, listOf("photo"))

        // Combinatorial generation with unique ids
        // 6 layouts × 16 palettes × 4 type styles × 3 densities = 1152 candidates; take photo variants sparingly
        for (layout in LayoutClass.entries) {
            for ((paletteKey, _) in palettes) {
                for (typeStyle in TypeStyle.entries) {
                    for (density in Density.entries) {
                        val id = "${layout.name.lowercase()}_${typeStyle.name.lowercase()}_${density.name.lowercase()}_$paletteKey"
                        val name = buildString {
                            append(layoutNames.getValue(layout))
                            append(' ')
                            append(paletteKey.replaceFirstChar { it.uppercase() })
                            append(' ')
                            append(typeStyle.name.lowercase().replaceFirstChar { it.uppercase() })
                            if (density != Density.NORMAL) {
                                append(' ')
                                append(density.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                        put(
                            id = id,
                            name = name,
                            layout = layout,
                            paletteKey = paletteKey,
                            typeStyle = typeStyle,
                            density = density,
                            showPhoto = false,
                            tags = listOf(layout.name.lowercase(), paletteKey, typeStyle.name.lowercase())
                        )
                    }
                }
            }
        }

        // Photo variants for single-column / sidebar layouts
        for (layout in listOf(LayoutClass.SINGLE_COLUMN, LayoutClass.LEFT_SIDEBAR, LayoutClass.BAND_HEADER)) {
            for ((paletteKey, _) in palettes) {
                for (typeStyle in listOf(TypeStyle.CLASSIC, TypeStyle.MODERN)) {
                    val id = "${layout.name.lowercase()}_${typeStyle.name.lowercase()}_${paletteKey}_photo"
                    put(
                        id = id,
                        name = "${layoutNames.getValue(layout)} ${paletteKey.replaceFirstChar { it.uppercase() }} Photo",
                        layout = layout,
                        paletteKey = paletteKey,
                        typeStyle = typeStyle,
                        density = Density.NORMAL,
                        showPhoto = true,
                        tags = listOf("photo", paletteKey)
                    )
                }
            }
        }

        // Friendly aliases used by freeIds that may not match combinatorial ids
        put("modern_single_teal", "Modern Teal", LayoutClass.SINGLE_COLUMN, "teal", TypeStyle.MODERN, Density.NORMAL, false, listOf("modern"))
        put("modern_single_indigo", "Modern Indigo", LayoutClass.SINGLE_COLUMN, "indigo", TypeStyle.MODERN, Density.NORMAL, false, listOf("modern"))
        put("modern_single_sky", "Modern Sky", LayoutClass.SINGLE_COLUMN, "sky", TypeStyle.MODERN, Density.NORMAL, false, listOf("modern"))
        put("modern_single_emerald", "Modern Emerald", LayoutClass.SINGLE_COLUMN, "emerald", TypeStyle.MODERN, Density.NORMAL, false, listOf("modern"))
        put("banner_navy", "Banner Navy", LayoutClass.BAND_HEADER, "navy", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("banner"))
        put("banner_charcoal", "Banner Charcoal", LayoutClass.BAND_HEADER, "charcoal", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("banner"))
        put("banner_teal", "Banner Teal", LayoutClass.BAND_HEADER, "teal", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("banner"))
        put("banner_indigo", "Banner Indigo", LayoutClass.BAND_HEADER, "indigo", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("banner"))
        put("sidebar_left_navy", "Sidebar Left Navy", LayoutClass.LEFT_SIDEBAR, "navy", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("sidebar"))
        put("sidebar_left_teal", "Sidebar Left Teal", LayoutClass.LEFT_SIDEBAR, "teal", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("sidebar"))
        put("sidebar_left_charcoal", "Sidebar Left Charcoal", LayoutClass.LEFT_SIDEBAR, "charcoal", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("sidebar"))
        put("sidebar_left_ocean", "Sidebar Left Ocean", LayoutClass.LEFT_SIDEBAR, "ocean", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("sidebar"))
        put("sidebar_right_slate", "Sidebar Right Slate", LayoutClass.RIGHT_SIDEBAR, "slate", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("sidebar"))
        put("sidebar_right_indigo", "Sidebar Right Indigo", LayoutClass.RIGHT_SIDEBAR, "indigo", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("sidebar"))
        put("sidebar_right_navy", "Sidebar Right Navy", LayoutClass.RIGHT_SIDEBAR, "navy", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("sidebar"))
        put("sidebar_right_forest", "Sidebar Right Forest", LayoutClass.RIGHT_SIDEBAR, "forest", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("sidebar"))
        put("timeline_navy", "Timeline Navy", LayoutClass.TIMELINE, "navy", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("timeline"))
        put("timeline_forest", "Timeline Forest", LayoutClass.TIMELINE, "forest", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("timeline"))
        put("timeline_slate", "Timeline Slate", LayoutClass.TIMELINE, "slate", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("timeline"))
        put("timeline_teal", "Timeline Teal", LayoutClass.TIMELINE, "teal", TypeStyle.CLASSIC, Density.NORMAL, false, listOf("timeline"))
        put("compact_charcoal", "Compact Charcoal", LayoutClass.COMPACT_TWO_COLUMN, "charcoal", TypeStyle.CLASSIC, Density.COMPACT, false, listOf("compact"))
        put("compact_ocean", "Compact Ocean", LayoutClass.COMPACT_TWO_COLUMN, "ocean", TypeStyle.CLASSIC, Density.COMPACT, false, listOf("compact"))
        put("compact_navy", "Compact Navy", LayoutClass.COMPACT_TWO_COLUMN, "navy", TypeStyle.CLASSIC, Density.COMPACT, false, listOf("compact"))
        put("compact_indigo", "Compact Indigo", LayoutClass.COMPACT_TWO_COLUMN, "indigo", TypeStyle.CLASSIC, Density.COMPACT, false, listOf("compact"))
        put("serif_single_navy", "Serif Navy", LayoutClass.SINGLE_COLUMN, "navy", TypeStyle.SERIF, Density.NORMAL, false, listOf("serif"))
        put("serif_single_burgundy", "Serif Burgundy", LayoutClass.SINGLE_COLUMN, "burgundy", TypeStyle.SERIF, Density.NORMAL, false, listOf("serif"))
        put("serif_banner_charcoal", "Serif Banner Charcoal", LayoutClass.BAND_HEADER, "charcoal", TypeStyle.SERIF, Density.NORMAL, false, listOf("serif"))
        put("serif_sidebar_left_navy", "Serif Sidebar Navy", LayoutClass.LEFT_SIDEBAR, "navy", TypeStyle.SERIF, Density.NORMAL, false, listOf("serif"))
        put("technical_single_graphite", "Technical Graphite", LayoutClass.SINGLE_COLUMN, "graphite", TypeStyle.TECHNICAL, Density.COMPACT, false, listOf("technical"))
        put("technical_compact_slate", "Technical Compact Slate", LayoutClass.COMPACT_TWO_COLUMN, "slate", TypeStyle.TECHNICAL, Density.COMPACT, false, listOf("technical"))
        put("technical_timeline_graphite", "Technical Timeline Graphite", LayoutClass.TIMELINE, "graphite", TypeStyle.TECHNICAL, Density.COMPACT, false, listOf("technical"))
        put("technical_sidebar_right_slate", "Technical Sidebar Slate", LayoutClass.RIGHT_SIDEBAR, "slate", TypeStyle.TECHNICAL, Density.COMPACT, false, listOf("technical"))

        require(presets.size >= 200) { "Expected at least 200 templates, got ${presets.size}" }
        require(presets.values.count { it.tier == TemplateTier.FREE } >= 35) {
            "Expected at least 35 free templates, got ${presets.values.count { it.tier == TemplateTier.FREE }}"
        }
        return presets.values.toList()
    }
}

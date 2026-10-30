package com.myresume.app.template

enum class LayoutClass {
    SINGLE_COLUMN,
    LEFT_SIDEBAR,
    RIGHT_SIDEBAR,
    BAND_HEADER,
    TIMELINE,
    COMPACT_TWO_COLUMN
}

enum class TemplateTier {
    FREE,
    PREMIUM
}

enum class Density {
    COMPACT,
    NORMAL,
    SPACIOUS
}

enum class TypeStyle {
    CLASSIC,
    MODERN,
    SERIF,
    TECHNICAL
}

data class TemplatePalette(
    val primary: Int,
    val accent: Int,
    val text: Int,
    val muted: Int,
    val background: Int,
    val sidebar: Int
)

data class TemplatePreset(
    val id: String,
    val name: String,
    val layout: LayoutClass,
    val palette: TemplatePalette,
    val typeStyle: TypeStyle,
    val density: Density,
    val showPhoto: Boolean,
    val tier: TemplateTier,
    val tags: List<String> = emptyList()
)

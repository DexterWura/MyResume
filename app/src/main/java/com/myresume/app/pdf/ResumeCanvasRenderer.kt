package com.myresume.app.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.myresume.app.data.ContactInfo
import com.myresume.app.data.ResumeEntity
import com.myresume.app.data.ResumeSection
import com.myresume.app.data.SectionType
import com.myresume.app.template.Density
import com.myresume.app.template.LayoutClass
import com.myresume.app.template.TemplatePreset
import com.myresume.app.template.TypeStyle
import kotlin.math.max
import kotlin.math.min

/**
 * Draws resume pages onto any Canvas (preview or PdfDocument).
 * Page size is A4 at 72 dpi points: 595 x 842.
 */
class ResumeCanvasRenderer {
    companion object {
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
    }

    fun pageCount(resume: ResumeEntity, template: TemplatePreset): Int {
        val measure = MeasureCanvas()
        return draw(measure, resume, template, pageIndex = -1)
    }

    fun drawPage(canvas: Canvas, resume: ResumeEntity, template: TemplatePreset, pageIndex: Int) {
        draw(canvas, resume, template, pageIndex)
    }

    fun renderThumbnail(resume: ResumeEntity, template: TemplatePreset, widthPx: Int): Bitmap {
        val scale = widthPx / PAGE_WIDTH
        val heightPx = (PAGE_HEIGHT * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        draw(canvas, resume, template, pageIndex = 0)
        return bitmap
    }

    private fun draw(canvas: Canvas, resume: ResumeEntity, template: TemplatePreset, pageIndex: Int): Int {
        val ctx = DrawContext(canvas, template, pageIndex)
        ctx.fillBackground()
        when (template.layout) {
            LayoutClass.LEFT_SIDEBAR -> drawSidebar(ctx, resume, left = true)
            LayoutClass.RIGHT_SIDEBAR -> drawSidebar(ctx, resume, left = false)
            LayoutClass.BAND_HEADER -> drawBand(ctx, resume)
            LayoutClass.TIMELINE -> drawTimeline(ctx, resume)
            LayoutClass.COMPACT_TWO_COLUMN -> drawCompact(ctx, resume)
            LayoutClass.SINGLE_COLUMN -> drawSingle(ctx, resume)
        }
        return ctx.totalPages
    }

    private fun drawSingle(ctx: DrawContext, resume: ResumeEntity) {
        ctx.y = ctx.margin
        drawHeader(ctx, resume, fullWidth = true)
        drawSummary(ctx, resume.summary)
        visibleSections(resume).forEach { drawSection(ctx, it) }
    }

    private fun drawBand(ctx: DrawContext, resume: ResumeEntity) {
        val bandHeight = 96f
        ctx.paint.color = ctx.template.palette.primary
        ctx.canvas.drawRect(0f, 0f, PAGE_WIDTH, bandHeight, ctx.paint)
        ctx.y = 24f
        ctx.textPaint.color = android.graphics.Color.WHITE
        ctx.titlePaint.color = android.graphics.Color.WHITE
        ctx.mutedPaint.color = android.graphics.Color.argb(200, 255, 255, 255)
        drawNameAndContact(ctx, resume.contact, resume.targetJobTitle, inset = ctx.margin)
        ctx.resetTextColors()
        ctx.y = bandHeight + 16f
        drawSummary(ctx, resume.summary)
        visibleSections(resume).forEach { drawSection(ctx, it) }
    }

    private fun drawSidebar(ctx: DrawContext, resume: ResumeEntity, left: Boolean) {
        val sidebarWidth = PAGE_WIDTH * 0.32f
        val contentLeft = if (left) sidebarWidth + 16f else ctx.margin
        val contentWidth = PAGE_WIDTH - sidebarWidth - ctx.margin - 16f
        val sidebarLeft = if (left) 0f else PAGE_WIDTH - sidebarWidth

        ctx.paint.color = ctx.template.palette.sidebar
        ctx.canvas.drawRect(sidebarLeft, 0f, sidebarLeft + sidebarWidth, PAGE_HEIGHT, ctx.paint)

        val photo = loadPhoto(resume.photoPath, ctx.template.showPhoto)
        var sideY = ctx.margin
        if (photo != null) {
            val size = sidebarWidth - 32f
            val leftX = sidebarLeft + 16f
            ctx.canvas.drawBitmap(photo, null, RectF(leftX, sideY, leftX + size, sideY + size), null)
            sideY += size + 12f
        }
        ctx.textPaint.color = ctx.template.palette.text
        ctx.titlePaint.color = ctx.template.palette.primary
        ctx.mutedPaint.color = ctx.template.palette.muted
        sideY = drawWrapped(ctx, "Contact", sidebarLeft + 16f, sideY, sidebarWidth - 32f, ctx.sectionPaint)
        sideY += 6f
        listOfNotNull(
            resume.contact.email.takeIf { it.isNotBlank() },
            resume.contact.phone.takeIf { it.isNotBlank() },
            resume.contact.location.takeIf { it.isNotBlank() },
            resume.contact.website.takeIf { it.isNotBlank() },
            resume.contact.linkedIn.takeIf { it.isNotBlank() }
        ).forEach {
            sideY = drawWrapped(ctx, it, sidebarLeft + 16f, sideY, sidebarWidth - 32f, ctx.mutedPaint)
            sideY += 4f
        }

        val skills = visibleSections(resume).firstOrNull { it.type == SectionType.SKILLS }
        if (skills != null) {
            sideY += 12f
            sideY = drawWrapped(ctx, skills.title, sidebarLeft + 16f, sideY, sidebarWidth - 32f, ctx.sectionPaint)
            sideY += 6f
            skills.skills.filter { it.name.isNotBlank() }.forEach { skill ->
                val line = if (skill.level.isBlank()) skill.name else "${skill.name} — ${skill.level}"
                sideY = drawWrapped(ctx, "• $line", sidebarLeft + 16f, sideY, sidebarWidth - 32f, ctx.textPaint)
                sideY += 3f
            }
        }

        ctx.contentLeft = contentLeft
        ctx.contentWidth = contentWidth
        ctx.y = ctx.margin
        drawNameAndContact(ctx, resume.contact, resume.targetJobTitle, inset = contentLeft, includeContact = false)
        drawSummary(ctx, resume.summary)
        visibleSections(resume)
            .filter { it.type != SectionType.SKILLS }
            .forEach { drawSection(ctx, it) }
    }

    private fun drawTimeline(ctx: DrawContext, resume: ResumeEntity) {
        ctx.y = ctx.margin
        drawHeader(ctx, resume, fullWidth = true)
        drawSummary(ctx, resume.summary)
        val lineX = ctx.margin + 10f
        visibleSections(resume).forEach { section ->
            ensureSpace(ctx, 40f)
            ctx.y = drawWrapped(ctx, section.title.uppercase(), ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.sectionPaint)
            ctx.y += 8f
            when (section.type) {
                SectionType.EXPERIENCE -> section.experience.forEach { item ->
                    ensureSpace(ctx, 48f)
                    ctx.paint.color = ctx.template.palette.accent
                    ctx.canvas.drawCircle(lineX, ctx.y + 6f, 4f, ctx.paint)
                    ctx.paint.strokeWidth = 1.5f
                    ctx.paint.style = Paint.Style.STROKE
                    ctx.canvas.drawLine(lineX, ctx.y + 10f, lineX, ctx.y + 40f, ctx.paint)
                    ctx.paint.style = Paint.Style.FILL
                    val left = lineX + 14f
                    val width = PAGE_WIDTH - left - ctx.margin
                    ctx.y = drawWrapped(ctx, item.title.ifBlank { "Role" }, left, ctx.y, width, ctx.itemTitlePaint)
                    val meta = listOf(item.company, item.location, dateRange(item.startDate, item.endDate))
                        .filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        ctx.y = drawWrapped(ctx, meta, left, ctx.y + 2f, width, ctx.mutedPaint)
                    }
                    item.bullets.filter { it.isNotBlank() }.forEach { bullet ->
                        ctx.y = drawWrapped(ctx, "• $bullet", left, ctx.y + 2f, width, ctx.textPaint)
                    }
                    ctx.y += ctx.blockGap
                }
                else -> drawSectionBody(ctx, section)
            }
            ctx.y += ctx.sectionGap
        }
    }

    private fun drawCompact(ctx: DrawContext, resume: ResumeEntity) {
        ctx.y = ctx.margin
        drawHeader(ctx, resume, fullWidth = true)
        drawSummary(ctx, resume.summary)
        val sections = visibleSections(resume)
        var i = 0
        while (i < sections.size) {
            val leftSection = sections[i]
            val rightSection = sections.getOrNull(i + 1)
            val startY = ctx.y
            val colWidth = (PAGE_WIDTH - ctx.margin * 2 - 12f) / 2f
            val leftX = ctx.margin
            val rightX = ctx.margin + colWidth + 12f

            ctx.contentLeft = leftX
            ctx.contentWidth = colWidth
            ctx.y = startY
            drawSection(ctx, leftSection)
            val leftEnd = ctx.y

            var rightEnd = startY
            if (rightSection != null) {
                ctx.contentLeft = rightX
                ctx.contentWidth = colWidth
                ctx.y = startY
                drawSection(ctx, rightSection)
                rightEnd = ctx.y
                i += 2
            } else {
                i += 1
            }
            ctx.contentLeft = ctx.margin
            ctx.contentWidth = PAGE_WIDTH - ctx.margin * 2
            ctx.y = max(leftEnd, rightEnd) + ctx.sectionGap
        }
    }

    private fun drawHeader(ctx: DrawContext, resume: ResumeEntity, fullWidth: Boolean) {
        val photo = loadPhoto(resume.photoPath, ctx.template.showPhoto)
        if (photo != null && fullWidth) {
            val size = 72f
            ctx.canvas.drawBitmap(photo, null, RectF(PAGE_WIDTH - ctx.margin - size, ctx.y, PAGE_WIDTH - ctx.margin, ctx.y + size), null)
        }
        drawNameAndContact(ctx, resume.contact, resume.targetJobTitle, inset = ctx.contentLeft)
        ctx.paint.color = ctx.template.palette.accent
        ctx.canvas.drawRect(ctx.contentLeft, ctx.y, ctx.contentLeft + ctx.contentWidth, ctx.y + 2f, ctx.paint)
        ctx.y += 12f
    }

    private fun drawNameAndContact(
        ctx: DrawContext,
        contact: ContactInfo,
        targetJob: String,
        inset: Float,
        includeContact: Boolean = true
    ) {
        val name = contact.fullName.ifBlank { "Your Name" }
        ctx.y = drawWrapped(ctx, name, inset, ctx.y, ctx.contentWidth, ctx.titlePaint)
        if (targetJob.isNotBlank()) {
            ctx.y = drawWrapped(ctx, targetJob, inset, ctx.y + 2f, ctx.contentWidth, ctx.itemTitlePaint)
        }
        if (includeContact) {
            val line = listOf(contact.email, contact.phone, contact.location, contact.website, contact.linkedIn)
                .filter { it.isNotBlank() }
                .joinToString("  |  ")
            if (line.isNotBlank()) {
                ctx.y = drawWrapped(ctx, line, inset, ctx.y + 4f, ctx.contentWidth, ctx.mutedPaint)
            }
        }
        ctx.y += 10f
    }

    private fun drawSummary(ctx: DrawContext, summary: String) {
        if (summary.isBlank()) return
        ensureSpace(ctx, 40f)
        ctx.y = drawWrapped(ctx, "SUMMARY", ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.sectionPaint)
        ctx.y += 6f
        ctx.y = drawWrapped(ctx, summary, ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.textPaint)
        ctx.y += ctx.sectionGap
    }

    private fun drawSection(ctx: DrawContext, section: ResumeSection) {
        if (section.isEmpty()) return
        ensureSpace(ctx, 36f)
        ctx.y = drawWrapped(ctx, section.title.uppercase(), ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.sectionPaint)
        ctx.paint.color = ctx.template.palette.accent
        ctx.canvas.drawRect(ctx.contentLeft, ctx.y + 2f, ctx.contentLeft + 40f, ctx.y + 4f, ctx.paint)
        ctx.y += 10f
        drawSectionBody(ctx, section)
        ctx.y += ctx.sectionGap
    }

    private fun drawSectionBody(ctx: DrawContext, section: ResumeSection) {
        when (section.type) {
            SectionType.EXPERIENCE -> section.experience.forEach { item ->
                ensureSpace(ctx, 40f)
                ctx.y = drawWrapped(ctx, item.title.ifBlank { "Role" }, ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.itemTitlePaint)
                val meta = listOf(item.company, item.location, dateRange(item.startDate, item.endDate))
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) ctx.y = drawWrapped(ctx, meta, ctx.contentLeft, ctx.y + 2f, ctx.contentWidth, ctx.mutedPaint)
                item.bullets.filter { it.isNotBlank() }.forEach {
                    ctx.y = drawWrapped(ctx, "• $it", ctx.contentLeft, ctx.y + 2f, ctx.contentWidth, ctx.textPaint)
                }
                ctx.y += ctx.blockGap
            }
            SectionType.EDUCATION -> section.education.forEach { item ->
                ensureSpace(ctx, 36f)
                ctx.y = drawWrapped(ctx, item.degree.ifBlank { "Degree" }, ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.itemTitlePaint)
                val meta = listOf(item.school, item.location, dateRange(item.startDate, item.endDate))
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) ctx.y = drawWrapped(ctx, meta, ctx.contentLeft, ctx.y + 2f, ctx.contentWidth, ctx.mutedPaint)
                if (item.details.isNotBlank()) {
                    ctx.y = drawWrapped(ctx, item.details, ctx.contentLeft, ctx.y + 2f, ctx.contentWidth, ctx.textPaint)
                }
                ctx.y += ctx.blockGap
            }
            SectionType.SKILLS -> {
                val text = section.skills.filter { it.name.isNotBlank() }.joinToString("  •  ") {
                    if (it.level.isBlank()) it.name else "${it.name} (${it.level})"
                }
                if (text.isNotBlank()) {
                    ctx.y = drawWrapped(ctx, text, ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.textPaint)
                }
            }
            SectionType.PROJECTS -> section.projects.forEach { item ->
                ensureSpace(ctx, 36f)
                val title = buildString {
                    append(item.name.ifBlank { "Project" })
                    if (item.link.isNotBlank()) append(" — ${item.link}")
                }
                ctx.y = drawWrapped(ctx, title, ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.itemTitlePaint)
                if (item.description.isNotBlank()) {
                    ctx.y = drawWrapped(ctx, item.description, ctx.contentLeft, ctx.y + 2f, ctx.contentWidth, ctx.textPaint)
                }
                item.bullets.filter { it.isNotBlank() }.forEach {
                    ctx.y = drawWrapped(ctx, "• $it", ctx.contentLeft, ctx.y + 2f, ctx.contentWidth, ctx.textPaint)
                }
                ctx.y += ctx.blockGap
            }
            SectionType.CERTIFICATIONS -> section.certifications.forEach { item ->
                ensureSpace(ctx, 28f)
                val line = listOf(item.name, item.issuer, item.date).filter { it.isNotBlank() }.joinToString(" · ")
                ctx.y = drawWrapped(ctx, line, ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.textPaint)
                ctx.y += ctx.blockGap
            }
            SectionType.LANGUAGES -> {
                val text = section.languages.filter { it.name.isNotBlank() }.joinToString("  •  ") {
                    if (it.proficiency.isBlank()) it.name else "${it.name} (${it.proficiency})"
                }
                if (text.isNotBlank()) {
                    ctx.y = drawWrapped(ctx, text, ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.textPaint)
                }
            }
            SectionType.CUSTOM -> section.custom.forEach { item ->
                ensureSpace(ctx, 28f)
                if (item.title.isNotBlank()) {
                    ctx.y = drawWrapped(ctx, item.title, ctx.contentLeft, ctx.y, ctx.contentWidth, ctx.itemTitlePaint)
                }
                if (item.body.isNotBlank()) {
                    ctx.y = drawWrapped(ctx, item.body, ctx.contentLeft, ctx.y + 2f, ctx.contentWidth, ctx.textPaint)
                }
                ctx.y += ctx.blockGap
            }
        }
    }

    private fun visibleSections(resume: ResumeEntity): List<ResumeSection> =
        resume.sections.filter { it.visible && !it.isEmpty() }

    private fun dateRange(start: String, end: String): String =
        listOf(start, end).filter { it.isNotBlank() }.joinToString(" – ")

    private fun loadPhoto(path: String?, showPhoto: Boolean): Bitmap? {
        if (!showPhoto || path.isNullOrBlank()) return null
        return runCatching {
            BitmapFactory.decodeFile(path)?.let { src ->
                val size = min(src.width, src.height)
                val x = (src.width - size) / 2
                val y = (src.height - size) / 2
                Bitmap.createBitmap(src, x, y, size, size)
            }
        }.getOrNull()
    }

    private fun ensureSpace(ctx: DrawContext, needed: Float) {
        if (ctx.y + needed > PAGE_HEIGHT - ctx.margin) {
            ctx.advancePage()
        }
    }

    private fun drawWrapped(
        ctx: DrawContext,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        paint: TextPaint
    ): Float {
        if (text.isBlank()) return y
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()
        var cursor = y
        if (ctx.shouldDraw) {
            ctx.canvas.save()
            ctx.canvas.translate(x, y)
            layout.draw(ctx.canvas)
            ctx.canvas.restore()
        }
        cursor += layout.height
        return cursor
    }

    private class MeasureCanvas : Canvas()

    private inner class DrawContext(
        val canvas: Canvas,
        val template: TemplatePreset,
        private val pageIndexFilter: Int
    ) {
        val margin = when (template.density) {
            Density.COMPACT -> 28f
            Density.NORMAL -> 36f
            Density.SPACIOUS -> 44f
        }
        val sectionGap = when (template.density) {
            Density.COMPACT -> 10f
            Density.NORMAL -> 14f
            Density.SPACIOUS -> 18f
        }
        val blockGap = when (template.density) {
            Density.COMPACT -> 6f
            Density.NORMAL -> 8f
            Density.SPACIOUS -> 12f
        }

        var contentLeft = margin
        var contentWidth = PAGE_WIDTH - margin * 2
        var y = margin
        private var currentPage = 0
        var totalPages = 1
            private set

        val shouldDraw: Boolean
            get() = pageIndexFilter < 0 || currentPage == pageIndexFilter

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = textPaint(22f, Typeface.BOLD)
        val sectionPaint = textPaint(11f, Typeface.BOLD)
        val itemTitlePaint = textPaint(11f, Typeface.BOLD)
        val textPaint = textPaint(10f, Typeface.NORMAL)
        val mutedPaint = textPaint(9f, Typeface.NORMAL)

        init {
            resetTextColors()
            applyTypeface()
        }

        fun fillBackground() {
            if (!shouldDraw) return
            paint.color = template.palette.background
            canvas.drawRect(0f, 0f, PAGE_WIDTH, PAGE_HEIGHT, paint)
        }

        fun resetTextColors() {
            titlePaint.color = template.palette.primary
            sectionPaint.color = template.palette.primary
            itemTitlePaint.color = template.palette.text
            textPaint.color = template.palette.text
            mutedPaint.color = template.palette.muted
        }

        fun advancePage() {
            currentPage += 1
            totalPages = max(totalPages, currentPage + 1)
            y = margin
            if (shouldDraw) {
                // New page background for PdfDocument is created by caller per page.
                // For on-screen multi-page preview we only render one page at a time.
            }
            fillBackground()
        }

        private fun textPaint(size: Float, style: Int): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                typeface = Typeface.create(Typeface.SANS_SERIF, style)
            }

        private fun applyTypeface() {
            val family = when (template.typeStyle) {
                TypeStyle.SERIF -> Typeface.SERIF
                TypeStyle.TECHNICAL -> Typeface.MONOSPACE
                TypeStyle.MODERN, TypeStyle.CLASSIC -> Typeface.SANS_SERIF
            }
            listOf(titlePaint, sectionPaint, itemTitlePaint, textPaint, mutedPaint).forEach {
                val style = if (it.typeface?.isBold == true) Typeface.BOLD else Typeface.NORMAL
                it.typeface = Typeface.create(family, style)
            }
            if (template.typeStyle == TypeStyle.MODERN) {
                titlePaint.textSize = 24f
            }
        }
    }
}

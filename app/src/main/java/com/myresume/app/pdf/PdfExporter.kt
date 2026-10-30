package com.myresume.app.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.myresume.app.data.ResumeEntity
import com.myresume.app.template.TemplateCatalog
import com.myresume.app.template.TemplatePreset
import java.io.File
import java.io.FileOutputStream

class PdfExporter(
    private val context: Context,
    private val renderer: ResumeCanvasRenderer = ResumeCanvasRenderer()
) {
    fun exportToCache(resume: ResumeEntity, template: TemplatePreset = TemplateCatalog.byId(resume.templateId)): File {
        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val safeName = resume.title.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "resume" }
        val file = File(dir, "$safeName.pdf")
        writePdf(resume, template, FileOutputStream(file))
        return file
    }

    fun exportToUri(resume: ResumeEntity, uri: Uri, template: TemplatePreset = TemplateCatalog.byId(resume.templateId)) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            writePdf(resume, template, stream)
        } ?: error("Unable to open output stream")
    }

    private fun writePdf(resume: ResumeEntity, template: TemplatePreset, output: java.io.OutputStream) {
        val document = PdfDocument()
        val pageCount = renderer.pageCount(resume, template).coerceAtLeast(1)
        try {
            for (pageIndex in 0 until pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    ResumeCanvasRenderer.PAGE_WIDTH.toInt(),
                    ResumeCanvasRenderer.PAGE_HEIGHT.toInt(),
                    pageIndex + 1
                ).create()
                val page = document.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                renderer.drawPage(canvas, resume, template, pageIndex)
                document.finishPage(page)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }
}

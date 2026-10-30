package com.myresume.app

import android.content.Context
import com.myresume.app.ai.ModelDownloader
import com.myresume.app.ai.ResumeAssistant
import com.myresume.app.billing.BillingFactory
import com.myresume.app.billing.BillingGateway
import com.myresume.app.billing.EntitlementStore
import com.myresume.app.data.ResumeDatabase
import com.myresume.app.data.ResumeRepository
import com.myresume.app.pdf.PdfExporter
import com.myresume.app.pdf.ResumeCanvasRenderer

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: ResumeDatabase = ResumeDatabase.getInstance(appContext)
    val resumeRepository: ResumeRepository = ResumeRepository(database.resumeDao())
    val entitlementStore: EntitlementStore = EntitlementStore(appContext)
    val billing: BillingGateway = BillingFactory.create(appContext, entitlementStore)
    val renderer: ResumeCanvasRenderer = ResumeCanvasRenderer()
    val pdfExporter: PdfExporter = PdfExporter(appContext, renderer)
    val modelDownloader: ModelDownloader = ModelDownloader(appContext)
    val resumeAssistant: ResumeAssistant = ResumeAssistant(appContext)
}

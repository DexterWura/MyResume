package com.myresume.app.ui.preview

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myresume.app.AppContainer
import com.myresume.app.billing.Entitlements
import com.myresume.app.pdf.ResumeCanvasRenderer
import com.myresume.app.template.TemplateCatalog
import com.myresume.app.template.TemplateTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    container: AppContainer,
    resumeId: String,
    onBack: () -> Unit,
    onPaywall: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val resume by container.resumeRepository.observeById(resumeId)
        .collectAsStateWithLifecycle(initialValue = null)
    val entitlements by container.entitlementStore.entitlements
        .collectAsStateWithLifecycle(initialValue = Entitlements())

    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(1) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val current = resume
    val template = current?.let { TemplateCatalog.byId(it.templateId) }
    val locked = template != null &&
        template.tier == TemplateTier.PREMIUM &&
        !entitlements.premiumTemplates

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null || current == null || template == null || locked) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    container.pdfExporter.exportToUri(current, uri, template)
                }
                status = "PDF saved"
            }.onFailure {
                status = it.message ?: "Save failed"
            }
        }
    }

    LaunchedEffect(current?.updatedAt, current?.templateId, pageIndex, locked) {
        if (current == null || template == null || locked) {
            bitmap = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.Default) {
            pageCount = container.renderer.pageCount(current, template).coerceAtLeast(1)
            val width = with(density) { 360.dp.roundToPx() }
            val scale = width / ResumeCanvasRenderer.PAGE_WIDTH
            val height = (ResumeCanvasRenderer.PAGE_HEIGHT * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            canvas.scale(scale, scale)
            container.renderer.drawPage(canvas, current, template, pageIndex.coerceAtMost(pageCount - 1))
            bitmap = bmp
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (current == null || template == null) return@IconButton
                            if (locked) {
                                onPaywall()
                                return@IconButton
                            }
                            scope.launch {
                                runCatching {
                                    val file = withContext(Dispatchers.IO) {
                                        container.pdfExporter.exportToCache(current, template)
                                    }
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share resume"))
                                }.onFailure {
                                    status = it.message ?: "Share failed"
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                current == null -> Text("Loading…")
                locked -> {
                    Text("This premium template is locked. Unlock premium templates to preview and export it.")
                    Button(onClick = onPaywall) { Text("Unlock premium templates") }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                            enabled = pageIndex > 0
                        ) { Text("Prev page") }
                        Text("Page ${pageIndex + 1} / $pageCount", modifier = Modifier.padding(top = 12.dp))
                        Button(
                            onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) },
                            enabled = pageIndex < pageCount - 1
                        ) { Text("Next page") }
                    }
                    Button(
                        onClick = {
                            val name = (current.title.ifBlank { "resume" }) + ".pdf"
                            createDoc.launch(name)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save PDF")
                    }
                    status?.let { Text(it) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Resume preview",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

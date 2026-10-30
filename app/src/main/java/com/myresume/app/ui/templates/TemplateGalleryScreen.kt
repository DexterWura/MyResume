package com.myresume.app.ui.templates

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myresume.app.AppContainer
import com.myresume.app.billing.Entitlements
import com.myresume.app.data.ResumeEntity
import com.myresume.app.pdf.ResumeCanvasRenderer
import com.myresume.app.template.TemplateCatalog
import com.myresume.app.template.TemplatePreset
import com.myresume.app.template.TemplateTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class TemplateFilter { ALL, FREE, PREMIUM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateGalleryScreen(
    container: AppContainer,
    resumeId: String,
    onBack: () -> Unit,
    onPaywall: () -> Unit
) {
    val resume by container.resumeRepository.observeById(resumeId)
        .collectAsStateWithLifecycle(initialValue = null)
    val entitlements by container.entitlementStore.entitlements
        .collectAsStateWithLifecycle(initialValue = Entitlements())
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TemplateFilter.ALL) }
    val scope = rememberCoroutineScope()
    val current = resume

    val templates = remember(query, filter) {
        TemplateCatalog.all.filter { preset ->
            val matchesFilter = when (filter) {
                TemplateFilter.ALL -> true
                TemplateFilter.FREE -> preset.tier == TemplateTier.FREE
                TemplateFilter.PREMIUM -> preset.tier == TemplateTier.PREMIUM
            }
            val matchesQuery = query.isBlank() ||
                preset.name.contains(query, ignoreCase = true) ||
                preset.tags.any { it.contains(query, ignoreCase = true) }
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates (${templates.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search templates") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == TemplateFilter.ALL,
                    onClick = { filter = TemplateFilter.ALL },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter == TemplateFilter.FREE,
                    onClick = { filter = TemplateFilter.FREE },
                    label = { Text("Free") }
                )
                FilterChip(
                    selected = filter == TemplateFilter.PREMIUM,
                    onClick = { filter = TemplateFilter.PREMIUM },
                    label = { Text("Premium") }
                )
            }
            if (current == null) {
                Text("Loading…", modifier = Modifier.padding(16.dp))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(templates, key = { it.id }) { preset ->
                        val locked = preset.tier == TemplateTier.PREMIUM && !entitlements.premiumTemplates
                        TemplateCard(
                            resume = current,
                            preset = preset,
                            selected = current.templateId == preset.id,
                            locked = locked,
                            container = container,
                            onClick = {
                                if (locked) {
                                    onPaywall()
                                } else {
                                    scope.launch {
                                        container.resumeRepository.save(
                                            current.copy(templateId = preset.id)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    resume: ResumeEntity,
    preset: TemplatePreset,
    selected: Boolean,
    locked: Boolean,
    container: AppContainer,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    var bitmap by remember(preset.id, resume.updatedAt) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(preset.id, resume.updatedAt) {
        bitmap = withContext(Dispatchers.Default) {
            val width = with(density) { 150.dp.roundToPx() }
            container.renderer.renderThumbnail(resume, preset, width)
        }
    }
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                else Modifier
            )
            .padding(4.dp)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = preset.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ResumeCanvasRenderer.PAGE_WIDTH / ResumeCanvasRenderer.PAGE_HEIGHT),
                contentScale = ContentScale.Fit
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(preset.name, style = MaterialTheme.typography.labelMedium, maxLines = 2)
            if (locked) {
                Icon(Icons.Default.Lock, contentDescription = "Premium")
            }
        }
    }
}

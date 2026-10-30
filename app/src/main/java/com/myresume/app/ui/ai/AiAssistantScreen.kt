package com.myresume.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myresume.app.AppContainer
import com.myresume.app.BuildConfig
import com.myresume.app.R
import com.myresume.app.ai.AssistTask
import com.myresume.app.ai.DownloadState
import com.myresume.app.ai.ModelPaths
import com.myresume.app.billing.Entitlements
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    container: AppContainer,
    resumeId: String,
    onBack: () -> Unit,
    onPaywall: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val entitlements by container.entitlementStore.entitlements
        .collectAsStateWithLifecycle(initialValue = Entitlements())
    val downloadState by container.modelDownloader.observeProgress()
        .collectAsStateWithLifecycle(initialValue = DownloadState.Idle)
    val resume by container.resumeRepository.observeById(resumeId)
        .collectAsStateWithLifecycle(initialValue = null)

    var task by remember { mutableStateOf(AssistTask.DRAFT_SUMMARY) }
    var notes by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var suggestion by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI assistant") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.ai_disclosure_body), style = MaterialTheme.typography.bodyMedium)

            if (!entitlements.aiAssistant) {
                Text("AI help is a one-time unlock through Google Play.")
                Button(onClick = onPaywall, modifier = Modifier.fillMaxWidth()) {
                    Text("Unlock AI assistant")
                }
                return@Column
            }

            if (container.resumeAssistant.deviceLikelyTooSmall()) {
                Text(
                    "This phone reports under about 3 GB of RAM. The on-device model may fail to load.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            when (val state = if (container.modelDownloader.isPresent()) DownloadState.Ready else downloadState) {
                DownloadState.Idle, is DownloadState.Failed -> {
                    Text(stringResource(R.string.gemma_license_notice))
                    Text("Approximate download size: ${ModelPaths.APPROX_SIZE_MB} MB")
                    if (BuildConfig.MODEL_URL.isBlank()) {
                        Text(
                            "MODEL_URL is not set in local.properties. Host the Gemma 3 1B int4 .litertlm file and set MODEL_URL before downloading.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (state is DownloadState.Failed) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { container.modelDownloader.enqueueDownload(preferWifi = true) },
                        enabled = BuildConfig.MODEL_URL.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download model on Wi‑Fi")
                    }
                    OutlinedButton(
                        onClick = { container.modelDownloader.enqueueDownload(preferWifi = false) },
                        enabled = BuildConfig.MODEL_URL.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download on any network")
                    }
                }
                is DownloadState.Downloading -> {
                    Text("Downloading model… ${state.percent}%")
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                DownloadState.Ready -> {
                    Text("Model ready on this device.", color = MaterialTheme.colorScheme.primary)

                    RowChips(task) { task = it }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (task == AssistTask.REWRITE_BULLET) "Bullet to rewrite" else "Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    if (task == AssistTask.TAILOR_TO_JOB) {
                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            label = { Text("Job description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5
                        )
                    }

                    Button(
                        onClick = {
                            val current = resume ?: return@Button
                            busy = true
                            error = null
                            suggestion = ""
                            val contextText = buildString {
                                append("Name: ${current.contact.fullName}\n")
                                append("Target: ${current.targetJobTitle}\n")
                                append("Summary: ${current.summary}\n")
                                current.sections.forEach { section ->
                                    append("${section.title}:\n")
                                    when (section.type) {
                                        com.myresume.app.data.SectionType.EXPERIENCE ->
                                            section.experience.forEach {
                                                append("- ${it.title} at ${it.company}: ${it.bullets.joinToString("; ")}\n")
                                            }
                                        com.myresume.app.data.SectionType.SKILLS ->
                                            append(section.skills.joinToString(", ") { it.name } + "\n")
                                        else -> Unit
                                    }
                                }
                            }
                            scope.launch {
                                container.resumeAssistant.generate(
                                    task = task,
                                    resumeContext = contextText,
                                    userInput = notes,
                                    jobDescription = jobDescription
                                ).catch {
                                    error = it.message ?: "Generation failed"
                                    busy = false
                                }.collect {
                                    suggestion = it
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (busy) "Generating…" else "Generate suggestion")
                    }

                    if (suggestion.isNotBlank()) {
                        Text("Generated suggestion — review before applying", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = suggestion,
                            onValueChange = { suggestion = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5
                        )
                        Button(
                            onClick = {
                                val current = resume ?: return@Button
                                scope.launch {
                                    when (task) {
                                        AssistTask.DRAFT_SUMMARY ->
                                            container.resumeRepository.save(current.copy(summary = suggestion.trim()))
                                        AssistTask.REWRITE_BULLET, AssistTask.TAILOR_TO_JOB -> {
                                            // Put tailored text into summary notes area for user control
                                            val merged = if (current.summary.isBlank()) suggestion.trim()
                                            else current.summary + "\n\n" + suggestion.trim()
                                            container.resumeRepository.save(current.copy(summary = merged))
                                        }
                                    }
                                    error = null
                                    suggestion = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply to resume")
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun RowChips(selected: AssistTask, onSelect: (AssistTask) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Task", style = MaterialTheme.typography.titleSmall)
        FilterChip(
            selected = selected == AssistTask.DRAFT_SUMMARY,
            onClick = { onSelect(AssistTask.DRAFT_SUMMARY) },
            label = { Text("Draft summary") }
        )
        FilterChip(
            selected = selected == AssistTask.REWRITE_BULLET,
            onClick = { onSelect(AssistTask.REWRITE_BULLET) },
            label = { Text("Rewrite bullet") }
        )
        FilterChip(
            selected = selected == AssistTask.TAILOR_TO_JOB,
            onClick = { onSelect(AssistTask.TAILOR_TO_JOB) },
            label = { Text("Tailor to job") }
        )
    }
}

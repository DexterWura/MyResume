package com.myresume.app.ui.editor

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myresume.app.billing.PlayProducts
import com.myresume.app.data.CertificationItem
import com.myresume.app.data.CustomItem
import com.myresume.app.data.EducationItem
import com.myresume.app.data.ExperienceItem
import com.myresume.app.data.LanguageItem
import com.myresume.app.data.ProjectItem
import com.myresume.app.data.ResumeFactory
import com.myresume.app.data.ResumeSection
import com.myresume.app.data.SectionType
import com.myresume.app.data.SkillItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onTemplates: () -> Unit,
    onPreview: () -> Unit,
    onAi: () -> Unit,
    onPaywall: (String) -> Unit
) {
    val resume by viewModel.resume.collectAsStateWithLifecycle()
    val entitlements by viewModel.entitlements.collectAsStateWithLifecycle()
    val current = resume ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit resume") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onTemplates) {
                        Icon(Icons.Default.Style, contentDescription = "Templates")
                    }
                    IconButton(onClick = {
                        if (entitlements.aiAssistant) onAi() else onPaywall(PlayProducts.AI_ASSISTANT)
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI help")
                    }
                    IconButton(onClick = onPreview) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Preview")
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
            OutlinedTextField(
                value = current.title,
                onValueChange = viewModel::setTitle,
                label = { Text("Resume name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = current.targetJobTitle,
                onValueChange = viewModel::setTargetJob,
                label = { Text("Target job title") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Contact", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = current.contact.fullName,
                onValueChange = { viewModel.setContact(current.contact.copy(fullName = it)) },
                label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = current.contact.email,
                onValueChange = { viewModel.setContact(current.contact.copy(email = it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = current.contact.phone,
                onValueChange = { viewModel.setContact(current.contact.copy(phone = it)) },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = current.contact.location,
                onValueChange = { viewModel.setContact(current.contact.copy(location = it)) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = current.contact.website,
                onValueChange = { viewModel.setContact(current.contact.copy(website = it)) },
                label = { Text("Website") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = current.contact.linkedIn,
                onValueChange = { viewModel.setContact(current.contact.copy(linkedIn = it)) },
                label = { Text("LinkedIn") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = current.summary,
                onValueChange = viewModel::setSummary,
                label = { Text("Professional summary") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Text("Sections", style = MaterialTheme.typography.titleMedium)
            current.sections.forEachIndexed { index, section ->
                SectionEditor(
                    section = section,
                    canMoveUp = index > 0,
                    canMoveDown = index < current.sections.lastIndex,
                    onMoveUp = { viewModel.moveSection(index, index - 1) },
                    onMoveDown = { viewModel.moveSection(index, index + 1) },
                    onToggle = { viewModel.toggleSection(section.id) },
                    onRemove = { viewModel.removeSection(section.id) },
                    onChange = viewModel::replaceSection
                )
            }

            var showAdd by remember { mutableStateOf(false) }
            Button(onClick = { showAdd = !showAdd }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showAdd) "Hide section types" else "Add section")
            }
            if (showAdd) {
                SectionType.entries.forEach { type ->
                    TextButton(onClick = {
                        viewModel.addSection(type)
                        showAdd = false
                    }) {
                        Text("Add ${type.name.lowercase().replaceFirstChar { it.uppercase() }}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionEditor(
    section: ResumeSection,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onChange: (ResumeSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(section.title, style = MaterialTheme.typography.titleSmall)
            Row {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (section.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle visibility"
                    )
                }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
        }
        if (section.type == SectionType.CUSTOM) {
            OutlinedTextField(
                value = section.title,
                onValueChange = { onChange(section.copy(title = it)) },
                label = { Text("Section title") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        when (section.type) {
            SectionType.EXPERIENCE -> {
                section.experience.forEachIndexed { i, item ->
                    ExperienceFields(item) { updated ->
                        val list = section.experience.toMutableList()
                        list[i] = updated
                        onChange(section.copy(experience = list))
                    }
                }
                TextButton(onClick = {
                    onChange(
                        section.copy(
                            experience = section.experience + ExperienceItem(
                                id = ResumeFactory.newId(),
                                bullets = listOf("")
                            )
                        )
                    )
                }) { Text("Add role") }
            }
            SectionType.EDUCATION -> {
                section.education.forEachIndexed { i, item ->
                    EducationFields(item) { updated ->
                        val list = section.education.toMutableList()
                        list[i] = updated
                        onChange(section.copy(education = list))
                    }
                }
                TextButton(onClick = {
                    onChange(section.copy(education = section.education + EducationItem(ResumeFactory.newId())))
                }) { Text("Add school") }
            }
            SectionType.SKILLS -> {
                section.skills.forEachIndexed { i, item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = {
                                val list = section.skills.toMutableList()
                                list[i] = item.copy(name = it)
                                onChange(section.copy(skills = list))
                            },
                            label = { Text("Skill") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = item.level,
                            onValueChange = {
                                val list = section.skills.toMutableList()
                                list[i] = item.copy(level = it)
                                onChange(section.copy(skills = list))
                            },
                            label = { Text("Level") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TextButton(onClick = {
                    onChange(section.copy(skills = section.skills + SkillItem(ResumeFactory.newId())))
                }) { Text("Add skill") }
            }
            SectionType.PROJECTS -> {
                section.projects.forEachIndexed { i, item ->
                    ProjectFields(item) { updated ->
                        val list = section.projects.toMutableList()
                        list[i] = updated
                        onChange(section.copy(projects = list))
                    }
                }
                TextButton(onClick = {
                    onChange(
                        section.copy(
                            projects = section.projects + ProjectItem(
                                id = ResumeFactory.newId(),
                                bullets = listOf("")
                            )
                        )
                    )
                }) { Text("Add project") }
            }
            SectionType.CERTIFICATIONS -> {
                section.certifications.forEachIndexed { i, item ->
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = {
                            val list = section.certifications.toMutableList()
                            list[i] = item.copy(name = it)
                            onChange(section.copy(certifications = list))
                        },
                        label = { Text("Certification") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = item.issuer,
                        onValueChange = {
                            val list = section.certifications.toMutableList()
                            list[i] = item.copy(issuer = it)
                            onChange(section.copy(certifications = list))
                        },
                        label = { Text("Issuer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = item.date,
                        onValueChange = {
                            val list = section.certifications.toMutableList()
                            list[i] = item.copy(date = it)
                            onChange(section.copy(certifications = list))
                        },
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(onClick = {
                    onChange(
                        section.copy(
                            certifications = section.certifications + CertificationItem(ResumeFactory.newId())
                        )
                    )
                }) { Text("Add certification") }
            }
            SectionType.LANGUAGES -> {
                section.languages.forEachIndexed { i, item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = {
                                val list = section.languages.toMutableList()
                                list[i] = item.copy(name = it)
                                onChange(section.copy(languages = list))
                            },
                            label = { Text("Language") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = item.proficiency,
                            onValueChange = {
                                val list = section.languages.toMutableList()
                                list[i] = item.copy(proficiency = it)
                                onChange(section.copy(languages = list))
                            },
                            label = { Text("Proficiency") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TextButton(onClick = {
                    onChange(section.copy(languages = section.languages + LanguageItem(ResumeFactory.newId())))
                }) { Text("Add language") }
            }
            SectionType.CUSTOM -> {
                section.custom.forEachIndexed { i, item ->
                    OutlinedTextField(
                        value = item.title,
                        onValueChange = {
                            val list = section.custom.toMutableList()
                            list[i] = item.copy(title = it)
                            onChange(section.copy(custom = list))
                        },
                        label = { Text("Entry title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = item.body,
                        onValueChange = {
                            val list = section.custom.toMutableList()
                            list[i] = item.copy(body = it)
                            onChange(section.copy(custom = list))
                        },
                        label = { Text("Body") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                TextButton(onClick = {
                    onChange(section.copy(custom = section.custom + CustomItem(ResumeFactory.newId())))
                }) { Text("Add entry") }
            }
        }
    }
}

@Composable
private fun ExperienceFields(item: ExperienceItem, onChange: (ExperienceItem) -> Unit) {
    OutlinedTextField(item.title, { onChange(item.copy(title = it)) }, label = { Text("Job title") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(item.company, { onChange(item.copy(company = it)) }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(item.location, { onChange(item.copy(location = it)) }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(item.startDate, { onChange(item.copy(startDate = it)) }, label = { Text("Start") }, modifier = Modifier.weight(1f))
        OutlinedTextField(item.endDate, { onChange(item.copy(endDate = it)) }, label = { Text("End") }, modifier = Modifier.weight(1f))
    }
    item.bullets.forEachIndexed { i, bullet ->
        OutlinedTextField(
            value = bullet,
            onValueChange = {
                val bullets = item.bullets.toMutableList()
                bullets[i] = it
                onChange(item.copy(bullets = bullets))
            },
            label = { Text("Bullet ${i + 1}") },
            modifier = Modifier.fillMaxWidth()
        )
    }
    TextButton(onClick = { onChange(item.copy(bullets = item.bullets + "")) }) {
        Text("Add bullet")
    }
}

@Composable
private fun EducationFields(item: EducationItem, onChange: (EducationItem) -> Unit) {
    OutlinedTextField(item.degree, { onChange(item.copy(degree = it)) }, label = { Text("Degree") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(item.school, { onChange(item.copy(school = it)) }, label = { Text("School") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(item.location, { onChange(item.copy(location = it)) }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(item.startDate, { onChange(item.copy(startDate = it)) }, label = { Text("Start") }, modifier = Modifier.weight(1f))
        OutlinedTextField(item.endDate, { onChange(item.copy(endDate = it)) }, label = { Text("End") }, modifier = Modifier.weight(1f))
    }
    OutlinedTextField(item.details, { onChange(item.copy(details = it)) }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ProjectFields(item: ProjectItem, onChange: (ProjectItem) -> Unit) {
    OutlinedTextField(item.name, { onChange(item.copy(name = it)) }, label = { Text("Project name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(item.link, { onChange(item.copy(link = it)) }, label = { Text("Link") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(item.description, { onChange(item.copy(description = it)) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
    item.bullets.forEachIndexed { i, bullet ->
        OutlinedTextField(
            value = bullet,
            onValueChange = {
                val bullets = item.bullets.toMutableList()
                bullets[i] = it
                onChange(item.copy(bullets = bullets))
            },
            label = { Text("Bullet ${i + 1}") },
            modifier = Modifier.fillMaxWidth()
        )
    }
    TextButton(onClick = { onChange(item.copy(bullets = item.bullets + "")) }) {
        Text("Add bullet")
    }
}

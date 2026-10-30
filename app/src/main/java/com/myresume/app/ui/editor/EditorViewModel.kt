package com.myresume.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myresume.app.AppContainer
import com.myresume.app.billing.Entitlements
import com.myresume.app.data.ContactInfo
import com.myresume.app.data.ResumeEntity
import com.myresume.app.data.ResumeFactory
import com.myresume.app.data.ResumeRepository
import com.myresume.app.data.ResumeSection
import com.myresume.app.data.SectionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditorViewModel(
    private val resumeId: String,
    private val repository: ResumeRepository,
    entitlementsFlow: kotlinx.coroutines.flow.Flow<Entitlements>
) : ViewModel() {

    val resume: StateFlow<ResumeEntity?> = repository.observeById(resumeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val entitlements: StateFlow<Entitlements> = entitlementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Entitlements())

    fun update(transform: (ResumeEntity) -> ResumeEntity) {
        viewModelScope.launch {
            val current = repository.getById(resumeId) ?: return@launch
            repository.save(transform(current))
        }
    }

    fun setTitle(title: String) = update { it.copy(title = title) }

    fun setTargetJob(title: String) = update { it.copy(targetJobTitle = title) }

    fun setSummary(summary: String) = update { it.copy(summary = summary) }

    fun setContact(contact: ContactInfo) = update { it.copy(contact = contact) }

    fun setSections(sections: List<ResumeSection>) = update { it.copy(sections = sections) }

    fun addSection(type: SectionType, customTitle: String = "Custom") {
        update { resume ->
            resume.copy(sections = resume.sections + ResumeFactory.sectionForType(type, customTitle))
        }
    }

    fun moveSection(from: Int, to: Int) {
        update { resume ->
            val list = resume.sections.toMutableList()
            if (from !in list.indices || to !in list.indices) return@update resume
            val item = list.removeAt(from)
            list.add(to, item)
            resume.copy(sections = list)
        }
    }

    fun toggleSection(sectionId: String) {
        update { resume ->
            resume.copy(
                sections = resume.sections.map {
                    if (it.id == sectionId) it.copy(visible = !it.visible) else it
                }
            )
        }
    }

    fun removeSection(sectionId: String) {
        update { resume ->
            resume.copy(sections = resume.sections.filterNot { it.id == sectionId })
        }
    }

    fun replaceSection(section: ResumeSection) {
        update { resume ->
            resume.copy(sections = resume.sections.map { if (it.id == section.id) section else it })
        }
    }

    companion object {
        fun factory(container: AppContainer, resumeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EditorViewModel(
                        resumeId = resumeId,
                        repository = container.resumeRepository,
                        entitlementsFlow = container.entitlementStore.entitlements
                    ) as T
                }
            }
    }
}

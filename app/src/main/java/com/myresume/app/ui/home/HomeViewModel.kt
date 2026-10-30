package com.myresume.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myresume.app.data.ResumeEntity
import com.myresume.app.data.ResumeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ResumeRepository
) : ViewModel() {
    val resumes: StateFlow<List<ResumeEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val resume = repository.create()
            onCreated(resume.id)
        }
    }

    fun duplicate(id: String, title: String) {
        viewModelScope.launch {
            repository.duplicate(id, title)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}

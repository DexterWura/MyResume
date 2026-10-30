package com.myresume.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.myresume.app.AppContainer
import com.myresume.app.ui.home.HomeViewModel

class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.resumeRepository) as T
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

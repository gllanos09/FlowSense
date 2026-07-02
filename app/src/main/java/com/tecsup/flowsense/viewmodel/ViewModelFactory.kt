package com.tecsup.flowsense.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.flowsense.repository.FirebaseRepository

class ViewModelFactory(
    private val repository: FirebaseRepository,
    private val context: Context,
    private val negocioId: String = ""
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> AdminViewModel(repository) as T
            modelClass.isAssignableFrom(DuenoViewModel::class.java) -> DuenoViewModel(repository, negocioId, context.applicationContext) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

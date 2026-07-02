package com.tecsup.flowsense.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.Usuario
import com.tecsup.flowsense.repository.FirebaseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: FirebaseRepository) : ViewModel() {

    val negocios: StateFlow<List<Negocio>> = repository.observeNegocios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usuarios: StateFlow<List<Usuario>> = repository.observeUsuarios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun eliminarUsuario(uid: String) {
        viewModelScope.launch { repository.eliminarUsuario(uid) }
    }

    fun actualizarPerfil(uid: String, nombre: String, negocioId: String, aforoMax: Int) {
        viewModelScope.launch {
            repository.actualizarUsuario(uid, nombre)
            if (negocioId.isNotEmpty()) {
                repository.actualizarAforoMaximo(negocioId, aforoMax)
            }
        }
    }

    fun crearNegocio(negocio: Negocio, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.crearNegocio(negocio)
            onComplete()
        }
    }

    fun crearDueno(email: String, pass: String, nombre: String, negocioId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.crearUsuarioDueno(email, pass, nombre, negocioId)
            onComplete()
        }
    }

    fun logout(onLogout: () -> Unit) {
        repository.logout()
        onLogout()
    }
}

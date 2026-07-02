package com.tecsup.flowsense.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.tecsup.flowsense.model.Alerta
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.RegistroAforo
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.utils.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DuenoViewModel(
    private val repository: FirebaseRepository,
    private val negocioId: String,
    private val context: Context
) : ViewModel() {

    val negocio: StateFlow<Negocio?> = repository.observeNegocio(negocioId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val registros: StateFlow<List<RegistroAforo>> = repository.observeRegistrosHoy(negocioId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertas: StateFlow<List<Alerta>> = repository.observeAlertas(negocioId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var ultimaAlertaId: String? = null

    init {
        repository.iniciarMonitoreoIoT(negocioId)
        
        viewModelScope.launch {
            alertas.collectLatest { lista ->
                val ultima = lista.lastOrNull()
                if (ultima != null && ultima.id != ultimaAlertaId) {
                    ultimaAlertaId = ultima.id
                    NotificationHelper.showLocalNotification(context, "ALERTA - FLOWSENSE", ultima.mensaje)
                }
            }
        }
    }

    fun toggleDia(activo: Boolean) {
        viewModelScope.launch {
            repository.setDiaEstado(negocioId, activo)
        }
    }

    fun resolverAlerta(alertaId: String) {
        viewModelScope.launch {
            repository.resolverAlerta(negocioId, alertaId)
        }
    }

    fun logout(onLogout: () -> Unit) {
        repository.logout()
        onLogout()
    }
}

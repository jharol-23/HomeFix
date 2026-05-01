package com.tunegocio.homefix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tunegocio.homefix.data.NotificationsRepository
import com.tunegocio.homefix.data.model.NotificationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val repositorio = NotificationsRepository()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Lista de notificaciones del usuario
    private val _notificaciones = MutableStateFlow<List<NotificationModel>>(emptyList())
    val notificaciones: StateFlow<List<NotificationModel>> = _notificaciones

    // Cantidad de notificaciones no leídas
    private val _noLeidas = MutableStateFlow(0)
    val noLeidas: StateFlow<Int> = _noLeidas

    init {
        cargarNotificaciones()
        cargarNoLeidas()
    }

    private fun cargarNotificaciones() {
        viewModelScope.launch {
            repositorio.obtenerNotificaciones(uid).collect { lista ->
                _notificaciones.value = lista
            }
        }
    }

    private fun cargarNoLeidas() {
        viewModelScope.launch {
            repositorio.obtenerNoLeidas(uid).collect { cantidad ->
                _noLeidas.value = cantidad
            }
        }
    }

    // Marca todas como leídas al abrir la pantalla
    fun marcarTodasComoLeidas() {
        repositorio.marcarTodasComoLeidas(uid)
    }
}
package com.tecsup.flowsense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "negocios")
data class Negocio(
    @PrimaryKey val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val aforoMaximo: Int = 30,
    val aforoActual: Int = 0,
    val totalEntradas: Int = 0,
    val totalSalidas: Int = 0,
    val diaActivo: Boolean = false,
    val apiKey: String = "",
    val synced: Boolean = true
)

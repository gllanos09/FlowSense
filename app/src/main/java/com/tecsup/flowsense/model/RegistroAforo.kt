package com.tecsup.flowsense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registros_aforo")
data class RegistroAforo(
    @PrimaryKey val id: String = "",
    val negocioId: String = "",
    val tipo: String = "", // "ENTRADA" o "SALIDA"
    val aforoActual: Int = 0,
    val timestamp: Long = 0L,
    val synced: Boolean = true
)
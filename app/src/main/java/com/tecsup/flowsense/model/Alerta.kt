package com.tecsup.flowsense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alertas")
data class Alerta(
    @PrimaryKey val id: String = "",
    val negocioId: String = "",
    val mensaje: String = "",
    val nivel: String = "", // "90%" o "100%"
    val resuelta: Boolean = false,
    val timestamp: Long = 0L,
    val synced: Boolean = true
)
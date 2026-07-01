package com.tecsup.flowsense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = "", // "ADMIN" o "DUENO"
    val negocioId: String = "",
    val synced: Boolean = true
)
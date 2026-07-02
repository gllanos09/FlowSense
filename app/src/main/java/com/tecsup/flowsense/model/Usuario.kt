package com.tecsup.flowsense.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val id: String = "",
    val nombre: String = "",
    val email: String = "", // Usado como username
    val password: String = "", // 3-6 dígitos
    val rol: String = "", // "ADMIN" o "DUENO"
    val negocioId: String = "",
    val synced: Boolean = true
)

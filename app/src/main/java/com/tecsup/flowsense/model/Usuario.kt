package com.tecsup.flowsense.model

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = "", // "ADMIN" o "DUENO"
    val negocioId: String = ""
)
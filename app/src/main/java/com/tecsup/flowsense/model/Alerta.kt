package com.tecsup.flowsense.model

data class Alerta(
    val id: String = "",
    val negocioId: String = "",
    val mensaje: String = "",
    val nivel: String = "", // "90%" o "100%"
    val resuelta: Boolean = false,
    val timestamp: Long = 0L
)
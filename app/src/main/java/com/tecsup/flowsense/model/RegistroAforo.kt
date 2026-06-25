package com.tecsup.flowsense.model

data class RegistroAforo(
    val id: String = "",
    val negocioId: String = "",
    val tipo: String = "", // "ENTRADA" o "SALIDA"
    val aforoActual: Int = 0,
    val timestamp: Long = 0L
)
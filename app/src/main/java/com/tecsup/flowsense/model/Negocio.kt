package com.tecsup.flowsense.model

data class Negocio(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val aforoMaximo: Int = 30,
    val aforoActual: Int = 0,
    val apiKey: String = ""
)
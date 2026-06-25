package com.tecsup.flowsense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.ui.admin.AdminScreen
import com.tecsup.flowsense.ui.auth.LoginScreen
import com.tecsup.flowsense.ui.dueño.DueñoScreen
import com.tecsup.flowsense.ui.theme.FlowSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = FirebaseRepository()

        setContent {
            FlowSenseTheme {
                var currentScreen by remember { mutableStateOf("login") }
                var userRol by remember { mutableStateOf("") }
                var userNegocioId by remember { mutableStateOf("") }

                when (currentScreen) {
                    "login" -> LoginScreen(
                        repository = repository,
                        onLoginSuccess = { rol, negocioId ->
                            userRol = rol
                            userNegocioId = negocioId
                            currentScreen = if (rol == "ADMIN") "admin" else "dueno"
                        }
                    )
                    "admin" -> AdminScreen(
                        repository = repository,
                        onLogout = { currentScreen = "login" }
                    )
                    "dueno" -> DueñoScreen(
                        repository = repository,
                        negocioId = userNegocioId,
                        onLogout = { currentScreen = "login" }
                    )
                }
            }
        }
    }
}
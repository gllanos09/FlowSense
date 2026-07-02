package com.tecsup.flowsense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.ui.admin.AdminScreen
import com.tecsup.flowsense.ui.auth.DarkBg
import com.tecsup.flowsense.ui.auth.LoginScreen
import com.tecsup.flowsense.ui.auth.Teal
import com.tecsup.flowsense.ui.dueno.DuenoScreen
import com.tecsup.flowsense.ui.theme.FlowSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Solicitar permisos de notificación en Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val repository = FirebaseRepository(this)

        setContent {
            FlowSenseTheme {
                var currentScreen by remember { mutableStateOf("loading") }
                var userRol by remember { mutableStateOf("") }
                var userNegocioId by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    val session = repository.getLocalSession()
                    if (session != null) {
                        userRol = session.rol
                        userNegocioId = session.negocioId
                        // Si es dueño de Rony, forzar el ID correcto para monitoreo
                        if (session.email == "rony@gmail.com") {
                            userNegocioId = "negocio-rony-123"
                        }
                        currentScreen = if (session.rol == "ADMIN") "admin" else "dueno"
                    } else {
                        currentScreen = "login"
                    }
                }

                when (currentScreen) {
                    "loading" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Teal)
                        }
                    }
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
                    "dueno" -> DuenoScreen(
                        repository = repository,
                        negocioId = userNegocioId,
                        onLogout = { currentScreen = "login" }
                    )
                }
            }
        }
    }
}
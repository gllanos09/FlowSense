package com.tecsup.flowsense.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.repository.FirebaseRepository
import kotlinx.coroutines.launch

val DarkBg = Color(0xFF0A0E1A)
val DarkCard = Color(0xFF111827)
val Teal = Color(0xFF00E5C8)
val TextPrimary = Color(0xFFE8F4F8)
val TextSecondary = Color(0xFF8BA8B8)

@Composable
fun LoginScreen(
    repository: FirebaseRepository,
    onLoginSuccess: (rol: String, negocioId: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Flow",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Sense",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Teal,
                    modifier = Modifier.offset(y = (-20).dp)
                )
                Text(
                    text = "Control de Aforo IoT",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña", color = TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal,
                        unfocusedBorderColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (errorMsg.isNotEmpty()) {
                    Text(text = errorMsg, color = Color.Red, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMsg = ""
                            val result = repository.login(email.trim(), password)
                            if (result.isSuccess) {
                                val uid = result.getOrNull() ?: ""
                                val usuario = repository.getUsuario(uid)
                                if (usuario != null) {
                                    onLoginSuccess(usuario.rol, usuario.negocioId)
                                } else {
                                    errorMsg = "Usuario no encontrado"
                                }
                            } else {
                                errorMsg = "Correo o contraseña incorrectos"
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = DarkBg,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Ingresar",
                            color = DarkBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
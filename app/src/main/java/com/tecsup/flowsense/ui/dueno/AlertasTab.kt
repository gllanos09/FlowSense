package com.tecsup.flowsense.ui.dueno

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Alerta
import com.tecsup.flowsense.ui.auth.Teal
import com.tecsup.flowsense.ui.auth.TextSecondary
import com.tecsup.flowsense.viewmodel.DuenoViewModel

@Composable
fun AlertasTab(viewModel: DuenoViewModel, alertas: List<Alerta>) {
    if (alertas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.VerifiedUser, null, tint = Teal.copy(alpha = 0.2f), modifier = Modifier.size(100.dp))
                Spacer(Modifier.height(16.dp))
                Text("Todo en orden", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("No hay alertas críticas registradas", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Alertas Críticas", color = Color(0xFFFF5252), fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(alertas) { alerta ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF421515)) // Rojo Oscuro
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFFF5252), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alerta.mensaje, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.resolverAlerta(alerta.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("MARCAR COMO ATENDIDO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

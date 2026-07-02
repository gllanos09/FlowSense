package com.tecsup.flowsense.ui.dueno

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.RegistroAforo
import com.tecsup.flowsense.ui.auth.*
import com.tecsup.flowsense.viewmodel.DuenoViewModel

@Composable
fun DashboardTab(viewModel: DuenoViewModel, negocio: Negocio?, registros: List<RegistroAforo>) {
    if (negocio == null) return

    val porcentaje = if (negocio.aforoMaximo > 0)
        negocio.aforoActual.toFloat() / negocio.aforoMaximo.toFloat()
    else 0f

    val colorAforo = when {
        porcentaje >= 1f -> Color(0xFFFF5252) // Rojo Alerta
        porcentaje >= 0.8f -> Color(0xFFFFD700) // Dorado/Naranja
        else -> Teal
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            // Control de Jornada (Botones Iniciar/Terminar)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleDia(true) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!negocio.diaActivo) Teal else Color.Gray.copy(alpha = 0.2f),
                            contentColor = if (!negocio.diaActivo) DarkBg else Color.Gray
                        ),
                        enabled = !negocio.diaActivo,
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("INICIAR DÍA", fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = { viewModel.toggleDia(false) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (negocio.diaActivo) Color(0xFFFF5252) else Color.Gray.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        enabled = negocio.diaActivo,
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Stop, null)
                        Spacer(Modifier.width(8.dp))
                        Text("TERMINAR DÍA", fontWeight = FontWeight.Black)
                    }
                }

                // Estado Visual del Sensor
                Surface(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = (if (negocio.diaActivo) Teal else Color.Red).copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (negocio.diaActivo) Teal else Color.Red, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (negocio.diaActivo) "MONITOREO ACTIVO" else "MONITOREO EN PAUSA",
                            color = if (negocio.diaActivo) Teal else Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        item {
            // Monitor de Aforo Estilo High-Tech
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
                // Brillo de fondo dinámico
                Box(modifier = Modifier.size(220.dp).background(colorAforo.copy(alpha = 0.1f), CircleShape).blur(50.dp))
                
                CircularProgressIndicator(
                    progress = { porcentaje.coerceIn(0f, 1f) },
                    modifier = Modifier.size(250.dp),
                    color = colorAforo,
                    strokeWidth = 14.dp,
                    trackColor = Color.White.copy(alpha = 0.05f),
                    strokeCap = StrokeCap.Round
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${negocio.aforoActual}",
                        fontSize = 84.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-4).sp
                    )
                    Text(
                        text = "PERSONAS DENTRO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextSecondary,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCardPremium("Entradas", "${negocio.totalEntradas}", Teal, Modifier.weight(1f))
                MetricCardPremium("Salidas", "${negocio.totalSalidas}", Color(0xFFF43F5E), Modifier.weight(1f))
            }
        }

        // Alerta de Discrepancia Crítica
        if (negocio.aforoActual > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1515)),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFFF5252), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("REVISIÓN DE SEGURIDAD", fontWeight = FontWeight.Black, color = Color(0xFFFF5252), fontSize = 13.sp)
                            Text("Quedan ${negocio.aforoActual} personas sin registrar salida.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

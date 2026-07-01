package com.tecsup.flowsense.ui.dueno

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.*
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.ui.auth.*
import com.tecsup.flowsense.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DuenoScreen(
    repository: FirebaseRepository,
    negocioId: String,
    onLogout: () -> Unit
) {
    // ── DATA SIMULADA PARA RONY ──
    val esRony = negocioId == "negocio-rony-123"
    val negocioState = if (esRony) {
        remember { mutableStateOf(Negocio(negocioId, "Negocio de Rony", "Calle Lux 777", 0.0, 0.0, 100, 35, "api-rony")) }
    } else {
        repository.observeNegocio(negocioId).collectAsState(initial = null)
    }
    val negocio = negocioState.value

    val registrosState = if (esRony) {
        remember { mutableStateOf(generateDummyRegistros(negocioId)) }
    } else {
        repository.observeRegistrosHoy(negocioId).collectAsState(initial = emptyList())
    }
    val registros = registrosState.value
    // ─────────────────────────────

    val alertas by repository.observeAlertas(negocioId).collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val tabs = listOf(
        Triple("Status", Icons.Default.Dashboard, 0),
        Triple("Log", Icons.Default.History, 1),
        Triple("Alertas", Icons.Default.NotificationsActive, 2),
        Triple("Excel", Icons.Default.Description, 3)
    )

    Column(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        // App Bar con estilo Glass
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkCard,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        negocio?.nombre?.uppercase() ?: "DASHBOARD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Panel de Control",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
                IconButton(
                    onClick = { repository.logout(); onLogout() },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar", tint = Color(0xFFFF5252))
                }
            }
        }

        // Custom Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(DarkCard, RoundedCornerShape(16.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { (label, icon, index) ->
                val isSelected = selectedTab == index
                val bgColor by animateColorAsState(if (isSelected) Teal else Color.Transparent)
                val contentColor by animateColorAsState(if (isSelected) DarkBg else TextSecondary)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { selectedTab = index }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
                        if (isSelected) {
                            Spacer(Modifier.width(8.dp))
                            Text(label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> DashboardTabPremium(negocio, registros)
                1 -> HistorialTabPremium(registros)
                2 -> AlertasTabPremium(alertas, negocioId, repository)
                3 -> ReporteTabPremium(negocio, registros)
            }
        }
    }
}

// Función para generar datos ficticios de 24 horas
fun generateDummyRegistros(negocioId: String): List<RegistroAforo> {
    val list = mutableListOf<RegistroAforo>()
    val now = System.currentTimeMillis()
    // Generar datos cada 2 horas para el gráfico
    for (i in 0..12) {
        val hourMillis = i * 3600000L * 2
        list.add(RegistroAforo("d$i", negocioId, "ENTRADA", (10..40).random(), now - hourMillis))
    }
    return list
}

@Composable
fun DashboardTabPremium(negocio: Negocio?, registros: List<RegistroAforo> = emptyList()) {
    if (negocio == null) return

    val porcentaje = if (negocio.aforoMaximo > 0)
        negocio.aforoActual.toFloat() / negocio.aforoMaximo.toFloat()
    else 0f

    val colorAforo = when {
        porcentaje >= 1f -> Color(0xFFFF5252)
        porcentaje >= 0.85f -> Color(0xFFFFD700)
        else -> Teal
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            // Monitor de Aforo Central
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                Box(modifier = Modifier.size(200.dp).background(colorAforo.copy(alpha = 0.05f), CircleShape).blur(40.dp))
                CircularProgressIndicator(
                    progress = { porcentaje.coerceIn(0f, 1f) },
                    modifier = Modifier.size(230.dp),
                    color = colorAforo,
                    strokeWidth = 16.dp,
                    trackColor = Color.White.copy(alpha = 0.05f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${negocio.aforoActual}", fontSize = 72.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = (-2).sp)
                    Text(text = "OCUPACIÓN ACTUAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                }
            }
        }

        item {
            // Gráfica de Entradas (Custom Designer Chart)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Flujo por Horas (Simulado)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                DesignerBarChart(registros)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard("Disponible", "${negocio.aforoMaximo - negocio.aforoActual}", Teal, Modifier.weight(1f))
                MetricCard("Capacidad", "${negocio.aforoMaximo}", TextSecondary, Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorAforo.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, colorAforo.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if(porcentaje >= 1f) Icons.Default.Warning else Icons.Default.CheckCircle, null, tint = colorAforo)
                    Spacer(Modifier.width(12.dp))
                    Text(text = if(porcentaje >= 1f) "LÍMITE ALCANZADO" else "ESTADO: OPERATIVO", fontWeight = FontWeight.Black, color = colorAforo, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun DesignerBarChart(registros: List<RegistroAforo>) {
    // Tomamos los últimos 7 registros para la gráfica
    val data = registros.sortedBy { it.timestamp }.takeLast(7)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { reg ->
            val barHeight = (reg.aforoActual * 2).dp.coerceAtMost(100.dp)
            val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reg.timestamp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(barHeight)
                        .background(
                            brush = Brush.verticalGradient(listOf(Teal, Teal.copy(alpha = 0.3f))),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(timeLabel, color = TextSecondary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 32.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistorialTabPremium(registros: List<RegistroAforo>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Actividad Reciente", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        items(registros.sortedByDescending { it.timestamp }.take(40)) { registro ->
            val isEntrada = registro.tipo == "ENTRADA"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if(isEntrada) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                        null,
                        tint = if(isEntrada) Teal else Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if(isEntrada) "Ingreso detectado" else "Salida detectada", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Aforo resultante: ${registro.aforoActual}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text(
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(registro.timestamp)),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AlertasTabPremium(alertas: List<Alerta>, negocioId: String, repository: FirebaseRepository) {
    val scope = rememberCoroutineScope()
    if (alertas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.VerifiedUser, null, tint = Teal.copy(alpha = 0.2f), modifier = Modifier.size(80.dp))
                Text("Todo en orden", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(alertas) { alerta ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1515))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(alerta.mensaje, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { scope.launch { repository.resolverAlerta(negocioId, alerta.id) } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("IGNORAR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReporteTabPremium(negocio: Negocio?, registros: List<RegistroAforo>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf("Día") }
    val periods = listOf("Día", "Semana", "Mes")

    val chartData = when (selectedPeriod) {
        "Día" -> listOf("08h" to 0.2f, "12h" to 0.8f, "16h" to 0.6f, "20h" to 0.4f)
        "Semana" -> listOf("Lun" to 0.5f, "Mie" to 0.8f, "Vie" to 0.9f, "Dom" to 0.3f)
        else -> listOf("Sem 1" to 0.7f, "Sem 2" to 0.6f, "Sem 3" to 0.9f, "Sem 4" to 0.5f)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reporte Ejecutivo", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 24.sp)
                
                // Selector de Periodo Mini
                Row(
                    modifier = Modifier.background(DarkCard, RoundedCornerShape(12.dp)).padding(4.dp)
                ) {
                    periods.forEach { period ->
                        val isSelected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) Teal else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedPeriod = period }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(period, color = if (isSelected) DarkBg else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        item {
            // Gráfica de Tendencia
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    chartData.forEach { (label, value) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(value)
                                    .background(
                                        brush = Brush.verticalGradient(listOf(Teal, Teal.copy(alpha = 0.1f))),
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(label, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ReporteRow("Negocio", negocio?.nombre ?: "-")
                    ReporteRow("Periodo", selectedPeriod)
                    ReporteRow("Flujo Estimado", if(selectedPeriod == "Día") "124" else if(selectedPeriod == "Semana") "845" else "3,240")
                    ReporteRow("Pico de Aforo", if(selectedPeriod == "Mes") "98%" else "95%")
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (negocio == null) return@Button
                    scope.launch(Dispatchers.IO) {
                        isExporting = true
                        exportarReporteExcel(context, negocio, registros)
                        isExporting = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                enabled = !isExporting
            ) {
                if (isExporting) CircularProgressIndicator(color = DarkBg)
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileDownload, null, tint = DarkBg)
                    Spacer(Modifier.width(8.dp))
                    Text("EXPORTAR DATA EXCEL", fontWeight = FontWeight.Black, color = DarkBg)
                }
            }
        }
    }
}

@Composable
fun ReporteRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

package com.tecsup.flowsense.ui.dueño

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Alerta
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.RegistroAforo
import com.tecsup.flowsense.model.Usuario
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.ui.auth.DarkBg
import com.tecsup.flowsense.ui.auth.DarkCard
import com.tecsup.flowsense.ui.auth.Teal
import com.tecsup.flowsense.ui.auth.TextPrimary
import com.tecsup.flowsense.ui.auth.TextSecondary
import com.tecsup.flowsense.utils.SkeletonBox
import com.tecsup.flowsense.utils.exportarReporteExcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DueñoScreen(
    repository: FirebaseRepository,
    negocioId: String,
    onLogout: () -> Unit
) {
    val negocio by repository.observeNegocio(negocioId).collectAsState(initial = null)
    val registros by repository.observeRegistrosHoy(negocioId).collectAsState(initial = emptyList())
    val alertas by repository.observeAlertas(negocioId).collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Historial", "Alertas", "Reporte")
    var usuarioActual by remember { mutableStateOf<Usuario?>(null) }

    LaunchedEffect(Unit) {
        usuarioActual = repository.getUsuarioActual()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp)
                .background(DarkCard)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text("FlowSense", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Teal)
                Text(negocio?.nombre ?: "Cargando...", fontSize = 13.sp, color = TextSecondary)
                usuarioActual?.let {
                    Text(it.nombre, fontSize = 11.sp, color = TextSecondary)
                }
            }
            IconButton(
                onClick = {
                    repository.logout()
                    onLogout()
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Cerrar sesion",
                    tint = TextSecondary
                )
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCard,
            contentColor = Teal
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) Teal else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> DashboardTab(negocio)
            1 -> HistorialTab(registros)
            2 -> AlertasTab(alertas, negocioId, repository)
            3 -> ReporteTab(negocio, registros)
        }
    }
}

@Composable
fun DashboardTab(negocio: Negocio?) {
    if (negocio == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(200.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(60.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(80.dp))
        }
        return
    }

    val porcentaje = if (negocio.aforoMaximo > 0)
        negocio.aforoActual.toFloat() / negocio.aforoMaximo.toFloat()
    else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = porcentaje.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "aforoProgress"
    )

    val colorAforo = when {
        porcentaje >= 1f -> Color.Red
        porcentaje >= 0.9f -> Color(0xFFFFD700)
        else -> Teal
    }

    val glowColor = if (porcentaje >= 0.9f) {
        if (porcentaje >= 1f) Color.Red else Color(0xFFFFD700)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Teal.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(200.dp),
                color = colorAforo,
                strokeWidth = 14.dp,
                trackColor = DarkCard,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${negocio.aforoActual}",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = colorAforo
                )
                Text(
                    "de ${negocio.aforoMaximo}",
                    fontSize = 16.sp,
                    color = TextSecondary
                )
                Text(
                    "personas",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    porcentaje >= 1f -> Color(0xFF2D1515)
                    porcentaje >= 0.9f -> Color(0xFF2D2A15)
                    else -> Color(0xFF0D2420)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when {
                        porcentaje >= 1f -> "AFORO MAXIMO ALCANZADO"
                        porcentaje >= 0.9f -> "CASI AL LIMITE (${(porcentaje * 100).toInt()}%)"
                        else -> "AFORO DISPONIBLE (${(porcentaje * 100).toInt()}%)"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorAforo
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Disponible", "${negocio.aforoMaximo - negocio.aforoActual}", Teal, Modifier.weight(1f), glowColor = glowColor)
            StatCard("Maximo", "${negocio.aforoMaximo}", TextSecondary, Modifier.weight(1f), glowColor = glowColor)
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    glowColor: Color? = null
) {
    val transition = rememberInfiniteTransition(label = "glow")
    val animatedBorder by transition.animateColor(
        initialValue = DarkCard,
        targetValue = glowColor ?: DarkCard,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, if (glowColor != null) animatedBorder else DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
fun HistorialTab(registros: List<RegistroAforo>) {
    val entradas = registros.filter { it.tipo == "ENTRADA" }
    val salidas = registros.filter { it.tipo == "SALIDA" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Entradas hoy", "${entradas.size}", Teal, Modifier.weight(1f))
            StatCard("Salidas hoy", "${salidas.size}", Color(0xFFFF6B6B), Modifier.weight(1f))
        }

        Text("Ultimos eventos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(registros.sortedByDescending { it.timestamp }.take(50)) { registro ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (registro.tipo == "ENTRADA") "ENTRADA" else "SALIDA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (registro.tipo == "ENTRADA") Teal else Color(0xFFFF6B6B)
                        )
                        Text(
                            "Aforo: ${registro.aforoActual}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(registro.timestamp)),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertasTab(
    alertas: List<Alerta>,
    negocioId: String,
    repository: FirebaseRepository
) {
    val scope = rememberCoroutineScope()

    if (alertas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sin alertas activas", fontSize = 16.sp, color = TextSecondary)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(alertas) { alerta ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (alerta.nivel == "100%")
                        Color(0xFF2D1515) else Color(0xFF2D2A15)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            alerta.mensaje,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                                .format(Date(alerta.timestamp)),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val btnScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.97f else 1f,
                        animationSpec = tween(120),
                        label = "resolverScale"
                    )

                    TextButton(
                        onClick = {
                            scope.launch {
                                repository.resolverAlerta(negocioId, alerta.id)
                            }
                        },
                        modifier = Modifier.scale(btnScale),
                        interactionSource = interactionSource
                    ) {
                        Text("Resolver", color = Teal, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReporteTab(negocio: Negocio?, registros: List<RegistroAforo>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }
    val entradas = registros.filter { it.tipo == "ENTRADA" }.size
    val salidas = registros.filter { it.tipo == "SALIDA" }.size
    val pico = registros.maxOfOrNull { it.aforoActual } ?: 0

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "buttonScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Reporte del dia", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                fontSize = 14.sp,
                color = TextSecondary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReporteItem("Negocio", negocio?.nombre ?: "-")
                    ReporteItem("Total entradas", "$entradas personas")
                    ReporteItem("Total salidas", "$salidas personas")
                    ReporteItem("Pico maximo", "$pico personas")
                    ReporteItem("Aforo maximo", "${negocio?.aforoMaximo ?: 0} personas")
                    ReporteItem("Total alertas", "${registros.filter { it.aforoActual >= (negocio?.aforoMaximo ?: 0) }.size}")
                }
            }

            Button(
                onClick = {
                    if (negocio == null) return@Button
                    scope.launch(Dispatchers.IO) {
                        isExporting = true
                        val success = exportarReporteExcel(context, negocio, registros)
                        isExporting = false
                        snackbarHostState.showSnackbar(
                            if (success) "Reporte exportado correctamente"
                            else "Error al exportar reporte"
                        )
                    }
                },
                modifier = Modifier
                    .scale(buttonScale)
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                enabled = !isExporting && negocio != null,
                interactionSource = interactionSource
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        color = DarkBg,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Exportar Excel", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun ReporteItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    HorizontalDivider(color = Color(0xFF1F2937))
}

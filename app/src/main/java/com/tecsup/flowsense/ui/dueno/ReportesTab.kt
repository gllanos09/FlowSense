package com.tecsup.flowsense.ui.dueno

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.RegistroAforo
import com.tecsup.flowsense.ui.auth.*

@Composable
fun ReportesTab(negocio: Negocio?, registros: List<RegistroAforo>) {
    var viewState by remember { mutableStateOf("home") }
    var selectedPeriod by remember { mutableStateOf("") }
    var selMonth by remember { mutableStateOf("") }
    var selWeek by remember { mutableStateOf("") }
    var selDay by remember { mutableStateOf("") }

    if (registros.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, null, tint = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.size(80.dp))
                Text("No hay datos registrados para generar informes", color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (viewState != "home") {
            IconButton(onClick = { 
                viewState = when(viewState) {
                    "month" -> "home"
                    "week" -> "month"
                    "day" -> "week"
                    "chart" -> if(selectedPeriod == "Mes") "month" else if(selectedPeriod == "Semana") "week" else "day"
                    else -> "home"
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Teal)
            }
        }

        when (viewState) {
            "home" -> SelectionTypeScreen { period ->
                selectedPeriod = period
                viewState = "month"
            }
            "month" -> SelectionMonthScreen { month ->
                selMonth = month
                viewState = if (selectedPeriod == "Mes") "chart" else "week"
            }
            "week" -> SelectionWeekScreen { week ->
                selWeek = week
                viewState = if (selectedPeriod == "Semana") "chart" else "day"
            }
            "day" -> SelectionDayScreen { day ->
                selDay = day
                viewState = "chart"
            }
            "chart" -> GraficaResultadosInteractive(selectedPeriod, selMonth, selWeek, selDay)
        }
    }
}

@Composable
fun SelectionTypeScreen(onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Generar Informe", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Seleccione el nivel de detalle para el análisis", color = TextSecondary, fontSize = 14.sp)
        
        listOf("Día", "Semana", "Mes").forEach { type ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(type) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, Teal.copy(alpha = 0.1f))
            ) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Teal.copy(alpha = 0.1f), shape = CircleShape) {
                        Icon(Icons.Default.BarChart, null, tint = Teal, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.width(20.dp))
                    Text("Informe por $type", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun SelectionMonthScreen(onSelect: (String) -> Unit) {
    val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    Column {
        Text("Seleccione Mes", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(months) { month ->
                Button(
                    onClick = { onSelect(month) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(60.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(month, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SelectionWeekScreen(onSelect: (String) -> Unit) {
    val weeks = listOf("Semana 1", "Semana 2", "Semana 3", "Semana 4")
    Column {
        Text("Seleccione Semana", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        weeks.forEach { week ->
            Button(
                onClick = { onSelect(week) },
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(week, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SelectionDayScreen(onSelect: (String) -> Unit) {
    val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    Column {
        Text("Seleccione Día", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days) { day ->
                Button(
                    onClick = { onSelect(day) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(day, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GraficaResultadosInteractive(periodo: String, mes: String, semana: String, dia: String) {
    val multiColorBars = listOf(
        Color(0xFF3B82F6), // Azul
        Color(0xFF10B981), // Verde
        Color(0xFFF59E0B), // Ambar
        Color(0xFF8B5CF6), // Violeta
        Color(0xFFEC4899), // Rosa
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF43F5E)  // Rosa-Rojo
    )

    val chartData = when (periodo) {
        "Día" -> listOf("08h" to 0.2f, "12h" to 0.8f, "16h" to 0.6f, "20h" to 0.4f)
        "Semana" -> listOf("Lun" to 0.5f, "Mie" to 0.8f, "Vie" to 0.9f, "Dom" to 0.3f)
        else -> listOf("Sem 1" to 0.7f, "Sem 2" to 0.6f, "Sem 3" to 0.9f, "Sem 4" to 0.5f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Text("Resultado: $periodo", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("$mes ${if(semana.isNotEmpty()) "> $semana" else ""} ${if(dia.isNotEmpty()) "> $dia" else ""}", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(24.dp).fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                chartData.forEachIndexed { index, (label, value) ->
                    val barColor = multiColorBars[index % multiColorBars.size]
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(value)
                                .background(
                                    brush = Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.2f))),
                                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                )
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Button(
            onClick = { /* Export */ },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("DESCARGAR PDF ANALÍTICO", color = DarkBg, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

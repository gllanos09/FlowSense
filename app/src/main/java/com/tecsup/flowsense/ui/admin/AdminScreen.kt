package com.tecsup.flowsense.ui.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.ui.auth.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    repository: FirebaseRepository,
    onLogout: () -> Unit
) {
    val negocios by repository.observeNegocios().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Triple("Negocios", Icons.Default.Business, 0),
        Triple("Nuevo", Icons.Default.AddBusiness, 1),
        Triple("Personal", Icons.Default.PersonAdd, 2),
        Triple("Informes", Icons.Default.BarChart, 3)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
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
                        "ADMIN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Control Center",
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
                    Icon(Icons.Default.Logout, "Cerrar", tint = Color(0xFFFF5252))
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
                        .noRippleClickable { selectedTab = index },
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
                0 -> ListaNegociosTab(negocios)
                1 -> CrearNegocioTab(repository)
                2 -> CrearDuenoTab(repository, negocios)
                3 -> InformesTab()
            }
        }
    }
}

@Composable
fun ListaNegociosTab(negocios: List<Negocio>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(negocios) { index, negocio ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(index * 50L); visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { 20 }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Indicador de Aforo Circular Mini
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                            CircularProgressIndicator(
                                progress = { negocio.aforoActual.toFloat() / negocio.aforoMaximo.toFloat() },
                                color = if (negocio.aforoActual >= negocio.aforoMaximo) Color(0xFFFF5252) else Teal,
                                strokeWidth = 4.dp,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )
                            Text(
                                "${(negocio.aforoActual.toFloat() / negocio.aforoMaximo * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(negocio.nombre, fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 16.sp)
                            Text(negocio.direccion, color = TextSecondary, fontSize = 12.sp)
                        }

                        if (!negocio.synced) {
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "OFFLINE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CrearNegocioTab(repository: FirebaseRepository) {
    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var aforoMax by remember { mutableStateOf("50") }
    var isLoading by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Registrar Negocio", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)

        DesignerInput(value = nombre, onValueChange = { nombre = it }, label = "Nombre del Establecimiento", icon = Icons.Default.Store)
        DesignerInput(value = direccion, onValueChange = { direccion = it }, label = "Ubicación / Dirección", icon = Icons.Default.LocationOn)
        DesignerInput(value = aforoMax, onValueChange = { aforoMax = it }, label = "Capacidad Máxima Permitida", icon = Icons.Default.People)

        if (mensaje.isNotEmpty()) {
            Text(mensaje, color = Teal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val negocio = Negocio(
                        nombre = nombre,
                        direccion = direccion,
                        aforoMaximo = aforoMax.toIntOrNull() ?: 50,
                        apiKey = "fs-${System.currentTimeMillis()}"
                    )
                    repository.crearNegocio(negocio)
                    mensaje = "Registro completado con éxito"
                    nombre = ""; direccion = ""; aforoMax = "50"
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            enabled = !isLoading && nombre.isNotEmpty()
        ) {
            if (isLoading) CircularProgressIndicator(color = DarkBg)
            else Text("FINALIZAR REGISTRO", fontWeight = FontWeight.Black, color = DarkBg)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearDuenoTab(repository: FirebaseRepository, negocios: List<Negocio>) {
    var nombre by remember { mutableStateOf("rony") }
    var email by remember { mutableStateOf("rony@gmail.com") }
    var password by remember { mutableStateOf("123456") }
    var negocioSeleccionado by remember { mutableStateOf<Negocio?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Alta de Personal", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)

        DesignerInput(nombre, { nombre = it }, "Nombre Completo", Icons.Default.Badge)
        DesignerInput(email, { email = it }, "Email Corporativo", Icons.Default.AlternateEmail)
        DesignerInput(password, { password = it }, "Contraseña Temporal", Icons.Default.VpnKey, isPassword = true)

        // Dropdown Estilizado
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = negocioSeleccionado?.nombre ?: "Asignar a Negocio",
                onValueChange = {},
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.Business, null, tint = Teal) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                negocios.forEach { negocio ->
                    DropdownMenuItem(
                        text = { Text(negocio.nombre) },
                        onClick = { negocioSeleccionado = negocio; expanded = false }
                    )
                }
            }
        }

        if (mensaje.isNotEmpty()) Text(mensaje, color = Teal, fontSize = 14.sp)

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    repository.crearUsuarioDueno(email.trim(), password, nombre, negocioSeleccionado?.id ?: "")
                    mensaje = "Usuario creado correctamente"
                    nombre = ""; email = ""; password = ""; negocioSeleccionado = null
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            enabled = !isLoading && email.isNotEmpty() && negocioSeleccionado != null
        ) {
            Text("CONFIRMAR ALTA", fontWeight = FontWeight.Black, color = DarkBg)
        }
    }
}

@Composable
fun InformesTab() {
    var selectedPeriod by remember { mutableStateOf("Día") }
    val periods = listOf("Día", "Semana", "Mes")

    val data = when (selectedPeriod) {
        "Día" -> listOf(
            "08h" to 0.15f, "10h" to 0.45f, "12h" to 0.85f, "14h" to 0.70f,
            "16h" to 0.60f, "18h" to 0.90f, "20h" to 0.40f
        )
        "Semana" -> listOf(
            "Lun" to 0.40f, "Mar" to 0.55f, "Mié" to 0.75f, "Jue" to 0.65f,
            "Vie" to 0.95f, "Sáb" to 0.85f, "Dom" to 0.30f
        )
        else -> listOf(
            "Sem 1" to 0.60f, "Sem 2" to 0.80f, "Sem 3" to 0.45f, "Sem 4" to 0.90f
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reporte de Tráfico", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            
            // Selector de Periodo
            Row(
                modifier = Modifier
                    .background(DarkCard, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                periods.forEach { period ->
                    val isSelected = selectedPeriod == period
                    Text(
                        text = period,
                        modifier = Modifier
                            .background(
                                if (isSelected) Teal else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPeriod = period }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (isSelected) DarkBg else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aforo promedio ($selectedPeriod)", color = TextSecondary, fontSize = 14.sp)
                    Icon(Icons.Default.TrendingUp, null, tint = Teal, modifier = Modifier.size(16.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEach { (label, valor) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(valor)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Teal, Teal.copy(alpha = 0.2f))
                                        ),
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                label, 
                                fontSize = 10.sp, 
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val total = if(selectedPeriod == "Día") "1,240" else if(selectedPeriod == "Semana") "8,680" else "34,720"
            EstadisticaCard("Total Entradas", total, Modifier.weight(1f))
            EstadisticaCard("Pico Aforo", if(selectedPeriod == "Día") "95%" else "98%", Modifier.weight(1f))
        }
    }
}

@Composable
fun EstadisticaCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(value, color = Teal, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

// Helper para el click sin efecto de onda
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

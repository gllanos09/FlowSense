package com.tecsup.flowsense.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.ui.auth.DarkBg
import com.tecsup.flowsense.ui.auth.DarkCard
import com.tecsup.flowsense.ui.auth.Teal
import com.tecsup.flowsense.ui.auth.TextPrimary
import com.tecsup.flowsense.ui.auth.TextSecondary
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
    val tabs = listOf("Negocios", "Crear Negocio", "Crear Dueño")

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
                Text("Panel de Administrador", fontSize = 13.sp, color = TextSecondary)
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
            0 -> ListaNegociosTab(negocios)
            1 -> CrearNegocioTab(repository)
            2 -> CrearDuenoTab(repository, negocios)
        }
    }
}

@Composable
fun ListaNegociosTab(negocios: List<Negocio>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(negocios, key = { _, n -> n.id }) { index, negocio ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 60L)
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { 20 },
                            animationSpec = tween(300)
                        )
            ) {
                val porcentaje = if (negocio.aforoMaximo > 0)
                    negocio.aforoActual.toFloat() / negocio.aforoMaximo.toFloat()
                else 0f

                val indicatorColor = when {
                    porcentaje >= 1f -> Color.Red
                    porcentaje >= 0.9f -> Color(0xFFFFD700)
                    else -> Teal
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(
                                    indicatorColor,
                                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                )
                        )
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text(
                                negocio.nombre,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(negocio.direccion, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Aforo: ${negocio.aforoActual}/${negocio.aforoMaximo}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = indicatorColor
                                )
                                Text(
                                    when {
                                        porcentaje >= 1f -> "LLENO"
                                        porcentaje >= 0.9f -> "CASI LLENO"
                                        else -> "DISPONIBLE"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = indicatorColor
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
    var aforoMax by remember { mutableStateOf("30") }
    var isLoading by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "buttonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Nuevo Negocio", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        Text("Informacion del negocio", fontSize = 12.sp, color = TextSecondary)

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del negocio", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Direccion", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Text("Configuracion", fontSize = 12.sp, color = TextSecondary)

        OutlinedTextField(
            value = aforoMax,
            onValueChange = { aforoMax = it },
            label = { Text("Aforo maximo", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        if (mensaje.isNotEmpty()) {
            Text(
                mensaje,
                color = if (mensaje.startsWith("Error")) Color.Red else Teal,
                fontSize = 14.sp
            )
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val negocio = Negocio(
                        nombre = nombre,
                        direccion = direccion,
                        aforoMaximo = aforoMax.toIntOrNull() ?: 30,
                        apiKey = "fs-${System.currentTimeMillis()}"
                    )
                    val result = repository.crearNegocio(negocio)
                    mensaje = if (result.isSuccess) "Negocio creado correctamente"
                    else "Error al crear negocio"
                    if (result.isSuccess) {
                        nombre = ""
                        direccion = ""
                        aforoMax = "30"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier
                .scale(buttonScale)
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            enabled = !isLoading && nombre.isNotEmpty() && direccion.isNotEmpty(),
            interactionSource = interactionSource
        ) {
            Text("Crear Negocio", color = DarkBg, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearDuenoTab(repository: FirebaseRepository, negocios: List<Negocio>) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var negocioSeleccionado by remember { mutableStateOf<Negocio?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "buttonScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Nuevo Dueño", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        Text("Datos personales", fontSize = 12.sp, color = TextSecondary)

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Text("Credenciales de acceso", fontSize = 12.sp, color = TextSecondary)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electronico", color = TextSecondary) },
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
            label = { Text("Contrasena", color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Text("Asignacion de negocio", fontSize = 12.sp, color = TextSecondary)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = negocioSeleccionado?.nombre ?: "Seleccionar negocio",
                onValueChange = {},
                readOnly = true,
                label = { Text("Negocio asignado", color = TextSecondary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(DarkCard)
            ) {
                negocios.forEach { negocio ->
                    DropdownMenuItem(
                        text = { Text(negocio.nombre, color = TextPrimary) },
                        onClick = {
                            negocioSeleccionado = negocio
                            expanded = false
                        }
                    )
                }
            }
        }

        if (mensaje.isNotEmpty()) {
            Text(
                mensaje,
                color = if (mensaje.startsWith("Error")) Color.Red else Teal,
                fontSize = 14.sp
            )
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val result = repository.crearUsuarioDueno(
                        email = email.trim(),
                        password = password,
                        nombre = nombre,
                        negocioId = negocioSeleccionado?.id ?: ""
                    )
                    mensaje = if (result.isSuccess) "Dueno creado correctamente"
                    else "Error: ${result.exceptionOrNull()?.message}"
                    if (result.isSuccess) {
                        nombre = ""
                        email = ""
                        password = ""
                        negocioSeleccionado = null
                    }
                    isLoading = false
                }
            },
            modifier = Modifier
                .scale(buttonScale)
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            enabled = !isLoading && nombre.isNotEmpty() &&
                    email.isNotEmpty() && password.isNotEmpty() &&
                    negocioSeleccionado != null,
            interactionSource = interactionSource
        ) {
            Text("Crear Dueno", color = DarkBg, fontWeight = FontWeight.Bold)
        }
    }
}

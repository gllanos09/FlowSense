package com.tecsup.flowsense.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionUsuariosTab(viewModel: AdminViewModel, negocios: List<Negocio>) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var negocioSeleccionado by remember { mutableStateOf<Negocio?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Alta de Dueño de Negocio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        
        AdminInput(nombre, { nombre = it }, "Nombre de Usuario", Icons.Default.Person)
        AdminInput(email, { email = it }, "ID / Nickname", Icons.Default.Badge)
        AdminInput(password, { password = it }, "Contraseña (3-6 dígitos)", Icons.Default.VpnKey)

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = negocioSeleccionado?.nombre ?: "Seleccionar Negocio",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AdminPrimary
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

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                viewModel.crearDueno(email, password, nombre, negocioSeleccionado?.id ?: "") {
                    nombre = ""; email = ""; password = ""; negocioSeleccionado = null
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
            enabled = email.isNotEmpty() && password.length >= 3
        ) {
            Text("CREAR CUENTA", color = AdminBg, fontWeight = FontWeight.Black)
        }
    }
}

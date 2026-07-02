package com.tecsup.flowsense.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.viewmodel.AdminViewModel

@Composable
fun GestionNegociosTab(viewModel: AdminViewModel, negocios: List<Negocio>) {
    var showAdd by remember { mutableStateOf(false) }
    
    if (showAdd) {
        FormularioNegocio(onAdd = { nombre, aforo ->
            viewModel.crearNegocio(Negocio(nombre = nombre, aforoMaximo = aforo)) {
                showAdd = false
            }
        }, onBack = { showAdd = false })
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = AdminBg)
                    Spacer(Modifier.width(8.dp))
                    Text("REGISTRAR NUEVO NEGOCIO", fontWeight = FontWeight.Bold, color = AdminBg)
                }
            }
            itemsIndexed(negocios) { _, negocio ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AdminCard),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, null, tint = AdminPrimary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(negocio.nombre, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Aforo Máx: ${negocio.aforoMaximo}", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormularioNegocio(onAdd: (String, Int) -> Unit, onBack: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var aforo by remember { mutableStateOf("30") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
        Text("Nuevo Establecimiento", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        
        AdminInput(nombre, { nombre = it }, "Nombre del Negocio", Icons.Default.Edit)
        AdminInput(aforo, { aforo = it }, "Capacidad Máxima", Icons.Default.Groups)

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onAdd(nombre, aforo.toIntOrNull() ?: 30) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
        ) {
            Text("REGISTRAR", color = AdminBg, fontWeight = FontWeight.Black)
        }
    }
}

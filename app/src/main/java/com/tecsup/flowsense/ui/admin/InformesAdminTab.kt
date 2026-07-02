package com.tecsup.flowsense.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.Usuario
import com.tecsup.flowsense.viewmodel.AdminViewModel

@Composable
fun InformesAdminTab(viewModel: AdminViewModel, negocios: List<Negocio>) {
    val usuarios by viewModel.usuarios.collectAsState()
    var usuarioParaEditar by remember { mutableStateOf<Usuario?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Resumen
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AdminCard),
            border = BorderStroke(1.dp, AdminPrimary.copy(alpha = 0.2f))
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, null, tint = AdminPrimary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Total Usuarios", color = Color.Gray, fontSize = 12.sp)
                    Text("${usuarios.size} Cuentas Activas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Gestión de Usuarios", color = AdminPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(usuarios) { usuario ->
                UsuarioItem(
                    usuario = usuario,
                    onEdit = { usuarioParaEditar = usuario },
                    onDelete = { viewModel.eliminarUsuario(usuario.id) }
                )
            }
        }
    }

    if (usuarioParaEditar != null) {
        DialogoEditarUsuario(
            usuario = usuarioParaEditar!!,
            negocios = negocios,
            onDismiss = { usuarioParaEditar = null },
            onConfirm = { nombre, aforo ->
                viewModel.actualizarPerfil(
                    usuarioParaEditar!!.id,
                    nombre,
                    usuarioParaEditar!!.negocioId,
                    aforo
                )
                usuarioParaEditar = null
            }
        )
    }
}

@Composable
fun UsuarioItem(usuario: Usuario, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminCard.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(usuario.nombre, color = Color.White, fontWeight = FontWeight.Bold)
                Text(usuario.rol, color = AdminPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = AdminPrimary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) }
        }
    }
}

@Composable
fun DialogoEditarUsuario(
    usuario: Usuario,
    negocios: List<Negocio>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var nombre by remember { mutableStateOf(usuario.nombre) }
    val negocio = negocios.find { it.id == usuario.negocioId }
    var aforo by remember { mutableStateOf(negocio?.aforoMaximo?.toString() ?: "30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminCard,
        title = { Text("Editar Perfil", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminInput(nombre, { nombre = it }, "Nombre de Perfil", Icons.Default.Person)
                if (usuario.rol == "DUENO") {
                    AdminInput(aforo, { aforo = it }, "Límite Aforo", Icons.Default.Groups)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nombre, aforo.toIntOrNull() ?: 30) }) {
                Text("GUARDAR", color = AdminPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray)
            }
        }
    )
}

package com.tecsup.flowsense.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colores Premium para el Admin
val AdminPrimary = Color(0xFF3B82F6) // Azul Material
val AdminBg = Color(0xFF0F172A) // Slate 900
val AdminCard = Color(0xFF1E293B) // Slate 800

@Composable
fun AdminInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, tint = AdminPrimary, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AdminPrimary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = AdminCard.copy(alpha = 0.5f),
            unfocusedContainerColor = AdminCard.copy(alpha = 0.3f)
        )
    )
}

package com.tecsup.flowsense.ui.dueno

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.model.RegistroAforo
import com.tecsup.flowsense.ui.auth.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistorialTab(registros: List<RegistroAforo>) {
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

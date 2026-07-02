package com.tecsup.flowsense.ui.dueno

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.ui.auth.*
import com.tecsup.flowsense.viewmodel.DuenoViewModel
import com.tecsup.flowsense.viewmodel.ViewModelFactory

@Composable
fun DuenoScreen(
    repository: FirebaseRepository,
    negocioId: String,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = ViewModelFactory(repository, context, negocioId)
    val viewModel: DuenoViewModel = viewModel(factory = factory)
    
    val negocio by viewModel.negocio.collectAsState()
    val registros by viewModel.registros.collectAsState()
    val alertas by viewModel.alertas.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val tabs = listOf(
        Triple("Status", Icons.Default.Dashboard, 0),
        Triple("Log", Icons.Default.History, 1),
        Triple("Alertas", Icons.Default.NotificationsActive, 2),
        Triple("Informe", Icons.Default.Analytics, 3)
    )

    Column(modifier = Modifier
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
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp), // Ajustado vertical
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
                    Text("Panel de Control", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }
                IconButton(
                    onClick = { viewModel.logout(onLogout) },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
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
                0 -> DashboardTab(viewModel, negocio, registros)
                1 -> HistorialTab(registros)
                2 -> AlertasTab(viewModel, alertas)
                3 -> ReportesTab(negocio, registros)
            }
        }
    }
}

package com.tecsup.flowsense.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.flowsense.repository.FirebaseRepository
import com.tecsup.flowsense.viewmodel.AdminViewModel
import com.tecsup.flowsense.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    repository: FirebaseRepository,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = ViewModelFactory(repository, context, "")
    val viewModel: AdminViewModel = viewModel(factory = factory)
    val negocios by viewModel.negocios.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Triple("Negocios", Icons.Default.Storefront, 0),
        Triple("Usuarios", Icons.Default.Group, 1),
        Triple("Informe", Icons.Default.Analytics, 2)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBg)
    ) {
        // App Bar sobria para el Admin
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AdminCard,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp), // Ajustado vertical
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PANEL DE CONTROL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdminPrimary, letterSpacing = 1.sp)
                    Text("Administrador", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                IconButton(onClick = { viewModel.logout(onLogout) }) {
                    Icon(Icons.Default.PowerSettingsNew, "Salir", tint = Color(0xFFFF5252))
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AdminCard,
            contentColor = AdminPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AdminPrimary
                )
            }
        ) {
            tabs.forEach { (label, icon, index) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label, fontSize = 12.sp) },
                    icon = { Icon(icon, null, modifier = Modifier.size(20.dp)) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> GestionNegociosTab(viewModel, negocios)
                1 -> GestionUsuariosTab(viewModel, negocios)
                2 -> InformesAdminTab(viewModel, negocios)
            }
        }
    }
}

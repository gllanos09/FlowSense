package com.tecsup.flowsense.ui.dueno

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.flowsense.ui.auth.DarkCard
import com.tecsup.flowsense.ui.auth.TextSecondary

@Composable
fun MetricCardPremium(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(20.dp), spotColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = (-1).sp
            )
            Text(
                label.uppercase(),
                fontSize = 9.sp,
                color = TextSecondary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

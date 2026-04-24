package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagChip(
    label: String,
    background: Color,
    foreground: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = foreground,
        modifier = modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

object TagColors {
    val VitalSign = Color(0xFFE0D4FF) to Color(0xFF4B2A99)
    val Medication = Color(0xFFD9C9FF) to Color(0xFF4B2A99)
    val Treatment = Color(0xFFCFE2FF) to Color(0xFF0B4AA8)
    val Diet = Color(0xFFCEDEEA) to Color(0xFF234E70)
    val Observation = Color(0xFFEAE2C8) to Color(0xFF7A5A00)
    val Excretion = Color(0xFFFFE0C2) to Color(0xFF8A4B00)
    val Sleep = Color(0xFFD8E4FF) to Color(0xFF1F3D99)
    val Activity = Color(0xFFD0F0D8) to Color(0xFF1B5E20)

    val OrderRx = Color(0xFFE0D4FF) to Color(0xFF4B2A99)
    val OrderFluid = Color(0xFFCFE2FF) to Color(0xFF0B4AA8)
    val OrderProcedure = Color(0xFFD0F0D8) to Color(0xFF1B5E20)
    val OrderLIS = Color(0xFFFFE0C2) to Color(0xFF8A4B00)
    val OrderImaging = Color(0xFFFFD6D6) to Color(0xFF8A1F1F)

    val StatusInProgress = Color(0xFFCFE2FF) to Color(0xFF0B4AA8)
    val StatusPrescribed = Color(0xFFE5E5E5) to Color(0xFF444444)
    val StatusDone = Color(0xFFD0F0D8) to Color(0xFF1B5E20)
    val StatusHold = Color(0xFFFFD6D6) to Color(0xFF8A1F1F)
}

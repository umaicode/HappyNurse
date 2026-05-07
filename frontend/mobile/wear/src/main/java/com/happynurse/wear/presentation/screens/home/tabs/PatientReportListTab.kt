// PatientReportListTab — 홈 환자알림 탭. 환자 자가증상 보고 카드 리스트.
package com.happynurse.wear.presentation.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.data.model.PatientSelfReport
import com.happynurse.wear.data.model.SymptomType
import com.happynurse.wear.presentation.components.HnSwipeToDeleteCard
import com.happynurse.wear.presentation.theme.HnIvBlue
import com.happynurse.wear.presentation.theme.HnPatientBg
import com.happynurse.wear.presentation.theme.HnSttOrange

@Composable
fun PatientReportListTab(
    items: List<PatientSelfReport>,
    onCardClick: (PatientSelfReport) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyMessage(text = "환자 알림이 없어요", modifier = modifier)
        return
    }
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items, key = { it.selfReportId }) { req ->
            HnSwipeToDeleteCard(
                onDelete = { onDelete(req.selfReportId) },
                onClick = { onCardClick(req) },
            ) {
                ReqCardContent(req)
            }
        }
    }
}

@Composable
private fun ReqCardContent(req: PatientSelfReport) {
    val (icon, color) = when (req.symptomType) {
        SymptomType.PAIN -> Icons.Filled.LocalHospital to HnPatientBg
        SymptomType.DRESSING -> Icons.Filled.Healing to HnSttOrange
        SymptomType.IV -> Icons.Filled.WaterDrop to HnIvBlue
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SymptomIcon(icon, color)
            Spacer(Modifier.width(6.dp))
            Text(
                text = req.patientName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = req.submittedRelative,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = req.symptomText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
        )
        Text(
            text = req.patientRoomBed,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SymptomIcon(icon: ImageVector, bg: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
    }
}

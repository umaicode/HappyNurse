// NFC 인식 화면 — 인식된 환자 정보 + 다음 작업(간호일지/약물 등록) 선택
package com.happynurse.presentation.screens.nfc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.core.sample.SampleData
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors

@Composable
fun NfcPatientScreen(
    onClose: () -> Unit,
    onLog: () -> Unit,
    onDrug: () -> Unit,
) {
    val p = SampleData.patients.first()
    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "닫기",
                modifier = Modifier.size(28.dp).clickable(onClick = onClose),
            )
            Spacer(Modifier.size(8.dp))
            Text("NFC 인식", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            HnCard(padding = 20.dp) {
                Column {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(HnColors.TagPillBg),
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = HnColors.Success, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("인식 완료", fontSize = 13.sp, color = HnColors.Success, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Text(p.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Text("${p.birthdate} · ${p.sex} · ${p.age}세", fontSize = 13.sp, color = HnColors.TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(HnColors.SurfaceAlt).padding(12.dp),
                    ) {
                        Column {
                            Info("병동", p.ward)
                            Info("호실", "${p.room}호 ${p.bed}번 침대")
                            Info("MRN", p.mrn)
                            Info("진료의", "${p.doctor} 의사")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("다음 작업을 선택하세요", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            ActionTile(Icons.Outlined.Mic, "간호일지 등록", "음성 녹음 → STT → 전송", onLog)
            Spacer(Modifier.height(8.dp))
            ActionTile(Icons.Outlined.MedicalServices, "약물 등록", "약물 NFC 태깅 → 리스트 → 전송", onDrug)
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = HnColors.TextTertiary, modifier = Modifier.size(width = 56.dp, height = 18.dp))
        Text(value, fontSize = 13.sp, color = HnColors.Text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionTile(icon: ImageVector, title: String, desc: String, onClick: () -> Unit) {
    HnCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(HnColors.PrimarySoft),
            ) { Icon(icon, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Text(desc, fontSize = 12.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = HnColors.TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

// NFC 인식 화면 — wristband 태깅 → 환자 정보 로드 → 다음 작업(간호일지/약물 등록) 선택
package com.happynurse.presentation.screens.nfc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.domain.model.NfcPatientInfo
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors

@Composable
fun NfcPatientScreen(
    token: String? = null,
    onClose: () -> Unit,
    onLog: (patientId: Long, encounterId: Long) -> Unit,
    onDrug: (patientId: Long, encounterId: Long) -> Unit,
    onIv: () -> Unit,
    viewModel: NfcPatientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Path A — manifest dispatch 로 들어온 token 을 화면 진입 시 1회 자동 스캔.
    LaunchedEffect(token) {
        if (token != null) viewModel.onTokenScanned(token)
    }

    NfcReaderEffect(viewModel)

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
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            when (val s = state) {
                NfcPatientViewModel.State.Idle -> IdleCard()
                NfcPatientViewModel.State.Loading -> LoadingCard()
                is NfcPatientViewModel.State.Success -> SuccessSection(
                    info = s.info,
                    onLog = { onLog(s.info.patientId, s.info.encounterId) },
                    onDrug = { onDrug(s.info.patientId, s.info.encounterId) },
                    onIv = onIv,
                )
                is NfcPatientViewModel.State.Error -> ErrorCard(s.message, onRetry = viewModel::reset)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun IdleCard() {
    HnCard(padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).clip(CircleShape).background(HnColors.PrimarySoft),
            ) {
                Icon(Icons.Outlined.Nfc, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("환자 손목띠를 휴대폰에 태깅해 주세요", fontSize = 13.sp, color = HnColors.TextSecondary)
        }
    }
}

@Composable
private fun LoadingCard() {
    HnCard(padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = HnColors.Primary)
            Spacer(Modifier.height(14.dp))
            Text("환자 정보를 불러오는 중입니다", fontSize = 13.sp, color = HnColors.TextSecondary)
        }
    }
}

@Composable
private fun SuccessSection(
    info: NfcPatientInfo,
    onLog: () -> Unit,
    onDrug: () -> Unit,
    onIv: () -> Unit,
) {
    HnCard(padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).clip(CircleShape).background(HnColors.TagPillBg),
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = HnColors.Success, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("인식 완료", fontSize = 13.sp, color = HnColors.Success, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(info.patientName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            info.roomName?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, fontSize = 12.sp, color = HnColors.TextSecondary)
            }
            info.diseaseName?.let {
                Spacer(Modifier.height(6.dp))
                Text("병명: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
            info.chiefComplaint?.let {
                Spacer(Modifier.height(2.dp))
                Text("주증상: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
            info.surgeryName?.let {
                Spacer(Modifier.height(2.dp))
                Text("수술: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
            info.attendingPhysicianName?.let {
                Spacer(Modifier.height(2.dp))
                Text("담당의: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("다음 작업을 선택하세요", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
    Spacer(Modifier.height(10.dp))
    ActionTile(Icons.Outlined.Mic, "간호일지 등록", "음성 녹음 → STT → 전송", onLog)
    Spacer(Modifier.height(8.dp))
    ActionTile(Icons.Outlined.MedicalServices, "약물 등록", "약물 NFC 태깅 → 리스트 → 전송", onDrug)
    Spacer(Modifier.height(8.dp))
    ActionTile(Icons.Outlined.WaterDrop, "수액 확인", "진행 중 수액 → NFC 재태깅", onIv)
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    HnCard(padding = 20.dp, onClick = onRetry) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(14.dp))
            Text(message, fontSize = 13.sp, color = HnColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("탭하여 다시 시도", fontSize = 12.sp, color = HnColors.Primary, fontWeight = FontWeight.SemiBold)
        }
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

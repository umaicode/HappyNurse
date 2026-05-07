// 간호일지 등록 — 녹음 → STT 결과 표시 → 웹 전송 (실제 STT 연결 전, 시뮬레이션 단계)
package com.happynurse.presentation.screens.logentry

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnButtonVariant
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.TagChip
import com.happynurse.presentation.theme.HnColors
import kotlinx.coroutines.delay

@Composable
fun LogEntryScreen(
    patientId: Long,
    encounterId: Long,
    onClose: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(patientId, encounterId) {
        android.util.Log.d("LogEntryScreen", "received patientId=$patientId encounterId=$encounterId")
    }
    var stage by remember { mutableIntStateOf(0) } // 0 idle, 1 recording, 2 result, 3 sending, 4 done
    var seconds by remember { mutableIntStateOf(0) }
    val text = "환자 김가민, 14시 30분. 보행 시도 중 어지러움 호소하여 즉시 침상 안정 시킴. 혈압 118 76, 맥박 82, 통증 NRS 2점으로 감소 확인."

    LaunchedEffect(stage) {
        if (stage == 1) {
            seconds = 0
            while (stage == 1) { delay(1000); seconds++ }
        }
        if (stage == 3) {
            delay(1500); stage = 4; delay(1800); onClose()
        }
    }

    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "뒤로",
                modifier = Modifier.size(28.dp).clickable(onClick = onClose),
            )
            Spacer(Modifier.size(8.dp))
            Text("간호일지 등록", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            HnCard(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("김가민", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                        Text("7W 701호 1번 침대", fontSize = 12.sp, color = HnColors.TextSecondary)
                    }
                    TagChip("NFC 인식됨", fg = HnColors.Success, bg = HnColors.TagPillBg)
                }
            }
        }

        if (stage <= 1) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(20.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp).clip(CircleShape)
                        .background(if (stage == 1) HnColors.Danger else HnColors.Primary)
                        .clickable { stage = if (stage == 0) 1 else 2 },
                ) {
                    Icon(
                        if (stage == 1) Icons.Outlined.Stop else Icons.Outlined.Mic,
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                if (stage == 1) {
                    Text(
                        formatSec(seconds),
                        fontSize = 32.sp, fontWeight = FontWeight.Bold, color = HnColors.Danger,
                    )
                    Text("녹음 중 · 정지 버튼을 누르세요", fontSize = 13.sp, color = HnColors.TextSecondary)
                } else {
                    Text("버튼을 눌러 녹음을 시작하세요", fontSize = 15.sp, color = HnColors.TextSecondary)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("STT 변환 결과", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
                HnCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Row {
                            Text("김가민 · 14:30", fontSize = 12.sp, color = HnColors.TextTertiary, modifier = Modifier.weight(1f))
                            Text("녹음 ${formatSec(if (seconds > 0) seconds else 47)}", fontSize = 12.sp, color = HnColors.TextTertiary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(text, fontSize = 14.sp, color = HnColors.Text)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HnButton(
                        text = "다시 녹음",
                        variant = HnButtonVariant.SECONDARY,
                        full = true,
                        onClick = { stage = 0; seconds = 0 },
                        modifier = Modifier.weight(1f),
                    )
                    HnButton(
                        text = if (stage == 4) "전송 완료" else "웹 전송",
                        icon = Icons.Outlined.Send,
                        full = true,
                        loading = stage == 3,
                        enabled = stage == 2,
                        onClick = { stage = 3 },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun formatSec(s: Int) = "%02d:%02d".format(s / 60, s % 60)

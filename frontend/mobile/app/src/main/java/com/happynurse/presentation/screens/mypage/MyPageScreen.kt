// 마이페이지 탭 — 프로필 카드 + 담당 환자 리스트 + 로그아웃 버튼
package com.happynurse.presentation.screens.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.core.sample.SampleData
import com.happynurse.domain.model.Patient
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.PageHeader
import com.happynurse.presentation.components.TagChip
import com.happynurse.presentation.theme.HnColors

@Composable
fun MyPageScreen(
    onLogout: () -> Unit,
    onOpenPatient: (String) -> Unit,
) {
    val mine = SampleData.patients.filter { it.nurse == "김소연" }
    Column(Modifier.fillMaxWidth()) {
        PageHeader(title = "마이페이지")
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            item {
                HnCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("김소연", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                            Text("삼성서울병원", fontSize = 12.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
                            Text("7W 병동 · 일반간호사", fontSize = 15.sp, color = HnColors.TextSecondary)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFDF4F4))
                                .clickable(onClick = onLogout),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "로그아웃", tint = HnColors.Danger, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                    Text("담당 환자 ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Text("${mine.size}명", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
                }
            }
            items(mine, key = { it.id }) { p -> MyPatientRow(p, onClick = { onOpenPatient(p.id) }) }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun MyPatientRow(p: Patient, onClick: () -> Unit) {
    HnCard(onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Spacer(Modifier.size(6.dp))
                    TagChip("${p.room}-${p.bed}", fg = HnColors.Primary, bg = HnColors.PrimarySoft)
                    Spacer(Modifier.size(4.dp))
                    TagChip("${p.sex} · ${p.age}세")
                }
                Text("${p.chief} · D+${p.daysSince}", fontSize = 12.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 3.dp))
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = HnColors.TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

// 의사 오더 카드 — 탭하면 상세 내용이 펼쳐지는 토글식 카드
package com.happynurse.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class OrderCategory(val label: String, val colors: Pair<Color, Color>) {
    Rx("지시", TagColors.OrderRx),
    Fluid("수액", TagColors.OrderFluid),
    Procedure("처치", TagColors.OrderProcedure),
    LIS("LIS", TagColors.OrderLIS),
    Imaging("영상", TagColors.OrderImaging)
}

enum class OrderStatus(val label: String, val colors: Pair<Color, Color>) {
    InProgress("진행중", TagColors.StatusInProgress),
    Prescribed("처방", TagColors.StatusPrescribed),
    Done("완료", TagColors.StatusDone),
    Hold("보류", TagColors.StatusHold)
}

data class DoctorOrder(
    val category: OrderCategory,
    val status: OrderStatus,
    val code: String,
    val name: String,
    val dose: String,
    val count: String,
    val unit: String,
    val days: String,
    val route: String,
    val usage: String,
    val note: String
)

@Composable
fun OrderCard(order: DoctorOrder, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFC))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagChip(
                    label = order.category.label,
                    background = order.category.colors.first,
                    foreground = order.category.colors.second
                )
                Spacer(Modifier.width(6.dp))
                TagChip(
                    label = order.status.label,
                    background = order.status.colors.first,
                    foreground = order.status.colors.second
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(order.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("처방코드: ${order.code}", fontSize = 12.sp, color = Color(0xFF666666))

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color(0xFFEAEAF0))

            OrderRow("1회 투여량", order.dose, "횟수", order.count)
            OrderRow("단위", order.unit, "일수", order.days)
            OrderRow("조제", order.route, "용법", order.usage)

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color(0xFFEAEAF0))

            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF666666)
                )
                Text("참고사항", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    order.note,
                    fontSize = 12.sp,
                    color = Color(0xFF444444),
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun OrderRow(label1: String, value1: String, label2: String, value2: String) {
    Row(Modifier.fillMaxWidth()) {
        OrderField(label1, value1, Modifier.weight(1f))
        OrderField(label2, value2, Modifier.weight(1f))
    }
}

@Composable
private fun OrderField(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", fontSize = 12.sp, color = Color(0xFF888888))
        Text(value, fontSize = 12.sp, color = Color(0xFF222222), fontWeight = FontWeight.Medium)
    }
}

// 환자 탭 — 내 담당환자/전체환자 토글, 호실별 그룹, NFC FAB (실 API 연동)
package com.happynurse.presentation.screens.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.domain.model.Patient
import com.happynurse.presentation.components.NotifBell
import com.happynurse.presentation.components.PageHeader
import com.happynurse.presentation.components.PatientCard
import com.happynurse.presentation.components.PatientLayout
import com.happynurse.presentation.theme.HnColors

@Composable
fun PatientsScreen(
    onOpenPatient: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    upcomingCount: Int,
    layout: PatientLayout = PatientLayout.CARD,
    vm: PatientsViewModel = hiltViewModel(),
) {
    val all by vm.patients.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val myName = profile?.name ?: ""
    var listTab by remember { mutableStateOf("mine") }
    var pickerOpen by remember { mutableStateOf(false) }
    val mine = all.filter { it.isMyPatient }

    // wristband NFC 진입은 manifest dispatch + autoVerify 로 자동 처리되므로 화면 내 별도 FAB 불필요.
    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = "환자",
            sub = "2026년 4월 30일 (목) · 데이 근무",
            right = { NotifBell(unreadCount = upcomingCount, onClick = onOpenNotifications) },
        )
        TabRow(
            active = listTab,
            onChange = { listTab = it },
            onOpenPicker = { pickerOpen = true },
        )
        val showing: List<Patient> = if (listTab == "mine") mine else all

        if (showing.isEmpty()) {
            EmptyState(onOpenPicker = { pickerOpen = true })
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp,
                ),
            ) {
                val rooms = showing.map { it.room }.distinct().sorted()
                rooms.forEach { room ->
                    val inRoom = showing.filter { it.room == room }
                    item(key = "h-$room") {
                        RoomHeader(room = room, count = inRoom.size)
                    }
                    items(inRoom, key = { it.id }) { p ->
                        PatientCard(
                            patient = p,
                            onClick = { onOpenPatient(p.id) },
                            layout = layout,
                            myNurseName = myName,
                        )
                    }
                }
            }
        }
    }

    if (pickerOpen) {
        AssignedPatientPickerDialog(
            allPatients = all,
            initialSelection = all.filter { it.isMyPatient }.map { it.encounterId }.toSet(),
            onDismiss = { pickerOpen = false },
            onConfirm = { newSelection ->
                vm.saveAssignment(newSelection)
                pickerOpen = false
            },
        )
    }
}

@Composable
private fun TabRow(
    active: String,
    onChange: (String) -> Unit,
    onOpenPicker: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        listOf("mine" to "내 담당환자", "all" to "전체 환자").forEach { (id, label) ->
            val on = id == active
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clickable { onChange(id) },
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = if (on) HnColors.Primary else HnColors.TextSecondary,
                        )
                        if (id == "mine") {
                            Spacer(Modifier.size(4.dp))
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "담당환자 선택",
                                tint = if (on) HnColors.Primary else HnColors.TextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(onClick = onOpenPicker),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (on) HnColors.Primary else Color.Transparent),
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(HnColors.Border))
}

@Composable
private fun RoomHeader(room: String, count: Int) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "${room}호실 · ${count}명",
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = HnColors.TextTertiary, letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(HnColors.Border))
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun EmptyState(onOpenPicker: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(HnColors.PrimarySoft),
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("담당 환자가 없습니다", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        Spacer(Modifier.height(8.dp))
        Text(
            "톱니바퀴를 눌러 담당 환자를 선택해 주세요.",
            fontSize = 14.sp, color = HnColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(HnColors.Primary)
                .clickable(onClick = onOpenPicker)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text("담당환자 선택", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AssignedPatientPickerDialog(
    allPatients: List<Patient>,
    initialSelection: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("담당 환자 선택", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(allPatients, key = { it.encounterId }) { p ->
                    val checked = p.encounterId in selection
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selection = if (checked) selection - p.encounterId else selection + p.encounterId
                            }
                            .padding(vertical = 4.dp),
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selection = if (it) selection + p.encounterId else selection - p.encounterId
                            },
                            colors = CheckboxDefaults.colors(checkedColor = HnColors.Primary),
                        )
                        Spacer(Modifier.size(4.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
                                Spacer(Modifier.size(6.dp))
                                Text("${p.sex}/${p.age}", fontSize = 12.sp, color = HnColors.TextSecondary)
                            }
                            Text(
                                "${p.room}호 ${p.bed}번 침대",
                                fontSize = 12.sp,
                                color = HnColors.TextTertiary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selection) }) {
                Text("확인", color = HnColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = HnColors.TextSecondary)
            }
        },
        containerColor = HnColors.Surface,
    )
}

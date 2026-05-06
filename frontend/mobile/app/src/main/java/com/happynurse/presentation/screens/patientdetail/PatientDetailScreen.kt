// 환자 상세 — 환자 정보 카드(접기/펼치기), 간호일지/의사오더 서브탭
package com.happynurse.presentation.screens.patientdetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.domain.model.Note
import com.happynurse.domain.model.Order
import com.happynurse.domain.model.OrderKind
import com.happynurse.domain.model.Patient
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.TagChip
import com.happynurse.presentation.theme.HnColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
    onSelectPatient: (String) -> Unit = {},
    vm: PatientDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(patientId) {
        patientId.toLongOrNull()?.let { vm.loadPatient(it) }
    }
    val p: Patient = vm.patient.collectAsStateWithLifecycle().value ?: return
    val notes by vm.notes.collectAsStateWithLifecycle()
    val orders by vm.orders.collectAsStateWithLifecycle()
    val myPatients by vm.myPatients.collectAsStateWithLifecycle()
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("notes") }
    var patientMenuOpen by remember { mutableStateOf(false) }
    var calendarOpen by remember { mutableStateOf(false) }

    val groupedOrders = remember(orders) {
        orders.groupBy { it.dateWritten }.toSortedMap(compareByDescending { it })
    }

    Column(Modifier.fillMaxWidth().background(HnColors.Bg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "뒤로",
                modifier = Modifier.size(28.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.size(4.dp))
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { patientMenuOpen = true }.padding(vertical = 4.dp),
                ) {
                    Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${p.sex}/${p.age}",
                        fontSize = 14.sp,
                        color = HnColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.size(2.dp))
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = "환자 선택",
                        tint = HnColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = patientMenuOpen,
                    onDismissRequest = { patientMenuOpen = false },
                ) {
                    myPatients.forEach { other ->
                        val current = other.id == p.id
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                other.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (current) HnColors.Primary else HnColors.Text,
                                            )
                                            Spacer(Modifier.size(6.dp))
                                            Text(
                                                "${other.sex}/${other.age}",
                                                fontSize = 12.sp,
                                                color = HnColors.TextSecondary,
                                            )
                                        }
                                        Text(
                                            "${other.room}호 ${other.bed}번 침대",
                                            fontSize = 11.sp,
                                            color = HnColors.TextTertiary,
                                        )
                                    }
                                    if (current) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = HnColors.Primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            },
                            onClick = {
                                patientMenuOpen = false
                                if (!current) onSelectPatient(other.id)
                            },
                        )
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            item {
                HnCard(padding = 14.dp) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("MRN: ${p.mrn}", fontSize = 13.sp, color = HnColors.TextSecondary)
                                Text("${p.room}호 ${p.bed}번 침대", fontSize = 13.sp, color = HnColors.TextSecondary)
                            }
                            Icon(
                                Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = HnColors.TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        if (expanded) {
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(HnColors.Border))
                            Spacer(Modifier.height(12.dp))
                            InfoRow("생년월일", p.birthdate)
                            InfoRow("진료부서", p.department)
                            InfoRow("담당의", "${p.doctor} 의사")
                            InfoRow("주증상", p.chief)
                            InfoRow("수술", p.surgery)
                            Spacer(Modifier.height(8.dp))
                            Text("메모", fontSize = 12.sp, color = HnColors.TextTertiary)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HnColors.SurfaceAlt)
                                    .padding(10.dp),
                            ) { Text(p.memo, fontSize = 13.sp, color = HnColors.Text) }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().background(HnColors.Surface, RoundedCornerShape(10.dp))) {
                    listOf("notes" to "간호일지", "orders" to "의사오더").forEach { (id, label) ->
                        val on = id == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clickable { tab = id }.height(44.dp),
                        ) {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (on) HnColors.Primary else HnColors.TextSecondary,
                                )
                            }
                            Box(
                                Modifier.fillMaxWidth().height(2.dp)
                                    .background(if (on) HnColors.Primary else Color.Transparent),
                            )
                        }
                    }
                }
            }
            if (tab == "notes") {
                item {
                    DateSelectorBar(
                        selectedDate = selectedDate,
                        count = notes.size,
                        calendarOpen = calendarOpen,
                        onPrev = { vm.setDate(selectedDate.minusDays(1)) },
                        onNext = { vm.setDate(selectedDate.plusDays(1)) },
                        onToggleCalendar = { calendarOpen = !calendarOpen },
                    )
                }
                if (calendarOpen) {
                    item {
                        InlineCalendar(
                            selectedDate = selectedDate,
                            onPick = {
                                vm.setDate(it)
                                calendarOpen = false
                            },
                        )
                    }
                }
                if (notes.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "해당 날짜의 간호일지가 없습니다.",
                                fontSize = 13.sp,
                                color = HnColors.TextTertiary,
                            )
                        }
                    }
                } else {
                    items(notes) { NoteRow(it) }
                }
            } else {
                groupedOrders.forEach { (date, list) ->
                    item { OrderDateHeader(date, list.size) }
                    items(list) { OrderRow(it) }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = HnColors.TextTertiary, modifier = Modifier.size(width = 64.dp, height = 18.dp))
        Text(value, fontSize = 14.sp, color = HnColors.Text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DateSelectorBar(
    selectedDate: LocalDate,
    count: Int,
    calendarOpen: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleCalendar: () -> Unit,
) {
    val isToday = selectedDate == LocalDate.now()
    val dow = selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    val label = "${selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))} ($dow)"

    HnCard(padding = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "이전 날짜",
                tint = HnColors.TextSecondary,
                modifier = Modifier.size(28.dp).clickable(onClick = onPrev),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleCalendar)
                    .padding(vertical = 6.dp),
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = HnColors.Text,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                if (isToday) {
                    Spacer(Modifier.size(6.dp))
                    TagChip("오늘", fg = HnColors.Success, bg = HnColors.TagPillBg)
                }
                Spacer(Modifier.size(6.dp))
                Text("${count}건", fontSize = 12.sp, color = HnColors.TextTertiary)
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "다음 날짜",
                tint = if (calendarOpen) HnColors.Primary else HnColors.TextSecondary,
                modifier = Modifier.size(28.dp).clickable(onClick = onNext),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineCalendar(
    selectedDate: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    val initialMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    LaunchedEffect(state.selectedDateMillis) {
        val millis = state.selectedDateMillis ?: return@LaunchedEffect
        val picked = java.time.Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
        if (picked != selectedDate) onPick(picked)
    }

    HnCard(padding = 4.dp) {
        DatePicker(
            state = state,
            showModeToggle = false,
            title = null,
            headline = null,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = HnColors.Primary,
                todayDateBorderColor = HnColors.Primary,
                todayContentColor = HnColors.Primary,
            ),
        )
    }
}

@Composable
private fun NoteRow(n: Note) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.width(56.dp).padding(top = 12.dp)) {
            Text(n.time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
        }
        HnCard(padding = 12.dp, modifier = Modifier.weight(1f)) {
            Column {
                val validTags = n.tags.filter { it == "투약" || it == "STT" }
                if (validTags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        validTags.forEach { t ->
                            if (t == "투약") TagChip("투약", fg = HnColors.Info, bg = HnColors.TagInjBg)
                            else TagChip("음성", fg = HnColors.Purple, bg = HnColors.TagFluidBg)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Text(n.author, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(n.text, fontSize = 14.sp, color = HnColors.Text)
            }
        }
    }
}

@Composable
private fun OrderDateHeader(date: String, count: Int) {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    val label = if (parsed != null) {
        val dow = parsed.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        "${parsed.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))} ($dow)"
    } else {
        date
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
            Spacer(Modifier.weight(1f))
            Text("${count}건", fontSize = 12.sp, color = HnColors.TextTertiary)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(HnColors.Border))
    }
}

@Composable
private fun OrderRow(o: Order) {
    val (label, fg, bg) = when (o.kind) {
        OrderKind.INJ   -> Triple("투약", HnColors.Info,          HnColors.TagInjBg)
        OrderKind.FLUID -> Triple("수액", HnColors.Purple,        HnColors.TagFluidBg)
        OrderKind.ORDER -> Triple("지시", HnColors.TextSecondary, HnColors.TagOrderBg)
        OrderKind.LIS   -> Triple("LIS",  HnColors.Warning,       HnColors.TagLisBg)
        OrderKind.IMG   -> Triple("영상", HnColors.Cyan,          HnColors.TagImgBg)
        OrderKind.PILL  -> Triple("알약", HnColors.Success,       HnColors.TagPillBg)
    }
    HnCard(padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagChip(label, fg = fg, bg = bg)
                Spacer(Modifier.size(8.dp))
                Text(o.code, fontSize = 12.sp, color = HnColors.TextTertiary)
            }
            Spacer(Modifier.height(8.dp))
            Text(o.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(HnColors.SurfaceAlt).padding(10.dp),
            ) {
                Column {
                    GridCell("1회량", o.dose); GridCell("횟수", o.freq); GridCell("단위", o.unit); GridCell("용법", o.usage)
                }
            }
            if (o.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("참고: ${o.note}", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun GridCell(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = HnColors.TextTertiary, modifier = Modifier.size(width = 56.dp, height = 16.dp))
        Text(value, fontSize = 13.sp, color = HnColors.Text, fontWeight = FontWeight.SemiBold)
    }
}

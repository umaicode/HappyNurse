// 환자 상세 — 환자 정보 카드(접기/펼치기), 간호일지/의사오더 서브탭
package com.happynurse.presentation.screens.patientdetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
    onSelectPatient: (String) -> Unit = {},
    vm: PatientDetailViewModel = hiltViewModel(),
) {
    var displayId by remember { mutableStateOf(patientId) }
    var direction by remember { mutableIntStateOf(0) }
    LaunchedEffect(patientId) { displayId = patientId }
    LaunchedEffect(displayId) {
        displayId.toLongOrNull()?.let { vm.loadPatient(it) }
    }
    val loadedPatient = vm.patient.collectAsStateWithLifecycle().value
    val notes by vm.notes.collectAsStateWithLifecycle()
    val orders by vm.orders.collectAsStateWithLifecycle()
    val myPatients by vm.myPatients.collectAsStateWithLifecycle()
    // 상세 API 응답 전에는 담당환자 목록에서 매칭되는 항목으로 즉시 표시 (깜빡임 방지)
    val resolvePatient: (String) -> Patient? = { id ->
        loadedPatient?.takeIf { it.id == id } ?: myPatients.firstOrNull { it.id == id }
    }
    if (resolvePatient(displayId) == null) return
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    val visibleMonth by vm.visibleMonth.collectAsStateWithLifecycle()
    val monthCounts by vm.monthCounts.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("notes") }
    var patientMenuOpen by remember { mutableStateOf(false) }
    var calendarOpen by remember { mutableStateOf(false) }

    val groupedOrders = remember(orders) {
        orders.groupBy { it.dateWritten }.toSortedMap(compareByDescending { it })
    }

    val swipeModifier = if (myPatients.size > 1) {
        Modifier.pointerInput(myPatients, displayId) {
            var dragX = 0f
            val threshold = 80f
            detectHorizontalDragGestures(
                onDragStart = { dragX = 0f },
                onDragEnd = {
                    if (kotlin.math.abs(dragX) >= threshold) {
                        val idx = myPatients.indexOfFirst { it.id == displayId }
                        if (idx >= 0) {
                            val size = myPatients.size
                            val forward = dragX < 0
                            val nextIdx = if (forward) (idx + 1) % size
                            else (idx - 1 + size) % size
                            direction = if (forward) 1 else -1
                            displayId = myPatients[nextIdx].id
                        }
                    }
                    dragX = 0f
                },
                onDragCancel = { dragX = 0f },
                onHorizontalDrag = { _, dragAmount -> dragX += dragAmount },
            )
        }
    } else Modifier

    AnimatedContent(
        targetState = displayId,
        transitionSpec = {
            val forward = direction >= 0
            (slideInHorizontally(animationSpec = tween(280)) { w -> if (forward) w else -w } +
                fadeIn(tween(220)))
                .togetherWith(
                    slideOutHorizontally(animationSpec = tween(280)) { w -> if (forward) -w else w } +
                        fadeOut(tween(220)),
                )
        },
        modifier = Modifier.fillMaxSize().background(HnColors.Bg).then(swipeModifier),
        label = "patient-swipe",
    ) { animatedId ->
        val p: Patient = resolvePatient(animatedId) ?: return@AnimatedContent
        val curIdx = myPatients.indexOfFirst { it.id == p.id }
        val sizeP = myPatients.size
        val prevPatient = if (sizeP > 1 && curIdx >= 0) myPatients[(curIdx - 1 + sizeP) % sizeP] else null
        val nextPatient = if (sizeP > 1 && curIdx >= 0) myPatients[(curIdx + 1) % sizeP] else null
        val goTo: (String, Boolean) -> Unit = { id, forward ->
            direction = if (forward) 1 else -1
            displayId = id
        }
        Column(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            item {
                val cardInteraction = remember { MutableInteractionSource() }
                HnCard(padding = 14.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = cardInteraction,
                                indication = null,
                            ) { expanded = !expanded },
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoCell("생년월일", formatDotDate(p.birthdate), modifier = Modifier.weight(1f))
                            Box(Modifier.width(1.dp).height(36.dp).background(HnColors.Border))
                            InfoCell("병명", p.diseaseName.ifBlank { "-" }, modifier = Modifier.weight(1f).padding(start = 12.dp))
                            Spacer(Modifier.size(22.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = HnColors.Border, thickness = 1.dp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            InfoCell("호실/침대", "${p.room}호 ${p.bed}", modifier = Modifier.weight(1f))
                            Box(Modifier.width(1.dp).height(36.dp).background(HnColors.Border))
                            InfoCell("MRN", p.mrn.ifBlank { "-" }, modifier = Modifier.weight(1f).padding(start = 12.dp))
                            Icon(
                                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = if (expanded) "접기" else "펼치기",
                                tint = HnColors.TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        if (expanded) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = HnColors.Border, thickness = 1.dp)
                            Spacer(Modifier.height(12.dp))
                            InfoRow("진료부서", p.department)
                            InfoRow("담당의", "${p.doctor} 의사")
                            InfoRow("주증상", p.chief.ifBlank { "-" })
                            InfoRow("수술", p.surgery.ifBlank { "-" })
                            InfoRow("입원일", formatAdmittedOn(p.admittedOn, p.daysSince))
                            InfoRow("휴대폰", p.phone.ifBlank { "-" })
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
                                    fontWeight = FontWeight.Bold,
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
                        MonthGrid(
                            yearMonth = visibleMonth,
                            selectedDate = selectedDate,
                            counts = monthCounts,
                            onPrevMonth = { vm.setMonth(visibleMonth.minusMonths(1)) },
                            onNextMonth = { vm.setMonth(visibleMonth.plusMonths(1)) },
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
        // 하단 바 위에 floating 원형 뒤로가기 버튼
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 12.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(HnColors.SurfaceAlt)
                .border(1.dp, HnColors.Border, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "뒤로",
                tint = HnColors.TextSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
        }
        HorizontalDivider(color = HnColors.Border, thickness = 1.dp)
        // 하단 바: [이전환자 | 현재환자 | 다음환자]
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HnColors.Surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (prevPatient != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { goTo(prevPatient.id, false) }
                                .padding(vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = HnColors.TextTertiary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                prevPatient.name,
                                fontSize = 14.sp,
                                color = HnColors.TextTertiary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { patientMenuOpen = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(p.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "${p.sex}/${p.age}",
                            fontSize = 16.sp,
                            color = HnColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.size(2.dp))
                        Icon(
                            Icons.Outlined.ExpandLess,
                            contentDescription = "환자 선택",
                            tint = HnColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = patientMenuOpen,
                        onDismissRequest = { patientMenuOpen = false },
                        offset = androidx.compose.ui.unit.DpOffset(0.dp, (-8).dp),
                    ) {
                        if (myPatients.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "담당 환자가 없습니다",
                                    fontSize = 13.sp,
                                    color = HnColors.TextTertiary,
                                )
                            }
                        } else {
                            myPatients.forEachIndexed { idx, other ->
                                if (idx > 0) {
                                    HorizontalDivider(
                                        color = HnColors.Border,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    )
                                }
                                val current = other.id == p.id
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.width(180.dp),
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        other.name,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (current) HnColors.Primary else HnColors.Text,
                                                    )
                                                    Spacer(Modifier.size(8.dp))
                                                    Text(
                                                        "${other.sex}/${other.age}",
                                                        fontSize = 20.sp,
                                                        color = HnColors.TextSecondary,
                                                    )
                                                }
                                                Text(
                                                    "${other.room}호 ${other.bed}번 침대",
                                                    fontSize = 16.sp,
                                                    color = HnColors.TextTertiary,
                                                    fontWeight = FontWeight.SemiBold ,
                                                    modifier = Modifier.padding(top = 5.dp),
                                                )
                                            }
                                            if (current) {
                                                Icon(
                                                    Icons.Outlined.Check,
                                                    contentDescription = null,
                                                    tint = HnColors.Primary,
                                                    modifier = Modifier.size(26.dp),
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
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (nextPatient != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { goTo(nextPatient.id, true) }
                                .padding(vertical = 6.dp),
                        ) {
                            Text(
                                nextPatient.name,
                                fontSize = 14.sp,
                                color = HnColors.TextTertiary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = HnColors.TextTertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = HnColors.TextTertiary,
            modifier = Modifier.width(64.dp).padding(top = 2.dp),
        )
        Text(
            value,
            fontSize = 14.sp,
            color = HnColors.Text,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 14.sp, color = HnColors.TextTertiary)
        Spacer(Modifier.height(1.dp))
        Text(value.ifBlank { "-" }, fontSize = 14.sp, color = HnColors.Text, fontWeight = FontWeight.SemiBold)
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
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold , color = HnColors.Text)
                if (isToday) {
                    Spacer(Modifier.size(6.dp))
                    TagChip("오늘", fg = HnColors.Success, bg = HnColors.TagPillBg)
                }
                Spacer(Modifier.size(6.dp))
                Text("${count}건", fontSize = 14.sp, color = HnColors.TextTertiary)
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

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    counts: Map<LocalDate, Int>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val firstOfMonth = yearMonth.atDay(1)
    // ISO: Mon=1..Sun=7. We want Sun-first columns (Sun=0..Sat=6)
    val leading = (firstOfMonth.dayOfWeek.value % 7) // Mon→1, Sun→0
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = ((leading + daysInMonth + 6) / 7) * 7

    HnCard(padding = 12.dp) {
        Column {
            // 헤더: 월 이동
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "이전 달",
                    tint = HnColors.TextSecondary,
                    modifier = Modifier.size(24.dp).clickable(onClick = onPrevMonth),
                )
                Text(
                    "${yearMonth.year}년 ${yearMonth.monthValue}월",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold ,
                    color = HnColors.Text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "다음 달",
                    tint = HnColors.TextSecondary,
                    modifier = Modifier.size(24.dp).clickable(onClick = onNextMonth),
                )
            }
            Spacer(Modifier.height(8.dp))
            // 요일 헤더
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { i, label ->
                    val color = when (i) {
                        0 -> HnColors.Danger
                        6 -> HnColors.Info
                        else -> HnColors.TextSecondary
                    }
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // 날짜 그리드 (7열)
            var idx = 0
            while (idx < totalCells) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val cell = idx + col
                        val dayNumber = cell - leading + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNumber)
                            val isSelected = date == selectedDate
                            val isToday = date == today
                            val cnt = counts[date] ?: 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(horizontal = 1.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFDCEEFB) else Color.Transparent)
                                    .clickable { onPick(date) }
                                    .padding(vertical = 4.dp),
                            ) {
                                Text(
                                    dayNumber.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isToday -> HnColors.Primary
                                        col == 0 -> HnColors.Danger
                                        col == 6 -> HnColors.Info
                                        else -> HnColors.Text
                                    },
                                )
                                if (isToday) {
                                    Spacer(Modifier.height(1.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(HnColors.Primary),
                                    )
                                }
                                if (cnt > 0) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${cnt}건",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HnColors.Primary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible,
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                            )
                        }
                    }
                }
                idx += 7
            }
        }
    }
}

@Composable
private fun NoteRow(n: Note) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.width(56.dp).padding(top = 12.dp)) {
            Text(n.time, fontSize = 16.sp, fontWeight = FontWeight.Medium , color = HnColors.Text)
        }
        HnCard(padding = 12.dp, modifier = Modifier.weight(1f)) {
            Column {
                val validTags = n.tags.filter { it == "투약" || it == "STT" }
                if (validTags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        validTags.forEach { t ->
                            if (t == "투약") TagChip("투약", fg = HnColors.TagInjFg, bg = HnColors.TagInjBg)
                            else TagChip("음성", fg = HnColors.TagFluidFg, bg = HnColors.TagFluidBg)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(n.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = HnColors.Text)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = HnColors.Border, thickness = 1.dp)
                Spacer(Modifier.height(12.dp))
                Text(n.author, fontSize = 16.sp, fontWeight = FontWeight.Medium , color = HnColors.TextSecondary)

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
            Text("${count}건", fontSize = 14.sp, color = HnColors.TextTertiary)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(HnColors.Border))
    }
}

@Composable
private fun OrderRow(o: Order) {
    val (label, fg, bg) = when (o.kind) {
        OrderKind.INJ   -> Triple("투약", HnColors.TagInjFg,   HnColors.TagInjBg)
        OrderKind.FLUID -> Triple("수액", HnColors.TagFluidFg, HnColors.TagFluidBg)
        OrderKind.ORDER -> Triple("지시", HnColors.TagOrderFg, HnColors.TagOrderBg)
        OrderKind.LIS   -> Triple("LIS",  HnColors.TagLisFg,   HnColors.TagLisBg)
        OrderKind.IMG   -> Triple("영상", HnColors.TagImgFg,   HnColors.TagImgBg)
        OrderKind.PILL  -> Triple("알약", HnColors.TagPillFg,  HnColors.TagPillBg)
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.width(56.dp).padding(top = 12.dp)) {
            Text(o.timeWritten, fontSize = 16.sp, fontWeight = FontWeight.Medium , color = HnColors.Text)
        }
        OrderCard(o, label, fg, bg, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OrderCard(
    o: Order,
    label: String,
    fg: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    HnCard(padding = 14.dp, modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagChip(label, fg = fg, bg = bg)
                Spacer(Modifier.size(8.dp))
                Text(o.code, fontSize = 14.sp, fontWeight = FontWeight.Medium , color = HnColors.TextTertiary)
            }
            Spacer(Modifier.height(8.dp))
            Text(o.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(HnColors.SurfaceAlt).padding(10.dp),
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        GridCell("1회량", o.dose, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        GridCell("횟수", o.freq, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        GridCell("단위", o.unit, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        GridCell("용법", o.usage, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (o.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = HnColors.Border, thickness = 1.dp)
                Spacer(Modifier.height(12.dp))
                Text(o.note, fontSize = 16.sp,fontWeight = FontWeight.Medium, color = HnColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun GridCell(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = HnColors.TextTertiary,
            modifier = Modifier.width(56.dp).padding(top = 1.dp),
        )
        Text(
            value.ifBlank { "-" },
            fontSize = 16.sp,
            color = HnColors.Text,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatDotDate(raw: String): String {
    if (raw.isBlank()) return "-"
    val parsed = runCatching { LocalDate.parse(raw.take(10)) }.getOrNull() ?: return raw
    return parsed.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
}

private fun formatAdmittedOn(raw: String, daysSince: Int): String {
    val date = formatDotDate(raw)
    if (date == "-") return "-"
    return "$date (D+$daysSince)"
}

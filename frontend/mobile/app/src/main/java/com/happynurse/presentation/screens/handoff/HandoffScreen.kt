// 인수인계 탭 — AI 서버의 roster-summary + 환자별 PASS-BAR 리포트 연동
// 상단: 전체 시프트 요약 / 하단: 담당 환자 HorizontalPager (탭 시 상세 펼침)
package com.happynurse.presentation.screens.handoff

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.clickable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.presentation.components.PageHeader
import com.happynurse.presentation.screens.handoff.components.HandoverPatientCard
import com.happynurse.presentation.screens.handoff.components.WardEventsStrip
import com.happynurse.presentation.screens.patients.currentShiftLabel
import com.happynurse.presentation.theme.HnColors

@Composable
fun HandoffScreen(
    onOpenPatient: (String) -> Unit = {},
    vm: HandoffViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = "인수인계",
            sub = currentShiftLabel().let { cur ->
                val next = when (cur) { "데이" -> "이브닝"; "이브닝" -> "나이트"; else -> "데이" }
                "$cur → $next"
            },
            right = {
                IconButton(onClick = vm::refresh, enabled = !state.refreshing) {
                    if (state.refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = HnColors.Primary,
                        )
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "새로고침", tint = HnColors.TextSecondary)
                    }
                }
            },
        )

        when {
            state.loading -> LoadingContent()
            state.error != null && state.rosterSummary == null -> ErrorContent(state.error!!, vm::refresh)
            state.rosterSummary == null -> EmptyContent()
            state.patients.isEmpty() -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(8.dp))
                    WardEventsStrip(
                        events = state.wardEvents,
                        loading = state.wardEventsLoading,
                        error = state.wardEventsError,
                        onRetry = vm::refresh,
                    )
                    Spacer(Modifier.height(24.dp))
                    EmptyContent()
                }
            }
            else -> Content(state, vm, onOpenPatient)
        }
    }
}

@Composable
private fun Content(
    state: HandoffUiState,
    vm: HandoffViewModel,
    onOpenPatient: (String) -> Unit,
) {
    state.rosterSummary ?: return
    val patients = state.patients
    val pagerState = rememberPagerState(pageCount = { patients.size })
    val scope = rememberCoroutineScope()
    var dropdownOpen by remember { mutableStateOf(false) }

    LaunchedEffect(patients) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                patients.getOrNull(page)?.handoverId
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        vm.ensureDetailLoaded(it)
                        vm.ensureChecksLoaded(it)
                    }
            }
    }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        WardEventsStrip(
            events = state.wardEvents,
            loading = state.wardEventsLoading,
            error = state.wardEventsError,
            onRetry = vm::refresh,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dropdownOpen = true }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                ) {
                    Text(
                        "담당 환자",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HnColors.Text,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${patients.size}명",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = HnColors.TextSecondary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "환자 목록",
                        tint = HnColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                ) {
                    patients.forEachIndexed { idx, item ->
                        val patient = state.patientByEncounterId[item.encounterId]
                        val label = patient?.name ?: "환자 ${idx + 1}"
                        val sub = patient?.let {
                            listOfNotNull(
                                it.sex.takeIf { s -> s.isNotBlank() },
                                it.birthdate.takeIf { b -> b.isNotBlank() },
                            ).joinToString(" · ")
                        } ?: ""
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        label,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HnColors.Text,
                                    )
                                    if (sub.isNotBlank()) {
                                        Text(
                                            sub,
                                            fontSize = 16.sp,
                                            color = HnColors.TextSecondary,
                                        )
                                    }
                                }
                            },
                            onClick = {
                                dropdownOpen = false
                                scope.launch { pagerState.animateScrollToPage(idx) }
                            },
                        )
                    }
                }
            }
            Box(Modifier.weight(1f))
            Text(
                "좌우로 슬라이드",
                fontSize = 12.sp,
                color = HnColors.TextTertiary,
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
        ) { page ->
            val item = patients[page]
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 16.dp),
            ) {
                val patient = state.patientByEncounterId[item.encounterId]
                HandoverPatientCard(
                    item = item,
                    patient = patient,
                    detail = state.detailByHandoverId[item.handoverId],
                    loadingDetail = state.loadingDetailIds.contains(item.handoverId),
                    checkedMap = state.checksByHandoverId[item.handoverId],
                    inFlight = state.checksInFlight[item.handoverId].orEmpty(),
                    onToggleCheck = { idx, v -> vm.toggleSynthesisCheck(item.handoverId, idx, v) },
                    onOpenPatient = {
                        patient?.patientId?.toString()?.takeIf { it.isNotBlank() && it != "0" }
                            ?.let(onOpenPatient)
                    },
                )
            }
        }
        PageIndicator(current = pagerState.currentPage, total = patients.size)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PageIndicator(current: Int, total: Int) {
    if (total <= 1) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(total) { idx ->
            val active = idx == current
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) HnColors.Primary else HnColors.BorderStrong),
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = HnColors.Primary,
        )
        Spacer(Modifier.height(12.dp))
        Text("인수인계 요약 생성중…", fontSize = 13.sp, color = HnColors.TextSecondary)
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Inbox,
            contentDescription = null,
            tint = HnColors.TextTertiary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "담당 환자가 없습니다",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = HnColors.TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "이번 시프트에 배정된 환자가 없거나 리포트가 생성되지 않았습니다.",
            fontSize = 12.sp,
            color = HnColors.TextTertiary,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "인수인계 정보를 불러오지 못했습니다",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HnColors.TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 12.sp, color = HnColors.TextTertiary)
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(HnColors.PrimaryLight)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Outlined.Refresh, null, tint = HnColors.Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            androidx.compose.material3.TextButton(onClick = onRetry, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("다시 시도", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Primary)
            }
        }
    }
}

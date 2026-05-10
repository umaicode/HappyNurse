/**
 * ============================================================================
 *  ⚠️ 임시 하드코딩 샘플 데이터 — 백엔드 API 연동 시 단계적으로 제거 예정
 * ============================================================================
 *
 *  현재 사용 중인 화면:
 *    • nurseAlarms      → AlarmsScreen           (알람 탭)
 *    • ivTimers         → AlarmsScreen           (수액 타이머 카드)
 *    • notifications    → NotificationsSheet,
 *                         MainScaffold           (상단 알림 벨 카운트)
 *    • handoffs         → HandoffScreen          (인계 카드 + AI 요약)
 *    • hospitalsToWards → (현재 미사용)           ※ LoginScreen 은 실 API 사용
 *
 *  연동 완료 모델 (이 파일에 없음, 실 백엔드 API 사용):
 *    ✅ Patient    — WardApi / PatientApi  →  PatientRepository
 *    ✅ Note       — EncounterApi          →  EncounterRepository
 *    ✅ Order      — EncounterApi          →  EncounterRepository
 *    ✅ NurseProfile — PractitionerApi     →  PractitionerRepository
 *
 *  백엔드 API 가 새로 생기면 다음 순서로 제거:
 *    1) data/remote/model/ 에 DTO 추가
 *    2) data/remote/api/   에 Retrofit interface 추가
 *    3) data/remote/mapper/Mappers.kt 에 toDomain() 매퍼 추가
 *    4) data/repository/   에 Repository 추가
 *    5) ViewModel / Screen 에서 SampleData.xxx → repository 호출로 교체
 *    6) 이 파일에서 해당 리스트 + 미사용 import 제거
 * ============================================================================
 */
package com.happynurse.core.sample

import com.happynurse.domain.model.HandoffCheck
import com.happynurse.domain.model.HandoffItem
import com.happynurse.domain.model.IVTimer
import com.happynurse.domain.model.NotifCategory
import com.happynurse.domain.model.Notif
import com.happynurse.domain.model.NurseAlarm

object SampleData {

    // ── 간호 알람 (AlarmsScreen) ─────────────────────────────────────────────
    // TODO(backend): 알람 API 생성 후 AlarmRepository.getAlarms() 로 교체
    val nurseAlarms: List<NurseAlarm> = listOf(
        NurseAlarm("a1", "김가민", "701-1", "2026.04.30", "14:00", "NPO 유지 중. 수술 후 GAS passing 확인 전까지 금식 지시.", "13:55"),
        NurseAlarm("a2", "김가민", "701-1", "2026.04.30", "15:30", "낙상 고위험군. 야간 보행 시 간호사 동행 권고.",         "15:20"),
        NurseAlarm("a3", "이승연", "701-2", "2026.04.30", "16:00", "드레싱 교체 예정. 수술 부위 분비물 관찰 필요.",         "15:50"),
        NurseAlarm("a4", "김가민", "701-1", "2026.04.30", "18:00", "세프트리악손 1g IV 투약 시간. 수액 라인 확인.",          "17:55"),
        NurseAlarm("a5", "박서영", "702-1", "2026.04.30", "23:30", "NPO 시작 30분 전 안내. 수술 D-1 준비 확인.",            "23:25"),
    )

    // ── 수액 타이머 (AlarmsScreen 내 수액 카드) ──────────────────────────────
    // TODO(backend): 수액 모니터링 API 생성 후 IVTimerRepository.getActive() 로 교체
    val ivTimers: List<IVTimer> = listOf(
        IVTimer(id = "iv1", patient = "김가민", room = "701호", bed = "1", drug = "0.9% 생리식염수 1L",     totalMin = 750,  elapsedMin = 270, endsAt = "21:30", startedAt = "09:00"),
        IVTimer(id = "iv2", patient = "이승연", room = "701호", bed = "2", drug = "5% 포도당 1L",            totalMin = 1000, elapsedMin = 820, endsAt = "17:50", startedAt = "01:30"),
        IVTimer(id = "iv3", patient = "박서영", room = "702호", bed = "1", drug = "0.45% 생리식염수 500mL", totalMin = 480,  elapsedMin = 100, endsAt = "20:10", startedAt = "12:10"),
    )

    // ── 알림 (NotificationsSheet + MainScaffold 알림 벨 카운트) ──────────────
    // TODO(backend): SSE/FCM 실시간 알림 API 생성 후 NotificationRepository 로 교체
    //                upcoming=true 항목 개수가 상단 벨 배지에 표시됨
    val notifications: List<Notif> = listOf(
        Notif("n1", NotifCategory.FLUID,   "이승연", "701-2", "수액 잔량 18%. 1시간 내 교체 필요.", "14:30",    0, true,  true),
        Notif("n2", NotifCategory.WATCH,   "김가민", "701-1", "낙상 고위험. 야간 보행 시 동행 권고.",  "14:30",    0, true,  true),
        Notif("n5", NotifCategory.REQUEST, "박서영", "702-1", "세프트리악손 투약 시간.",               "14:32",   -2, true,  true),
        Notif("n6", NotifCategory.FLUID,   "강민서", "703-2", "수액 교체 필요.",                       "14:33",   -3, true,  true),
        Notif("n8", NotifCategory.REQUEST, "최정현", "703-1", "혈압 측정 예정.",                       "14:35",   -5, true,  true),
        Notif("n3", NotifCategory.WATCH,   "김가민", "701-1", "세프트리악손 투약 시간 18:00.",         "13:58",   32, false, false),
        Notif("n4", NotifCategory.FLUID,   "박서영", "702-1", "수액 시작 완료 (12:10).",               "12:10",  140, false, false),
        Notif("n7", NotifCategory.REQUEST, "이승연", "701-2", "체온 37.2℃ 측정됨. 계속 모니터링 필요.", "11:20",  250, false, false),
    )

    val handoffs: List<HandoffItem> = listOf(
        HandoffItem(
            id = "h1", patient = "김가민", room = "701-1", tag = "복강경 충수절제술",
            note = "수술 직후. NPO 유지. 통증 NRS 4점 → 2점 (트라마돌 50mg IV 투여 후). V/S 안정. 야간 보행 시 동행 권고.",
            aiSummary = listOf(
                "수술 직후 안정. V/S 정상 범위.",
                "통증 관리 양호 (NRS 4→2, 트라마돌 효과).",
                "낙상 고위험군 — 야간 보행 동행 필수.",
            ),
            warnings = "신규 처방: 세프트리악손 1g q12h 추가 — 18:00 첫 투여 확인 필요.",
            checklist = listOf(
                HandoffCheck("18:00 세프트리악손 IV 투여", false),
                HandoffCheck("NS 1L 수액 교체 (21:30 예정)", false),
                HandoffCheck("수술 부위 드레싱 확인 (저녁 라운드)", false),
            ),
        ),
        HandoffItem(
            id = "h2", patient = "이승연", room = "701-2", tag = "복강경 담낭절제술",
            note = "미열 T 37.4 지속. 통증 NRS 5점 호소 시 아세트아미노펜 1g IV. 수액 D5W 60mL/hr 유지. 17:50 종료 예정.",
            aiSummary = listOf(
                "미열 지속 — 추가 모니터링 필요.",
                "통증 관리 PRN 처방 활용 중.",
                "수액 17:50 종료 — 교체 여부 결정 필요.",
            ),
            warnings = "체온 38.0 이상 시 즉시 주치의 보고. 혈액배양 검사 가능성.",
            checklist = listOf(
                HandoffCheck("체온 q4h 측정", true),
                HandoffCheck("수액 교체 또는 종료 확인", false),
                HandoffCheck("드레싱 교체 (16:00 예정)", false),
            ),
        ),
        HandoffItem(
            id = "h3", patient = "박서영", room = "702-1", tag = "서혜부 탈장 교정술 (수술 D-1)",
            note = "수술 D-1. 내일 08:30 수술 예정. 24:00부터 NPO 시작. 수술 전 교육 완료.",
            aiSummary = listOf(
                "수술 전날 — 환자 상태 안정.",
                "24:00 NPO 시작 안내 완료.",
                "수술 동의서 서명 확인 필요.",
            ),
            warnings = "수술 전 검사 결과 미확인 — CBC, 흉부 X-ray 결과 확인 필요.",
            checklist = listOf(
                HandoffCheck("수술 동의서 확인", true),
                HandoffCheck("23:30 NPO 안내 알림", false),
                HandoffCheck("수술 전 검사 결과 확인", false),
            ),
        ),
    )

    val hospitalsToWards: Map<String, List<String>> = mapOf(
        "삼성서울병원"      to listOf("7W", "8W", "9W", "ICU", "NICU", "ER"),
        "서울아산병원"      to listOf("내과1", "내과2", "외과1", "ICU", "ER"),
        "세브란스병원"      to listOf("A병동", "B병동", "C병동", "ICU"),
        "서울대학교병원"    to listOf("101호", "201호", "301호", "ICU"),
        "고려대학교안암병원" to listOf("동관1", "동관2", "서관1", "ICU"),
        "가톨릭성모병원"    to listOf("내과병동", "외과병동", "정형외과", "ICU"),
    )
}

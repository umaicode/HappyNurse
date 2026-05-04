// 와이어프레임 data.jsx 의 PATIENTS/ALARMS_NURSE/IV_TIMERS/NOTIFICATIONS/HANDOFF 샘플을 그대로 이식
package com.happynurse.core.sample

import com.happynurse.domain.model.HandoffCheck
import com.happynurse.domain.model.HandoffItem
import com.happynurse.domain.model.IVTimer
import com.happynurse.domain.model.NotifCategory
import com.happynurse.domain.model.Notif
import com.happynurse.domain.model.NurseAlarm
import com.happynurse.domain.model.Note
import com.happynurse.domain.model.Order
import com.happynurse.domain.model.OrderKind
import com.happynurse.domain.model.Patient
import com.happynurse.domain.model.Vitals

object SampleData {

    val patients: List<Patient> = listOf(
        Patient(
            id = "p1", name = "김가민", sex = "여", age = 36, birthdate = "1990-03-15",
            mrn = "SMC-MRN-0001", ward = "7W", room = "701", bed = "1",
            admittedOn = "2026-04-26", daysSince = 4, nurse = "김소연",
            department = "일반외과 (GS)", doctor = "최현웅",
            chief = "급성충수염", surgery = "복강경 충수절제술",
            memo = "NPO 유지 중. 수술 후 통증 호소 가능성, 진통제 PRN.",
            vitals = Vitals(bp = "118/76", hr = 82, rr = 18, temp = "36.7", spo2 = 98),
            notes = listOf(
                Note("06:30", "박지현 간호사", "V/S 안정. BP 118/76, HR 82, T 36.7. 수술 부위 드레싱 깨끗함.", listOf("투약")),
                Note("09:15", "박지현 간호사", "식이 상태 NPO 유지. 환자 갈증 호소 → 구강 적심 시행.", listOf("확정")),
                Note("11:40", "김소연 간호사", "주치의 회진. 통증 NRS 4점 → 트라마돌 50mg IV 투여. 30분 후 NRS 2점으로 감소.", listOf("투약", "STT", "확정")),
                Note("14:30", "김소연 간호사", "보행 시도. 침상 옆 1m 보행 후 어지러움 호소 → 즉시 침상 안정.", listOf("STT", "확정")),
            ),
            orders = listOf(
                Order(OrderKind.INJ,   "CFTRI", "세프트리악손 1g",      "1g",   "q12h", "IV", "식전",   "진행", "수술 후 항생제 7일 예정",  "2026-04-26"),
                Order(OrderKind.FLUID, "NS09",  "0.9% 생리식염수 1L",   "1L",   "q24h", "IV", "24시간", "진행", "주입 속도 80 mL/hr",         "2026-04-26"),
                Order(OrderKind.INJ,   "TRAM",  "트라마돌 50mg",        "50mg", "PRN",  "IV", "통증 시", "완료", "11:40 투여 완료. NRS 4→2", "2026-04-26"),
                Order(OrderKind.ORDER, "NPO",   "NPO 유지",             "-",    "q24h", "-",  "경구금식", "진행", "수술 후 GAS PASSING 시까지", "2026-04-26"),
                Order(OrderKind.LIS,   "CBC",   "Complete Blood Count", "1회",  "q24h", "-",  "검사실",   "검사중", "06:00 채혈 완료. 결과 대기", "2026-04-30"),
                Order(OrderKind.IMG,   "CXR",   "Chest X-ray PA",       "1매",  "1회",  "-",  "영상의학과", "접수", "오후 일정 협의 중",         "2026-04-30"),
            ),
        ),
        Patient(
            id = "p2", name = "이승연", sex = "남", age = 58, birthdate = "1968-11-07",
            mrn = "SMC-MRN-0014", ward = "7W", room = "701", bed = "2",
            admittedOn = "2026-04-25", daysSince = 5, nurse = "김소연",
            department = "일반외과 (GS)", doctor = "최현웅",
            chief = "담낭염", surgery = "복강경 담낭절제술",
            memo = "복부 통증 NRS 5점 호소 중. 야간 모니터링 필요.",
            vitals = Vitals("132/84", 92, 20, "37.4", 96),
            notes = listOf(
                Note("07:00", "박지현 간호사", "V/S 측정. T 37.4 미열. 통증 NRS 5점.", listOf("확정")),
                Note("10:20", "김소연 간호사", "주치의 보고 후 아세트아미노펜 IV 1g 투여.", listOf("투약", "확정")),
            ),
            orders = listOf(
                Order(OrderKind.INJ,   "APAP", "아세트아미노펜 1g", "1g", "q6h",  "IV", "PRN",   "진행", "발열 또는 통증 시"),
                Order(OrderKind.FLUID, "D5W",  "5% 포도당 1L",      "1L", "q24h", "IV", "연속",  "진행", "60 mL/hr"),
            ),
        ),
        Patient(
            id = "p3", name = "박서영", sex = "여", age = 42, birthdate = "1984-06-22",
            mrn = "SMC-MRN-0027", ward = "7W", room = "702", bed = "1",
            admittedOn = "2026-04-28", daysSince = 2, nurse = "김소연",
            department = "일반외과 (GS)", doctor = "최현웅",
            chief = "서혜부 탈장", surgery = "서혜부 탈장 교정술",
            memo = "내일 수술 예정. NPO from 24:00.",
            vitals = Vitals("120/78", 76, 16, "36.5", 99),
            notes = listOf(
                Note("08:00", "김소연 간호사", "수술 전 교육 시행. NPO 안내. 환자 이해 양호.", listOf("확정")),
            ),
            orders = listOf(
                Order(OrderKind.ORDER, "NPO", "NPO 유지", "-", "q24h", "-", "경구금식", "진행", "수술 24시간 전부터"),
            ),
        ),
        Patient(
            id = "p4", name = "최정현", sex = "남", age = 45, birthdate = "1981-02-18",
            mrn = "SMC-MRN-0038", ward = "7W", room = "703", bed = "1",
            admittedOn = "2026-04-29", daysSince = 1, nurse = "문현지",
            department = "정형외과 (OS)", doctor = "박재혁",
            chief = "우측 대퇴골 골절", surgery = "대퇴골 내고정술",
            memo = "수술 후 통증 조절 중. 혈전 예방 헤파린 처방.",
            vitals = Vitals("126/80", 78, 16, "36.8", 97),
            notes = listOf(
                Note("08:00", "문현지 간호사", "수술 후 V/S 안정. 통증 NRS 3점. 헤파린 5000U SC 투여.", listOf("투약")),
            ),
            orders = listOf(
                Order(OrderKind.INJ, "HEP", "헤파린 5000U", "5000U", "q12h", "SC", "피하주사", "진행", "DVT 예방"),
            ),
        ),
        Patient(
            id = "p5", name = "강민서", sex = "여", age = 29, birthdate = "1997-08-30",
            mrn = "SMC-MRN-0041", ward = "7W", room = "703", bed = "2",
            admittedOn = "2026-04-28", daysSince = 2, nurse = "문현지",
            department = "내과 (IM)", doctor = "김태영",
            chief = "폐렴", surgery = "-",
            memo = "항생제 치료 중. 산소 포화도 모니터링.",
            vitals = Vitals("110/70", 96, 22, "38.1", 94),
            notes = listOf(
                Note("09:00", "문현지 간호사", "SpO₂ 94%. 산소 2L/min 투여. 아지트로마이신 500mg IV 투여.", listOf("투약", "STT")),
            ),
            orders = listOf(
                Order(OrderKind.INJ,   "AZI", "아지트로마이신 500mg", "500mg",   "q24h", "IV",       "1일 1회", "진행", "폐렴 치료 5일 예정"),
                Order(OrderKind.ORDER, "O2",  "산소 투여",            "2L/min",  "cont", "비강캐뉼라", "연속",   "진행", "SpO₂ 95% 이상 유지"),
            ),
        ),
    )

    val nurseAlarms: List<NurseAlarm> = listOf(
        NurseAlarm("a1", "김가민", "701-1", "2026.04.30", "14:00", "NPO 유지 중. 수술 후 GAS passing 확인 전까지 금식 지시.", "13:55"),
        NurseAlarm("a2", "김가민", "701-1", "2026.04.30", "15:30", "낙상 고위험군. 야간 보행 시 간호사 동행 권고.",         "15:20"),
        NurseAlarm("a3", "이승연", "701-2", "2026.04.30", "16:00", "드레싱 교체 예정. 수술 부위 분비물 관찰 필요.",         "15:50"),
        NurseAlarm("a4", "김가민", "701-1", "2026.04.30", "18:00", "세프트리악손 1g IV 투약 시간. 수액 라인 확인.",          "17:55"),
        NurseAlarm("a5", "박서영", "702-1", "2026.04.30", "23:30", "NPO 시작 30분 전 안내. 수술 D-1 준비 확인.",            "23:25"),
    )

    val ivTimers: List<IVTimer> = listOf(
        IVTimer("iv1", "김가민", "701-1", "0.9% 생리식염수 1L",     totalMin = 750,  elapsedMin = 270, endsAt = "21:30", startedAt = "09:00"),
        IVTimer("iv2", "이승연", "701-2", "5% 포도당 1L",            totalMin = 1000, elapsedMin = 820, endsAt = "17:50", startedAt = "01:30"),
        IVTimer("iv3", "박서영", "702-1", "0.45% 생리식염수 500mL", totalMin = 480,  elapsedMin = 100, endsAt = "20:10", startedAt = "12:10"),
    )

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

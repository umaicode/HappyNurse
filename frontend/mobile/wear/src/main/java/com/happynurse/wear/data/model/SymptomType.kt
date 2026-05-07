// 환자 자가증상 유형 enum — 홈 환자알림 탭 카드 아이콘/색상 구분에 사용.
package com.happynurse.wear.data.model

enum class SymptomType(val label: String) {
    PAIN("통증"),
    DRESSING("드레싱"),
    IV("수액"),
}

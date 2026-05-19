// 폰 ↔ 워치 DataLayer 메시지 path 단일 출처 (양쪽 모듈 공통, 변경 시 app 의 동일 파일도 같이 수정)
// 음성 메모 알람 등록과 STT 음성 인식은 워치가 백엔드에 직접 호출하므로 관련 path 는 보유하지 않는다.
package com.happynurse.wear.data.remote.wearable

object WearableMessagePaths {
    // 워치 → 폰
    const val WEAR_FCM_TOKEN = "/wear/fcm-token" // 워치 FCM 토큰을 폰이 백엔드에 대행 등록
    const val WEAR_AUTH_TOKEN_REQUEST = "/wear/auth/token/request" // 워치가 토큰을 모를 때 폰에 요청

    // 폰 → 워치
    const val IV_ALERT = "/notification/iv_alert"        // 수액 알림
    const val TIMER_ALARM = "/notification/timer_alarm"  // 타이머 종료 알람
    const val PATIENT_CALL = "/notification/patient_call" // 환자 호출/요구사항
    const val SELF_REPORT_ALARM = "/notification/self_report_alarm" // 위급/높음 환자요청 풀스크린 알람 트리거
    const val SESSION_LOGOUT = "/session/logout"         // 폰 로그아웃 시 워치 세션 동시 해제
    const val WEAR_AUTH_TOKEN_RESPONSE = "/wear/auth/token/response" // 폰이 accessToken/wardId 응답
}

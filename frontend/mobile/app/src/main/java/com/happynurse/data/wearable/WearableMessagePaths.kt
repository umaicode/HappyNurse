package com.happynurse.data.wearable

// 폰 ↔ 워치 DataLayer 메시지 path 단일 출처 (양쪽 모듈 공통, 변경 시 wear 의 동일 파일도 같이 수정)
// STT 처리는 서버에서 하므로 워치는 raw audio 캡처/송신, 폰은 서버 업로드/응답 중계만 담당
object WearableMessagePaths {
    // 워치 → 폰
    const val AUDIO_TIMER = "/audio/timer" // 타이머 STT 용 raw audio (폰이 서버에 업로드 후 시간 파싱)
    const val WEAR_FCM_TOKEN = "/wear/fcm-token"          // 워치 FCM 토큰을 폰이 백엔드에 대행 등록
    const val WEAR_STT_TIMER_CREATE = "/wear/stt/create"  // 워치에서 등록한 STT 타이머를 폰이 백엔드에 등록 위임

    // 폰 → 워치
    const val TIMER_START = "/timer/start"               // 서버에서 파싱된 타이머 시간(millis) 워치에 전달
    const val IV_ALERT = "/notification/iv_alert"        // 수액 알림
    const val TIMER_ALARM = "/notification/timer_alarm"  // 타이머 종료 알람
    const val PATIENT_CALL = "/notification/patient_call" // 환자 호출/요구사항
    const val SESSION_LOGOUT = "/session/logout"         // 폰 로그아웃 시 워치 세션 동시 해제
}

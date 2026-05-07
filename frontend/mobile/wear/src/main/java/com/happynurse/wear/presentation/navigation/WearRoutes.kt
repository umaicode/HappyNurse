// WearRoutes — 워치 NavGraph 의 sealed route 정의. s09/s13 알람은 Activity 진입이라 여기에 없음.
package com.happynurse.wear.presentation.navigation

sealed class WearRoute(val path: String) {
    data object HomePager : WearRoute("home_pager")            // home + record HorizontalPager
    data object SttRecording : WearRoute("stt/recording")      // s11
    data object SttResult : WearRoute("stt/result")            // s12
    data object IvProgress : WearRoute("iv/{id}") {
        fun build(id: Long) = "iv/$id"
        const val ARG_ID = "id"
    }
    data object SttTimerDetail : WearRoute("stt/timer/{id}") {
        fun build(id: String) = "stt/timer/$id"
        const val ARG_ID = "id"
    }
}

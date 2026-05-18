// PatientCallAlarmActivity — 환자 요청 HIGH/CRITICAL 풀스크린 알람 호스트 Activity.
// setShowWhenLocked + setTurnScreenOn 으로 잠금/꺼진 화면 위로 띄움.
package com.happynurse.wear.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.happynurse.wear.presentation.screens.alarm.PatientCallAlarmScreen
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme

class PatientCallAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val patientName = intent.getStringExtra(EXTRA_PATIENT) ?: ""
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val roomLocation = intent.getStringExtra(EXTRA_ROOM_LOCATION) ?: ""
        val priority = intent.getStringExtra(EXTRA_PRIORITY) ?: "HIGH"

        setContent {
            HappyNurseWearTheme {
                PatientCallAlarmScreen(
                    patientName = patientName,
                    body = body,
                    roomLocation = roomLocation,
                    priority = priority,
                    onDismiss = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_PATIENT = "extra.patient"
        const val EXTRA_BODY = "extra.body"
        const val EXTRA_ROOM_LOCATION = "extra.room_location"
        const val EXTRA_PRIORITY = "extra.priority"
    }
}

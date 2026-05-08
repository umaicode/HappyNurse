// SttAlarmActivity — s13 풀스크린 STT 타이머 알람의 호스트 Activity.
package com.happynurse.wear.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.happynurse.wear.presentation.screens.alarm.SttAlarmScreen
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme

class SttAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val patientName = intent.getStringExtra(EXTRA_PATIENT) ?: "환자"
        val contentSummary = intent.getStringExtra(EXTRA_CONTENT) ?: ""
        val roomBedTime = intent.getStringExtra(EXTRA_ROOM_BED_TIME) ?: ""

        setContent {
            HappyNurseWearTheme {
                SttAlarmScreen(
                    patientName = patientName,
                    contentSummary = contentSummary,
                    roomBedTime = roomBedTime,
                    onDismiss = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_PATIENT = "extra.patient"
        const val EXTRA_CONTENT = "extra.content"
        const val EXTRA_ROOM_BED_TIME = "extra.room_bed_time"
    }
}

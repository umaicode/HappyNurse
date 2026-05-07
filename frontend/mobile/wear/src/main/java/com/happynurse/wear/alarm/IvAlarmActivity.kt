// IvAlarmActivity — s09 풀스크린 수액 종료 알람의 호스트 Activity. setShowWhenLocked + setTurnScreenOn.
package com.happynurse.wear.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.happynurse.wear.presentation.screens.alarm.IvAlarmScreen
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme

class IvAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val patientName = intent.getStringExtra(EXTRA_PATIENT) ?: "환자"
        val medication = intent.getStringExtra(EXTRA_MEDICATION) ?: ""
        val roomBedTime = intent.getStringExtra(EXTRA_ROOM_BED_TIME) ?: ""

        setContent {
            HappyNurseWearTheme {
                IvAlarmScreen(
                    patientName = patientName,
                    medicationName = medication,
                    roomBedTime = roomBedTime,
                    onDismiss = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_PATIENT = "extra.patient"
        const val EXTRA_MEDICATION = "extra.medication"
        const val EXTRA_ROOM_BED_TIME = "extra.room_bed_time"
    }
}
